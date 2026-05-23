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

    public interface MessageListener {
        void onPriceUpdated(String itemId, String user, double newPrice);
        void onAuctionEnded(String itemId, String winner, double finalPrice);
        // SỬA Ở ĐÂY: Thêm tham số String imageBase64
        void onNewItemAdded(String itemId, String name, double price, String seller, long endTime, String imageBase64);
        void onInitialItemsReceived(List<Map<String, Object>> items);
        void onError(String errorMessage);
    }

    public AuctionWebSocketClient(URI serverUri, String jwtToken) {
        super(serverUri);
        this.jwtToken = jwtToken;
    }

    public void setMessageListener(MessageListener l) { this.messageListener = l; }

    @Override
    public void onMessage(String message) {
        try {
            ResponseDTO response = gson.fromJson(message, ResponseDTO.class);
            if (response != null && response.getData() != null && messageListener != null) {
                Map<String, Object> data = (Map<String, Object>) response.getData();
                String type = (String) data.get("type");

                if ("INITIAL_ITEMS".equals(type)) {
                    List<Map<String, Object>> items = (List<Map<String, Object>>) data.get("items");
                    messageListener.onInitialItemsReceived(items);
                } else if ("NEW_ITEM_ADDED".equals(type)) {
                    String itemId = String.valueOf(((Number) data.get("itemId")).intValue());
                    String name = (String) data.get("name");
                    double price = ((Number) data.get("price")).doubleValue();
                    String seller = (String) data.get("seller");
                    long endTime = ((Number) data.get("endTime")).longValue();

                    // SỬA Ở ĐÂY: Đọc ảnh từ Server gửi về (nếu có)
                    String imageBase64 = data.containsKey("image") ? (String) data.get("image") : "";

                    messageListener.onNewItemAdded(itemId, name, price, seller, endTime, imageBase64);
                } else if ("UPDATE_PRICE".equals(type)) {
                    messageListener.onPriceUpdated(String.valueOf(((Number)data.get("itemId")).intValue()), (String)data.get("user"), ((Number)data.get("price")).doubleValue());
                } else if ("AUCTION_ENDED".equals(type)) {
                    String itemId = String.valueOf(((Number) data.get("itemId")).intValue());
                    String winner = (String) data.get("winner");
                    double finalPrice = ((Number) data.get("price")).doubleValue();
                    messageListener.onAuctionEnded(itemId, winner, finalPrice);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    @Override public void onOpen(ServerHandshake h) { System.out.println("✅ Kết nối Server thành công"); }
    @Override public void onClose(int i, String s, boolean b) {}
    @Override public void onError(Exception e) {}

    public void sendBid(String id, long p) {
        this.send(String.format("{\"action\":\"BID\", \"token\":\"%s\", \"itemId\":\"%s\", \"price\":%d}", jwtToken, id, p));
    }
    public void sendJoinRoom(String id) {
        this.send(String.format("{\"action\":\"JOIN\", \"token\":\"%s\", \"itemId\":\"%s\"}", jwtToken, id));
    }
}