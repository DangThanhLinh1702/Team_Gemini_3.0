package auction.client.controller;

import auction.client.network.AuctionWebSocketClient;
import auction.client.ui.AuctionUI;
import auction.client.ui.ProductItem;
import auction.shared.util.JwtUtil;
import com.google.gson.Gson;
import javafx.application.Platform;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuctionController implements AuctionWebSocketClient.MessageListener {
    private final AuctionUI ui;
    private String currentUsername;
    private AuctionWebSocketClient webSocketClient;
    private final Gson gson = new Gson();

    public AuctionController(AuctionUI ui, String username) {
        this.ui = ui;
        this.currentUsername = username;
        connectToServer();
    }

    private void connectToServer() {
        try {
            String token = JwtUtil.createToken(currentUsername, "USER");
            webSocketClient = new AuctionWebSocketClient(new URI("ws://localhost:8081/auction"), token);
            webSocketClient.setMessageListener(this);
            webSocketClient.connect();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void postNewItem(String name, String desc, double price, int duration, String imageBase64) {
        if (webSocketClient != null && webSocketClient.isOpen()) {
            Map<String, Object> msg = new HashMap<>();
            msg.put("action", "POST_ITEM");
            msg.put("token", JwtUtil.createToken(currentUsername, "SELLER"));
            msg.put("name", name);
            msg.put("description", desc);
            msg.put("price", price);
            msg.put("duration", duration);
            msg.put("image", imageBase64);

            webSocketClient.send(gson.toJson(msg));
        }
    }

    @Override
    public void onNewItemAdded(String itemId, String name, double price, String seller, long endTime, String imageBase64) {
        Platform.runLater(() -> {
            ProductItem newItem = new ProductItem(itemId, name, (long)price, "---", "Đang đấu", seller, endTime);
            newItem.setImageBase64(imageBase64); // Nhét ảnh vào Item
            ui.addProduct(newItem);
            ui.showNotification("✨ Sản phẩm mới: " + name, "success");
        });
    }

    @Override
    public void onInitialItemsReceived(List<Map<String, Object>> items) {
        Platform.runLater(() -> {
            ui.clearTable();
            for (Map<String, Object> item : items) {
                try {
                    String id = String.valueOf(item.get("id"));
                    String name = (String) item.get("name");

                    // Ép kiểu an toàn bằng cách chuyển sang String rồi parse ra số
                    long price = (long) Double.parseDouble(String.valueOf(item.get("startingPrice")));
                    String seller = (String) item.get("seller");
                    long endTime = (long) Double.parseDouble(String.valueOf(item.get("endTime")));

                    // --- ĐÃ SỬA Ở ĐÂY: Lấy chuỗi ảnh từ dữ liệu Server gửi về ---
                    String imageBase64 = "";
                    if (item.containsKey("image") && item.get("image") != null) {
                        imageBase64 = (String) item.get("image");
                    }

                    // --- ĐÃ SỬA Ở ĐÂY: Khởi tạo ProductItem và gán ảnh vào ---
                    ProductItem productItem = new ProductItem(id, name, price, "---", "Đang đấu", seller, endTime);
                    productItem.setImageBase64(imageBase64);

                    ui.addProduct(productItem);
                } catch (Exception e) {
                    System.err.println("Lỗi parse dữ liệu sản phẩm: " + e.getMessage());
                }
            }
        });
    }

    @Override public void onPriceUpdated(String id, String u, double p) { ui.updatePrice(id, (long)p, u); }
    @Override public void onAuctionEnded(String id, String w, double p) { ui.showAuctionEnded(id, w, (long)p); }
    @Override public void onError(String msg) { Platform.runLater(() -> ui.showNotification(msg, "error")); }
    public void joinAuction(String id) { if (webSocketClient != null) webSocketClient.sendJoinRoom(id); ui.enableBidButton(); }
    public void placeBid(String id, long a) { if (webSocketClient != null) webSocketClient.sendBid(id, a); }
    public void fetchInitialProducts() {}
}