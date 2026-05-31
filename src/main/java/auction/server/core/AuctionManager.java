package auction.server.core;

import auction.server.model.AuctionSession;
import auction.server.repository.AuctionRepository;
import auction.server.repository.BidHistoryRepository;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Singleton quản lý tất cả phiên đấu giá đang hoạt động trong RAM.
 * Khi khởi động, tự động restore các phiên đang chạy từ DB vào RAM
 * để client mới kết nối vẫn có thể đặt giá bình thường.
 */
public class AuctionManager {

    private static AuctionManager instance;

    private final Map<Integer, AuctionSession> activeSessions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);
    private Consumer<AuctionSession> onAuctionFinishedCallback;

    private final AuctionRepository auctionRepository = new AuctionRepository();
    private final BidHistoryRepository bidHistoryRepository = new BidHistoryRepository();

    private static final SimpleDateFormat SDF = new SimpleDateFormat("HH:mm:ss");

    private AuctionManager() {
        bidHistoryRepository.ensureTableExists();
        // Restore các phiên đang chạy từ DB vào RAM ngay khi khởi động
        restoreActiveSessionsFromDb();
    }

    public static synchronized AuctionManager getInstance() {
        if (instance == null) {
            instance = new AuctionManager();
        }
        return instance;
    }

    /**
     * Restore tất cả phiên đấu giá chưa kết thúc từ DB vào RAM.
     * Phiên nào còn thời gian → lên lịch kết thúc tiếp.
     * Phiên nào đã hết giờ nhưng chưa đánh dấu finished → đánh dấu kết thúc ngay.
     */
    private void restoreActiveSessionsFromDb() {
        List<AuctionSession> allSessions = auctionRepository.getAllAuctions();
        long now = System.currentTimeMillis();

        for (AuctionSession session : allSessions) {
            if (session.isFinished()) continue; // Bỏ qua phiên đã kết thúc

            long endTimeMs = session.getEndTime().getTime();
            long timeLeft = endTimeMs - now;

            // Load lịch sử bid từ DB vào RAM
            List<String> history = bidHistoryRepository.getBidHistoryByItem(session.getItemId());
            if (!history.isEmpty()) {
                session.loadBidHistory(history);
            }

            activeSessions.put(session.getItemId(), session);

            if (timeLeft > 0) {
                // Phiên còn thời gian → lên lịch kết thúc sau khoảng thời gian còn lại
                final AuctionSession s = session;
                scheduler.schedule(() -> finishSession(s), timeLeft, TimeUnit.MILLISECONDS);
                System.out.println("🔄 [RESTORE] Phiên item_id=" + session.getItemId()
                        + " còn " + (timeLeft / 1000) + "s");
            } else {
                // Phiên đã hết giờ nhưng server chưa kịp đánh dấu → kết thúc ngay
                System.out.println("⚡ [RESTORE] Phiên item_id=" + session.getItemId()
                        + " đã hết giờ, kết thúc ngay.");
                final AuctionSession s = session;
                scheduler.schedule(() -> finishSession(s), 0, TimeUnit.MILLISECONDS);
            }
        }
        System.out.println("✅ [RESTORE] Đã restore " + activeSessions.size() + " phiên từ DB.");
    }

    /** Kết thúc một phiên: cập nhật RAM + DB + broadcast callback */
    private void finishSession(AuctionSession session) {
        if (session.isFinished()) return; // Tránh double-finish
        session.finishAuction();

        auctionRepository.finishAuction(session.getAuctionId(),
                session.getCurrentPrice(), session.getHighestBidder());

        String endLine = String.format("[%s] ⏰ Phiên kết thúc — Người thắng: %s với giá %,.0f VNĐ",
                SDF.format(new Timestamp(System.currentTimeMillis())),
                session.getHighestBidder(), session.getCurrentPrice());
        bidHistoryRepository.saveBid(
                session.getAuctionId(), session.getItemId(),
                session.getHighestBidder(), session.getCurrentPrice(), endLine);

        System.out.println("🏁 Kết thúc phiên item_id=" + session.getItemId()
                + " | Người thắng: " + session.getHighestBidder()
                + " | Giá: " + String.format("%,.0f", session.getCurrentPrice()) + " VNĐ");

        if (onAuctionFinishedCallback != null) {
            onAuctionFinishedCallback.accept(session);
        }
    }

    /**
     * Tạo phiên đấu giá mới cho sản phẩm vừa đăng.
     */
    public void createNewSession(int itemId, int sellerId, double startPrice, long durationSeconds) {
        Timestamp startTime = new Timestamp(System.currentTimeMillis());
        Timestamp endTime   = new Timestamp(System.currentTimeMillis() + durationSeconds * 1000L);

        AuctionSession session = new AuctionSession(itemId, sellerId, startPrice, startTime, endTime);
        activeSessions.put(itemId, session);

        auctionRepository.saveAuction(session);

        List<String> existingHistory = bidHistoryRepository.getBidHistoryByItem(itemId);
        if (!existingHistory.isEmpty()) {
            session.loadBidHistory(existingHistory);
        }

        scheduler.schedule(() -> finishSession(session), durationSeconds, TimeUnit.SECONDS);

        System.out.println("🚀 Tạo phiên | item_id=" + itemId
                + " | auction_id=" + session.getAuctionId()
                + " | thời gian=" + durationSeconds + "s");
    }

    /**
     * Đặt giá cho một phiên đấu giá: cập nhật RAM → lưu bid_history DB → cập nhật auctions DB.
     */
    public boolean placeBid(int itemId, String username, double newPrice) {
        AuctionSession session = activeSessions.get(itemId);
        if (session == null) return false;

        String timeStr = SDF.format(new Timestamp(System.currentTimeMillis()));
        String displayText = String.format("[%s] %s → %,.0f VNĐ", timeStr, username, newPrice);

        boolean success = session.placeBid(username, newPrice);
        if (success) {
            bidHistoryRepository.saveBid(
                    session.getAuctionId(), itemId, username, newPrice, displayText);
            auctionRepository.updateBid(session.getAuctionId(),
                    session.getCurrentPrice(), session.getHighestBidder());
        }
        return success;
    }

    public List<String> getBidHistoryFromDb(int itemId) {
        return bidHistoryRepository.getBidHistoryByItem(itemId);
    }

    public void setOnAuctionEndCallback(Consumer<AuctionSession> callback) {
        this.onAuctionFinishedCallback = callback;
    }

    public AuctionSession getSession(int itemId) {
        return activeSessions.get(itemId);
    }

    public Map<Integer, AuctionSession> getAllSessions() {
        return activeSessions;
    }

    public boolean removeSession(int itemId) {
        return activeSessions.remove(itemId) != null;
    }

    public boolean updateSessionPrice(int itemId, double newStartingPrice) {
        AuctionSession session = activeSessions.get(itemId);
        if (session != null) {
            session.setCurrentPrice(newStartingPrice);
            return true;
        }
        return false;
    }
}