package auction.client.controller;

import auction.client.ClientMain; // <--- ĐÃ THÊM: Import ClientMain để lấy Token toàn cục
import auction.client.network.AuctionWebSocketClient;
import auction.client.network.AdminItemClient;
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
    private String jwtToken; // Token cục bộ (Có thể giữ lại hoặc bỏ)

    public AuctionController(AuctionUI ui, String username) {
        this.ui = ui;
        this.currentUsername = username;
        connectToServer();
    }

    /** Lưu JWT token để dùng cho các lệnh ADMIN */
    public void setJwtToken(String token) {
        this.jwtToken = token;
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
            newItem.setImageBase64(imageBase64);
            ui.addProduct(newItem);
            ui.showNotification("✨ Sản phẩm mới: " + name, "success");
        });
    }

    public void onInitialItemsReceived(List<Map<String, Object>> items) {
        Platform.runLater(() -> {
            ui.clearTable();
            for (Map<String, Object> item : items) {
                try {
                    // ✅ ĐÃ SỬA: Lấy chuỗi ID thô ra trước (có thể là "1" hoặc "1.0")
                    String idRaw = String.valueOf(item.get("id"));
                    // ✅ Xử lý: Nếu chuỗi kết thúc bằng ".0" thì chặt bỏ 2 ký tự cuối đi
                    String id = idRaw.endsWith(".0") ? idRaw.substring(0, idRaw.length() - 2) : idRaw;

                    String name = (String) item.get("name");

                    // Các phần khác giữ nguyên...
                    long price = (long) Double.parseDouble(String.valueOf(item.get("startingPrice")));
                    String seller = (String) item.get("seller");
                    long endTime = (long) Double.parseDouble(String.valueOf(item.get("endTime")));

                    String imageBase64 = "";
                    if (item.containsKey("image") && item.get("image") != null) {
                        imageBase64 = (String) item.get("image");
                    }

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

    // ── ADMIN: Xóa sản phẩm + phiên đấu giá ─────────────────────────────────
    // ĐÃ SỬA: Lấy token trực tiếp từ ClientMain.getJwtToken()
    public AdminItemClient.Result deleteItem(String itemId) {
        return AdminItemClient.deleteItem(ClientMain.getJwtToken(), itemId);
    }

    // ── ADMIN: Sửa sản phẩm + phiên đấu giá ─────────────────────────────────
    // ĐÃ SỬA: Lấy token trực tiếp từ ClientMain.getJwtToken()
    public AdminItemClient.Result updateItem(String itemId, String name,
                                             String description, double price) {
        System.out.println(">>> KIỂM TRA TOKEN TRONG CONTROLLER: [" + ClientMain.getJwtToken() + "]");
        return AdminItemClient.updateItem(ClientMain.getJwtToken(), itemId, name, description, price);
    }

    // ── ADMIN: Quản lý User ───────────────────────────────────────────────────
    public AdminItemClient.Result fetchUsers() {
        return AdminItemClient.fetchUsers(ClientMain.getJwtToken());
    }

    public AdminItemClient.Result deleteUser(String username) {
        return AdminItemClient.deleteUser(ClientMain.getJwtToken(), username);
    }

    public AdminItemClient.Result blockUser(String username) {
        return AdminItemClient.blockUser(ClientMain.getJwtToken(), username);
    }

    public AdminItemClient.Result unblockUser(String username) {
        return AdminItemClient.unblockUser(ClientMain.getJwtToken(), username);
    }
}