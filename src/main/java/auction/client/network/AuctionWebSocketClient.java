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
        /** Nhận danh sách sản phẩm ban đầu khi mới kết nối */
        void onInitialItemsReceived(List<Map<String, Object>> items);

        /** Sản phẩm mới được đăng lên */
        void onNewItemAdded(String itemId, String name, double price, String seller,
                            long endTime, String imageBase64);

        /**
         * Cập nhật giá trong phòng đấu giá (kèm lịch sử đầy đủ).
         * Được gọi khi đang ở trong detail panel của sản phẩm.
         */
        void onPriceUpdated(String itemId, String user, double newPrice, List<String> bidHistory);

        /**
         * Cập nhật giá toàn cục (broadcast ra ngoài grid).
         * Được gọi để refresh card ngoài danh sách.
         */
        void onGlobalPriceUpdate(String itemId, String user, double newPrice);

        /**
         * Nhận trạng thái phiên khi vừa JOIN phòng:
         * giá hiện tại, người dẫn đầu, lịch sử đầy đủ.
         */
        void onSessionState(String itemId, double currentPrice, String highestBidder,
                            List<String> bidHistory, boolean isFinished, long endTime);

        /** Phiên đấu giá kết thúc */
        void onAuctionEnded(String itemId, String winner, double finalPrice, List<String> bidHistory);

        /** Lỗi từ server */
        void onError(String errorMessage);
    }

    public AuctionWebSocketClient(URI serverUri, String jwtToken) {
        super(serverUri);
        this.jwtToken = jwtToken;
    }

    public void setMessageListener(MessageListener l) { this.messageListener = l; }

    @SuppressWarnings("unchecked")
    @Override
    public void onMessage(String message) {
        try {
            ResponseDTO response = gson.fromJson(message, ResponseDTO.class);

            // Xử lý lỗi từ server
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
                    String itemId = parseIntId(data.get("itemId"));
                    String name = (String) data.get("name");
                    double price = ((Number) data.get("price")).doubleValue();
                    String seller = (String) data.get("seller");
                    long endTime = ((Number) data.get("endTime")).longValue();
                    String imageBase64 = data.containsKey("image") ? (String) data.get("image") : "";
                    messageListener.onNewItemAdded(itemId, name, price, seller, endTime, imageBase64);
                    break;
                }
                case "UPDATE_PRICE": {
                    // Trong phòng: có kèm lịch sử
                    String itemId = parseIntId(data.get("itemId"));
                    String user = (String) data.get("user");
                    double price = ((Number) data.get("price")).doubleValue();
                    List<String> history = data.containsKey("bidHistory")
                            ? (List<String>) data.get("bidHistory") : List.of();
                    messageListener.onPriceUpdated(itemId, user, price, history);
                    break;
                }
                case "PRICE_UPDATE_GLOBAL": {
                    // Ngoài grid: chỉ cập nhật giá trên card
                    String itemId = parseIntId(data.get("itemId"));
                    String user = (String) data.get("user");
                    double price = ((Number) data.get("price")).doubleValue();
                    messageListener.onGlobalPriceUpdate(itemId, user, price);
                    break;
                }
                case "SESSION_STATE": {
                    // Nhận khi vừa JOIN phòng
                    String itemId = parseIntId(data.get("itemId"));
                    double currentPrice = ((Number) data.get("currentPrice")).doubleValue();
                    String highestBidder = (String) data.get("highestBidder");
                    List<String> history = data.containsKey("bidHistory")
                            ? (List<String>) data.get("bidHistory") : List.of();
                    boolean isFinished = data.containsKey("isFinished")
                            && Boolean.TRUE.equals(data.get("isFinished"));
                    long endTime = ((Number) data.get("endTime")).longValue();
                    messageListener.onSessionState(itemId, currentPrice, highestBidder,
                            history, isFinished, endTime);
                    break;
                }
                case "AUCTION_ENDED": {
                    String itemId = parseIntId(data.get("itemId"));
                    String winner = (String) data.get("winner");
                    double finalPrice = ((Number) data.get("price")).doubleValue();
                    List<String> history = data.containsKey("bidHistory")
                            ? (List<String>) data.get("bidHistory") : List.of();
                    messageListener.onAuctionEnded(itemId, winner, finalPrice, history);
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Parse id dạng "1" hoặc "1.0" → "1" */
    private String parseIntId(Object raw) {
        if (raw == null) return "0";
        String s = String.valueOf(raw);
        if (s.endsWith(".0")) s = s.substring(0, s.length() - 2);
        return s;
    }

    @Override public void onOpen(ServerHandshake h) {
        System.out.println("✅ Kết nối Server thành công");
    }
    @Override public void onClose(int i, String s, boolean b) {
        System.out.println("⚠ Mất kết nối server");
    }
    @Override public void onError(Exception e) {
        System.err.println("❌ WebSocket error: " + e.getMessage());
    }

    public void sendBid(String id, long price) {
        send(String.format(
                "{\"action\":\"BID\", \"token\":\"%s\", \"itemId\":\"%s\", \"price\":%d}",
                jwtToken, id, price));
    }

    public void sendJoinRoom(String id) {
        send(String.format(
                "{\"action\":\"JOIN\", \"token\":\"%s\", \"itemId\":\"%s\"}",
                jwtToken, id));
    }
}