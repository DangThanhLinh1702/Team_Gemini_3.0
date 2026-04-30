package auction.server.core;

import auction.server.model.AuctionSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class AuctionManager {
    private static AuctionManager instance;
    private final Map<String, AuctionSession> activeSessions;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);
    private Consumer<AuctionSession> onAuctionFinishedCallback;

    private AuctionManager() {
        activeSessions = new ConcurrentHashMap<>();
        createNewSession("ITEM-01", "Điện thoại iPhone 15", 1000.0, 60);
        createNewSession("ITEM-02", "Laptop Dell XPS", 2500.0, 60);
    }

    public static synchronized AuctionManager getInstance(){
        if(instance == null){
            instance = new AuctionManager();
        }
        return instance;
    }

    public void createNewSession(String id, String name, double startPrice, long durationSeconds) {
        // CẬP NHẬT: Truyền thêm durationSeconds vào hàm khởi tạo
        AuctionSession session = new AuctionSession(id, name, startPrice, durationSeconds);
        activeSessions.put(id, session);
        scheduler.schedule(() -> {
            session.finishAuction();
            System.out.println("HẾT GIỜ! " + id + " | Người thắng: " + session.getHighestBidder());
            if(onAuctionFinishedCallback != null){
                onAuctionFinishedCallback.accept(session);
            }
        }, durationSeconds, TimeUnit.SECONDS);
    }

    public void setOnAuctionEndCallback(Consumer<AuctionSession> callback) {
        this.onAuctionFinishedCallback = callback;
    }

    public AuctionSession getSession(String itemId) { return activeSessions.get(itemId); }
    public Map<String, AuctionSession> getAllSessions() { return activeSessions; }
}