package auction.client.network;

import auction.shared.dto.ResponseDTO;
import com.google.gson.Gson;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.List;
import java.util.Map;

public class AuctionWebSocketClient extends WebSocketClient {

    private final Gson gson = new Gson();
    private final String jwtToken;
    private MessageListener messageListener;
    private Runnable onDisconnect;
    private Runnable onConnect;

    public interface MessageListener {
        void onInitialItemsReceived(List<Map<String, Object>> items);
        void onNewItemAdded(String itemId, String name, double price,
                            String seller, long endTime, String imageBase64);
        void onPriceUpdated(String itemId, String user, double newPrice, List<String> bidHistory);
        /** Cập nhật card ngoài grid. isFinished=true khi phiên đã kết thúc */
        void onGlobalPriceUpdate(String itemId, String user, double newPrice, boolean isFinished);
        void onSessionState(String itemId, double currentPrice, String highestBidder,
                            List<String> bidHistory, boolean isFinished, long endTime);
        void onAuctionEnded(String itemId, String winner, double finalPrice, List<String> bidHistory);
        void onError(String errorMessage);
    }

    public AuctionWebSocketClient(URI serverUri, String jwtToken) {
        super(serverUri);
        this.jwtToken = jwtToken;
    }

    public void setMessageListener(MessageListener l) { this.messageListener = l; }
    public void setOnDisconnect(Runnable callback)     { this.onDisconnect = callback; }
    public void setOnConnect(Runnable callback)         { this.onConnect = callback; }

    @Override
    public void onOpen(ServerHandshake handshake) {
        System.out.println("✅ Kết nối WebSocket thành công");
        if (onConnect != null) onConnect.run();
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("⚠ Mất kết nối WebSocket (code=" + code + ")");
        if (onDisconnect != null) onDisconnect.run();
    }

    @Override
    public void onError(Exception e) {
        System.err.println("❌ WebSocket error: " + e.getMessage());
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onMessage(String message) {
        try {
            ResponseDTO response = gson.fromJson(message, ResponseDTO.class);

            if ("error".equals(response.getStatus())) {
                if (messageListener != null) {
                    messageListener.onError(response.getMessage() != null
                            ? response.getMessage() : "Lỗi không xác định từ server");
                }
                return;
            }

            if (response.getData() == null || messageListener == null) return;

            Map<String, Object> data = (Map<String, Object>) response.getData();
            String type = (String) data.get("type");
            if (type == null) return;

            switch (type) {
                case "INITIAL_ITEMS": {
                    List<Map<String, Object>> items = (List<Map<String, Object>>) data.get("items");
                    messageListener.onInitialItemsReceived(items);
                    break;
                }
                case "NEW_ITEM_ADDED": {
                    String itemId     = parseId(data.get("itemId"));
                    String name       = (String) data.get("name");
                    double price      = ((Number) data.get("price")).doubleValue();
                    String seller     = (String) data.get("seller");
                    long   endTime    = ((Number) data.get("endTime")).longValue();
                    String imageBase64 = data.containsKey("image") ? (String) data.get("image") : "";
                    messageListener.onNewItemAdded(itemId, name, price, seller, endTime, imageBase64);
                    break;
                }
                case "UPDATE_PRICE": {
                    String itemId  = parseId(data.get("itemId"));
                    String user    = (String) data.get("user");
                    double price   = ((Number) data.get("price")).doubleValue();
                    List<String> history = data.containsKey("bidHistory")
                            ? (List<String>) data.get("bidHistory") : List.of();
                    messageListener.onPriceUpdated(itemId, user, price, history);
                    break;
                }
                case "PRICE_UPDATE_GLOBAL": {
                    String itemId    = parseId(data.get("itemId"));
                    String user      = (String) data.get("user");
                    double price     = ((Number) data.get("price")).doubleValue();
                    boolean finished = Boolean.TRUE.equals(data.get("isFinished"));
                    messageListener.onGlobalPriceUpdate(itemId, user, price, finished);
                    break;
                }
                case "SESSION_STATE": {
                    String itemId        = parseId(data.get("itemId"));
                    double currentPrice  = ((Number) data.get("currentPrice")).doubleValue();
                    String highestBidder = (String) data.get("highestBidder");
                    List<String> history = data.containsKey("bidHistory")
                            ? (List<String>) data.get("bidHistory") : List.of();
                    boolean isFinished   = Boolean.TRUE.equals(data.get("isFinished"));
                    long endTime         = ((Number) data.get("endTime")).longValue();
                    messageListener.onSessionState(itemId, currentPrice, highestBidder,
                            history, isFinished, endTime);
                    break;
                }
                case "AUCTION_ENDED": {
                    String itemId    = parseId(data.get("itemId"));
                    String winner    = (String) data.get("winner");
                    double finalPrice = ((Number) data.get("price")).doubleValue();
                    List<String> history = data.containsKey("bidHistory")
                            ? (List<String>) data.get("bidHistory") : List.of();
                    messageListener.onAuctionEnded(itemId, winner, finalPrice, history);
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Lỗi parse message: " + e.getMessage());
        }
    }

    public void sendBid(String id, long price) {
        send(String.format(
                "{\"action\":\"BID\",\"token\":\"%s\",\"itemId\":\"%s\",\"price\":%d}",
                jwtToken, id, price));
    }

    public void sendJoinRoom(String id) {
        send(String.format(
                "{\"action\":\"JOIN\",\"token\":\"%s\",\"itemId\":\"%s\"}",
                jwtToken, id));
    }

    private String parseId(Object raw) {
        if (raw == null) return "0";
        String s = String.valueOf(raw);
        return s.endsWith(".0") ? s.substring(0, s.length() - 2) : s;
    }
}