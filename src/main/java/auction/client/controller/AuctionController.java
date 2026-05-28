package auction.client.controller;

import auction.client.ClientMain;
import auction.client.network.AuctionWebSocketClient;
import auction.client.network.AdminItemClient;
import auction.client.ui.AuctionUI;
import com.google.gson.Gson;
import javafx.application.Platform;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class AuctionController implements AuctionWebSocketClient.MessageListener {

    private final AuctionUI ui;
    private final String currentUsername;
    private final String jwtToken;
    private AuctionWebSocketClient webSocketClient;
    private final Gson gson = new Gson();

    private final AtomicBoolean isReconnecting = new AtomicBoolean(false);
    private final ScheduledExecutorService reconnectScheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "ws-reconnect");
                t.setDaemon(true);
                return t;
            });

    public AuctionController(AuctionUI ui, String username, String token) {
        this.ui = ui;
        this.currentUsername = username;
        this.jwtToken = token;
        connectToServer();
    }

    private void connectToServer() {
        try {
            webSocketClient = new AuctionWebSocketClient(
                    new URI("ws://localhost:8081/auction"), jwtToken);
            webSocketClient.setMessageListener(this);
            webSocketClient.setOnDisconnect(this::scheduleReconnect);
            webSocketClient.connect();
            System.out.println("🔌 Đang kết nối WebSocket...");
        } catch (Exception e) {
            System.err.println("❌ Lỗi kết nối WebSocket: " + e.getMessage());
            scheduleReconnect();
        }
    }

    private void scheduleReconnect() {
        if (isReconnecting.compareAndSet(false, true)) {
            reconnectScheduler.schedule(() -> {
                System.out.println("🔄 Đang thử kết nối lại...");
                connectToServer();
                isReconnecting.set(false);
            }, 3, TimeUnit.SECONDS);
        }
    }

    // ── Gửi action ────────────────────────────────────────────────────────────

    public void postNewItem(String name, String desc, double price, int duration, String imageBase64) {
        if (!isConnected()) {
            Platform.runLater(() -> ui.showNotification("❌ Chưa kết nối server, vui lòng thử lại!", "error"));
            return;
        }
        String msg = gson.toJson(Map.of(
                "action",      "POST_ITEM",
                "token",       jwtToken,
                "name",        name,
                "description", desc,
                "price",       price,
                "duration",    duration,
                "image",       imageBase64 != null ? imageBase64 : ""
        ));
        webSocketClient.send(msg);
    }

    public void joinAuction(String itemId) {
        if (isConnected()) webSocketClient.sendJoinRoom(itemId);
    }

    public void placeBid(String itemId, long amount) {
        if (!isConnected()) {
            Platform.runLater(() -> ui.showNotification("❌ Mất kết nối server!", "error"));
            return;
        }
        webSocketClient.sendBid(itemId, amount);
    }

    public void fetchInitialProducts() {
        if (isConnected()) {
            webSocketClient.send(String.format(
                    "{\"action\":\"GET_ITEMS\",\"token\":\"%s\"}", jwtToken));
        } else {
            connectToServer();
        }
    }

    private boolean isConnected() {
        return webSocketClient != null && webSocketClient.isOpen();
    }

    // ── MessageListener callbacks ────────────────────────────────────────────

    @Override
    public void onInitialItemsReceived(List<Map<String, Object>> items) {
        Platform.runLater(() -> ui.onInitialItemsReceived(items));
    }

    @Override
    public void onNewItemAdded(String itemId, String name, double price,
                               String seller, long endTime, String imageBase64) {
        Platform.runLater(() -> ui.onNewItemAdded(itemId, name, price, seller, endTime, imageBase64));
    }

    @Override
    public void onPriceUpdated(String itemId, String user, double newPrice, List<String> bidHistory) {
        Platform.runLater(() -> {
            ui.updatePrice(itemId, newPrice, user);
            ui.updateBidHistory(itemId, bidHistory);
        });
    }

    @Override
    public void onGlobalPriceUpdate(String itemId, String user, double newPrice, boolean isFinished) {
        Platform.runLater(() -> {
            ui.updatePrice(itemId, newPrice, user);
            if (isFinished) {
                // Khi phiên kết thúc: disable bid, đổi label, cập nhật card
                ui.markAuctionFinished(itemId);
            }
        });
    }

    @Override
    public void onSessionState(String itemId, double currentPrice, String highestBidder,
                               List<String> bidHistory, boolean isFinished, long endTime) {
        Platform.runLater(() -> {
            ui.updatePrice(itemId, currentPrice, highestBidder);
            ui.updateBidHistory(itemId, bidHistory);
            // Cập nhật endTime cho sản phẩm (quan trọng khi client join lại sau restart)
            ui.updateProductEndTime(itemId, endTime);
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
            ui.markAuctionFinished(itemId);
            ui.showAuctionEnded(itemId, winner, finalPrice);
        });
    }

    @Override
    public void onError(String message) {
        Platform.runLater(() -> ui.showNotification("❌ " + message, "error"));
    }

    // ── ADMIN operations (HTTP) ──────────────────────────────────────────────

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

    public void setJwtToken(String token) { /* kept for compatibility */ }
}