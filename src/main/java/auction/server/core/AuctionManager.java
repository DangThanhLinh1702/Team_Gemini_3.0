package auction.server.core;

import auction.server.model.AuctionSession;
import auction.server.repository.AuctionRepository;

import java.sql.Timestamp;
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
 * - Xử lý đặt giá (placeBid) và đồng bộ vào DB ngay lập tức
 * - Tự động kết thúc phiên khi hết giờ và lưu kết quả vào DB
 */
public class AuctionManager {

    private static AuctionManager instance;

    // Map: itemId → AuctionSession đang chạy trong RAM
    private final Map<Integer, AuctionSession> activeSessions = new ConcurrentHashMap<>();

    // Scheduler để tự động kết thúc phiên khi hết giờ
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);

    // Callback gửi thông báo kết thúc về cho WebSocket server → broadcast client
    private Consumer<AuctionSession> onAuctionFinishedCallback;

    private final AuctionRepository auctionRepository = new AuctionRepository();

    private AuctionManager() {
        // Không tạo session demo cứng — chỉ tạo session khi có item thật từ DB
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

        // 2. Lên lịch tự động kết thúc phiên sau durationSeconds giây
        scheduler.schedule(() -> {
            session.finishAuction();
            System.out.println("🏁 Hết giờ! item_id=" + itemId
                    + " | Người thắng: " + session.getHighestBidder()
                    + " | Giá: " + String.format("%,.0f", session.getCurrentPrice()) + " VNĐ");

            // 3. Lưu kết quả cuối vào DB
            auctionRepository.finishAuction(session.getAuctionId(),
                    session.getCurrentPrice(), session.getHighestBidder());

            // 4. Thông báo cho WebSocket server để broadcast AUCTION_ENDED đến client
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
     * Nếu hợp lệ: cập nhật RAM → cập nhật DB ngay lập tức → trả về true.
     *
     * @param itemId   ID sản phẩm
     * @param username Người đặt giá
     * @param newPrice Giá mới
     * @return true nếu đặt giá thành công
     */
    public boolean placeBid(int itemId, String username, double newPrice) {
        AuctionSession session = activeSessions.get(itemId);
        if (session == null) return false;

        boolean success = session.placeBid(username, newPrice);
        if (success) {
            // Đồng bộ giá mới vào DB ngay sau khi đặt thành công
            auctionRepository.updateBid(session.getAuctionId(),
                    session.getCurrentPrice(), session.getHighestBidder());
        }
        return success;
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
