package auction.client.controller;

import auction.client.ClientMain;
import auction.client.network.AuctionWebSocketClient;
import auction.client.network.AdminItemClient;
import auction.client.ui.AuctionUI;
import auction.shared.util.JwtUtil;
import com.google.gson.Gson;
import javafx.application.Platform;

import java.net.URI;
import java.util.List;
import java.util.Map;

public class AuctionController implements AuctionWebSocketClient.MessageListener {
    private final AuctionUI ui;
    private final String currentUsername;
    private AuctionWebSocketClient webSocketClient;
    private final Gson gson = new Gson();

    public AuctionController(AuctionUI ui, String username) {
        this.ui = ui;
        this.currentUsername = username;
        connectToServer();
    }

    public void setJwtToken(String token) { /* kept for compatibility */ }

    private void connectToServer() {
        try {
            String token = JwtUtil.createToken(currentUsername, "USER");
            webSocketClient = new AuctionWebSocketClient(
                    new URI("ws://localhost:8081/auction"), token);
            webSocketClient.setMessageListener(this);
            webSocketClient.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ─── Gửi đăng sản phẩm mới ──────────────────────────────────────────────
    public void postNewItem(String name, String desc, double price, int duration, String imageBase64) {
        if (webSocketClient == null || !webSocketClient.isOpen()) return;
        String msg = gson.toJson(Map.of(
                "action", "POST_ITEM",
                "token", JwtUtil.createToken(currentUsername, "SELLER"),
                "name", name,
                "description", desc,
                "price", price,
                "duration", duration,
                "image", imageBase64 != null ? imageBase64 : ""
        ));
        webSocketClient.send(msg);
    }

    // ─── MessageListener callbacks ───────────────────────────────────────────

    @Override
    public void onInitialItemsReceived(List<Map<String, Object>> items) {
        Platform.runLater(() -> ui.onInitialItemsReceived(items));
    }

    @Override
    public void onNewItemAdded(String itemId, String name, double price,
                               String seller, long endTime, String imageBase64) {
        Platform.runLater(() -> ui.onNewItemAdded(itemId, name, price, seller, endTime, imageBase64));
    }

    /**
     * Nhận UPDATE_PRICE từ phòng: cập nhật giá + lịch sử trong detail panel
     * VÀ cập nhật card ngoài grid (nếu đang ở grid view).
     */
    @Override
    public void onPriceUpdated(String itemId, String user, double newPrice, List<String> bidHistory) {
        Platform.runLater(() -> {
            ui.updatePrice(itemId, newPrice, user);           // cập nhật card + sidebar
            ui.updateBidHistory(itemId, bidHistory);          // cập nhật listBidHistory trong detail
        });
    }

    /**
     * PRICE_UPDATE_GLOBAL: chỉ cập nhật giá trên card ngoài danh sách grid.
     */
    @Override
    public void onGlobalPriceUpdate(String itemId, String user, double newPrice) {
        Platform.runLater(() -> ui.updatePrice(itemId, newPrice, user));
    }

    /**
     * SESSION_STATE: nhận khi vừa JOIN phòng — đổ toàn bộ state hiện tại vào detail panel.
     */
    @Override
    public void onSessionState(String itemId, double currentPrice, String highestBidder,
                               List<String> bidHistory, boolean isFinished, long endTime) {
        Platform.runLater(() -> {
            ui.updatePrice(itemId, currentPrice, highestBidder);
            ui.updateBidHistory(itemId, bidHistory);
            if (isFinished) {
                ui.markAuctionFinished(itemId);
            } else {
                ui.enableBidButton();
            }
        });
    }

    @Override
    public void onAuctionEnded(String itemId, String winner, double finalPrice, List<String> bidHistory) {
        Platform.runLater(() -> {
            ui.updateBidHistory(itemId, bidHistory);
            ui.showAuctionEnded(itemId, winner, finalPrice);
            ui.markAuctionFinished(itemId);
        });
    }

    @Override
    public void onError(String msg) {
        Platform.runLater(() -> ui.showNotification("❌ " + msg, "error"));
    }

    // ─── Public actions ──────────────────────────────────────────────────────

    public void joinAuction(String id) {
        if (webSocketClient != null && webSocketClient.isOpen()) {
            webSocketClient.sendJoinRoom(id);
        }
    }

    public void placeBid(String id, long amount) {
        if (webSocketClient != null && webSocketClient.isOpen()) {
            webSocketClient.sendBid(id, amount);
        }
    }

    public void fetchInitialProducts() { /* Server tự gửi khi kết nối */ }

    // ─── ADMIN operations ────────────────────────────────────────────────────
    public AdminItemClient.Result deleteItem(String itemId) {
        return AdminItemClient.deleteItem(ClientMain.getJwtToken(), itemId);
    }
    public AdminItemClient.Result updateItem(String itemId, String name,
                                             String description, double price) {
        return AdminItemClient.updateItem(ClientMain.getJwtToken(), itemId, name, description, price);
    }
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