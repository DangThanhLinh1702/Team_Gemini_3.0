package auction.server.core;

import auction.server.model.AuctionSession;

import java.sql.Timestamp;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class AuctionManager {
    private static AuctionManager instance;
    private final Map<Integer, AuctionSession> activeSessions; // dùng int thay vì String
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);
    private Consumer<AuctionSession> onAuctionFinishedCallback;

    private AuctionManager() {
        activeSessions = new ConcurrentHashMap<>();
        // Ví dụ tạo sẵn 2 phiên đấu giá demo
        createNewSession(1, 101, 1000.0, 60); // itemId=1, sellerId=101
        createNewSession(2, 102, 2500.0, 60); // itemId=2, sellerId=102
    }

    public static synchronized AuctionManager getInstance() {
        if (instance == null) {
            instance = new AuctionManager();
        }
        return instance;
    }

    /**
     * Tạo phiên đấu giá mới
     * @param itemId ID sản phẩm
     * @param sellerId ID người bán
     * @param startPrice giá khởi điểm
     * @param durationSeconds thời gian phiên đấu giá (giây)
     */
    public void createNewSession(int itemId, int sellerId, double startPrice, long durationSeconds) {
        Timestamp startTime = new Timestamp(System.currentTimeMillis());
        Timestamp endTime = new Timestamp(System.currentTimeMillis() + (durationSeconds * 1000));

        AuctionSession session = new AuctionSession(itemId, sellerId, startPrice, startTime, endTime);
        activeSessions.put(itemId, session);

        scheduler.schedule(() -> {
            session.finishAuction();
            System.out.println("HẾT GIỜ! Item " + itemId + " | Người thắng: " + session.getHighestBidder());
            if (onAuctionFinishedCallback != null) {
                onAuctionFinishedCallback.accept(session);
            }
        }, durationSeconds, TimeUnit.SECONDS);
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

    // Xóa phiên đấu giá khỏi RAM (khi ADMIN xóa sản phẩm)
    public boolean removeSession(int itemId) {
        return activeSessions.remove(itemId) != null;
    }

    // Cập nhật giá khởi điểm của phiên đấu giá trong RAM (khi ADMIN sửa sản phẩm)
    public boolean updateSessionPrice(int itemId, double newStartingPrice) {
        AuctionSession session = activeSessions.get(itemId);
        if (session != null) {
            session.setCurrentPrice(newStartingPrice);
            return true;
        }
        return false;
    }
}