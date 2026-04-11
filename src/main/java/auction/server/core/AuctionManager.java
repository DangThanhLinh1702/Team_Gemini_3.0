package auction.server.core;

import auction.server.model.AuctionSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionManager {
    // singleton
    private static AuctionManager instance;
    private final Map<String, AuctionSession> activeSessions;

    private AuctionManager() {
        activeSessions = new ConcurrentHashMap<>();
        createNewSession("ITEM-01", "Điện thoại iPhone 15", 1000.0);
        createNewSession("ITEM-02", "Laptop Dell XPS", 2500.0);

    }
    public static synchronized AuctionManager getInstance(){
        if(instance == null){
            instance = new AuctionManager();
        }
        return instance;
    }
    public void createNewSession(String id, String name, double startPrice) {
        AuctionSession session = new AuctionSession(id, name, startPrice);
        activeSessions.put(id, session);
    }
    public AuctionSession getSession(String itemId) {
        return activeSessions.get(itemId);
    }
    public Map<String, AuctionSession> getAllSessions() {
        return activeSessions;
    }
}
