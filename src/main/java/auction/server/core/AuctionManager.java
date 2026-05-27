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
 * Chịu trách nhiệm:
 * - Tạo phiên mới (createNewSession)
 * - Xử lý đặt giá (placeBid): cập nhật RAM + lưu bid_history DB + cập nhật auctions DB
 * - Tự động kết thúc phiên khi hết giờ và lưu kết quả vào DB
 * - Restore lịch sử bid từ DB vào RAM khi session được tạo lại
 */
public class AuctionManager {

    private static AuctionManager instance;

    // Map: itemId → AuctionSession đang chạy trong RAM
    private final Map<Integer, AuctionSession> activeSessions = new ConcurrentHashMap<>();

    // Scheduler để tự động kết thúc phiên khi hết giờ
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);

    // Callback gửi thông báo kết thúc về cho WebSocket server → broadcast client
    private Consumer<AuctionSession> onAuctionFinishedCallback;

    private final AuctionRepository auctionRepository     = new AuctionRepository();
    private final BidHistoryRepository bidHistoryRepository = new BidHistoryRepository();

    private static final SimpleDateFormat SDF = new SimpleDateFormat("HH:mm:ss");

    private AuctionManager() {
        // Đảm bảo bảng bid_history tồn tại ngay khi manager khởi động
        bidHistoryRepository.ensureTableExists();

        // Tải các phiên đấu giá đang diễn ra từ DB lên RAM
        loadActiveSessionsFromDb();
    }

    private void loadActiveSessionsFromDb() {
        List<AuctionSession> allSessions = auctionRepository.getAllAuctions();
        long now = System.currentTimeMillis();
        for (AuctionSession session : allSessions) {
            if (!session.isFinished() && session.getEndTime().getTime() > now) {
                activeSessions.put(session.getItemId(), session);

                // Nạp lịch sử bid
                List<String> history = bidHistoryRepository.getBidHistoryByItem(session.getItemId());
                if (!history.isEmpty()) {
                    session.loadBidHistory(history);
                }

                // Lên lịch tự kết thúc
                long delaySeconds = (session.getEndTime().getTime() - now) / 1000;
                scheduler.schedule(() -> {
                    session.finishAuction();
                    System.out.println("🏁 Hết giờ (Restore)! item_id=" + session.getItemId()
                            + " | Người thắng: " + session.getHighestBidder()
                            + " | Giá: " + String.format("%,.0f", session.getCurrentPrice()) + " VNĐ");

                    auctionRepository.finishAuction(session.getAuctionId(),
                            session.getCurrentPrice(), session.getHighestBidder());

                    String endLine = String.format("[%s] ⏰ Phiên kết thúc — Người thắng: %s với giá %,.0f VNĐ",
                            SDF.format(new Timestamp(System.currentTimeMillis())),
                            session.getHighestBidder(), session.getCurrentPrice());
                    bidHistoryRepository.saveBid(
                            session.getAuctionId(), session.getItemId(),
                            session.getHighestBidder(),
                            session.getCurrentPrice(),
                            endLine);

                    if (onAuctionFinishedCallback != null) {
                        onAuctionFinishedCallback.accept(session);
                    }
                }, delaySeconds, TimeUnit.SECONDS);

                System.out.println("🔄 Khôi phục phiên | item_id=" + session.getItemId()
                        + " | auction_id=" + session.getAuctionId()
                        + " | còn lại=" + delaySeconds + "s");
            } else if (!session.isFinished()) {
                // Đã quá hạn trong lúc server tắt -> Kết thúc luôn
                session.finishAuction();
                auctionRepository.finishAuction(session.getAuctionId(),
                        session.getCurrentPrice(), session.getHighestBidder());
                System.out.println("🏁 Đóng phiên quá hạn | item_id=" + session.getItemId());
            }
        }
    }

    public static synchronized AuctionManager getInstance() {
        if (instance == null) {
            instance = new AuctionManager();
        }
        return instance;
    }

    /**
     * Tạo phiên đấu giá mới cho một sản phẩm vừa được đăng.
     *
     * @param itemId          ID sản phẩm (từ bảng items)
     * @param sellerId        ID người bán (từ bảng users)
     * @param startPrice      Giá khởi điểm
     * @param durationSeconds Thời gian phiên đấu giá (giây)
     */
    public void createNewSession(int itemId, int sellerId, double startPrice, long durationSeconds) {
        Timestamp startTime = new Timestamp(System.currentTimeMillis());
        Timestamp endTime   = new Timestamp(System.currentTimeMillis() + durationSeconds * 1000L);

        AuctionSession session = new AuctionSession(itemId, sellerId, startPrice, startTime, endTime);
        activeSessions.put(itemId, session);

        // 1. Lưu phiên mới vào DB → DB sẽ gán auction_id và gán ngược vào session
        auctionRepository.saveAuction(session);

        // 2. Nạp lịch sử bid cũ từ DB vào RAM (phòng trường hợp item đã tồn tại bid)
        List<String> existingHistory = bidHistoryRepository.getBidHistoryByItem(itemId);
        if (!existingHistory.isEmpty()) {
            session.loadBidHistory(existingHistory);
        }

        // 3. Lên lịch tự động kết thúc phiên sau durationSeconds giây
        scheduler.schedule(() -> {
            session.finishAuction();
            System.out.println("🏁 Hết giờ! item_id=" + itemId
                    + " | Người thắng: " + session.getHighestBidder()
                    + " | Giá: " + String.format("%,.0f", session.getCurrentPrice()) + " VNĐ");

            // Lưu kết thúc vào bảng auctions
            auctionRepository.finishAuction(session.getAuctionId(),
                    session.getCurrentPrice(), session.getHighestBidder());

            // Lưu dòng "Phiên kết thúc" vào bid_history để hiển thị lại sau
            String endLine = String.format("[%s] ⏰ Phiên kết thúc — Người thắng: %s với giá %,.0f VNĐ",
                    SDF.format(new Timestamp(System.currentTimeMillis())),
                    session.getHighestBidder(), session.getCurrentPrice());
            bidHistoryRepository.saveBid(
                    session.getAuctionId(), itemId,
                    session.getHighestBidder(),
                    session.getCurrentPrice(),
                    endLine);

            // Thông báo cho WebSocket server để broadcast AUCTION_ENDED đến client
            if (onAuctionFinishedCallback != null) {
                onAuctionFinishedCallback.accept(session);
            }

        }, durationSeconds, TimeUnit.SECONDS);

        System.out.println("🚀 Tạo phiên đấu giá | item_id=" + itemId
                + " | auction_id=" + session.getAuctionId()
                + " | thời gian=" + durationSeconds + "s");
    }

    /**
     * Đặt giá cho một phiên đấu giá.
     * Nếu hợp lệ: cập nhật RAM → lưu bid_history DB → cập nhật auctions DB → trả về true.
     *
     * @param itemId   ID sản phẩm
     * @param username Người đặt giá
     * @param newPrice Giá mới
     * @return true nếu đặt giá thành công
     */
    public boolean placeBid(int itemId, String username, double newPrice) {
        AuctionSession session = activeSessions.get(itemId);
        if (session == null) return false;

        // Lưu displayText trước khi placeBid (để lấy đúng timestamp tại thời điểm bid)
        String timeStr = SDF.format(new Timestamp(System.currentTimeMillis()));
        String displayText = String.format("[%s] %s → %,.0f VNĐ", timeStr, username, newPrice);

        boolean success = session.placeBid(username, newPrice);
        if (success) {
            // Lưu từng bid vào bảng bid_history ngay lập tức
            bidHistoryRepository.saveBid(
                    session.getAuctionId(), itemId,
                    username, newPrice, displayText);

            // Cập nhật giá hiện tại vào bảng auctions
            auctionRepository.updateBid(session.getAuctionId(),
                    session.getCurrentPrice(), session.getHighestBidder());
        }
        return success;
    }

    /**
     * Lấy lịch sử bid từ DB (dùng khi JOIN phòng để trả về lịch sử đầy đủ,
     * kể cả khi phiên đã kết thúc hoặc server vừa restart).
     *
     * @param itemId ID sản phẩm
     * @return Danh sách chuỗi lịch sử, sắp xếp cũ → mới
     */
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

    /** Xóa phiên khỏi RAM (khi admin xóa sản phẩm) */
    public boolean removeSession(int itemId) {
        return activeSessions.remove(itemId) != null;
    }

    /** Cập nhật giá khởi điểm trong RAM (khi admin sửa sản phẩm chưa có bid) */
    public boolean updateSessionPrice(int itemId, double newStartingPrice) {
        AuctionSession session = activeSessions.get(itemId);
        if (session != null) {
            session.setCurrentPrice(newStartingPrice);
            return true;
        }
        return false;
    }
}