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

/**
 * Controller trung gian giữa UI và WebSocket client.
 * - Kết nối WebSocket đến server (tự reconnect nếu mất kết nối)
 * - Chuyển tiếp action từ UI xuống network (bid, join, post item)
 * - Nhận callback từ network và cập nhật UI trên JavaFX thread
 */
public class AuctionController implements AuctionWebSocketClient.MessageListener {

    private final AuctionUI ui;
    private final String currentUsername;
    private final String jwtToken;
    private AuctionWebSocketClient webSocketClient;
    private final Gson gson = new Gson();

    // Dùng để tránh nhiều luồng reconnect cùng lúc
    private final AtomicBoolean isReconnecting = new AtomicBoolean(false);
    private final ScheduledExecutorService reconnectScheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "ws-reconnect");
                t.setDaemon(true); // tự dừng khi app đóng
                return t;
            });

    public AuctionController(AuctionUI ui, String username, String token) {
        this.ui = ui;
        this.currentUsername = username;
        this.jwtToken = token;
        connectToServer();
    }

    // ── Kết nối / Reconnect ──────────────────────────────────────────────────

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

    /**
     * Lên lịch reconnect sau 3 giây. Chỉ chạy 1 lần tại một thời điểm.
     */
    private void scheduleReconnect() {
        if (isReconnecting.compareAndSet(false, true)) {
            reconnectScheduler.schedule(() -> {
                System.out.println("🔄 Đang thử kết nối lại...");
                connectToServer();
                isReconnecting.set(false);
            }, 3, TimeUnit.SECONDS);
        }
    }

    // ── Gửi action từ UI xuống server ───────────────────────────────────────

    /** Đăng sản phẩm mới lên đấu giá */
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

    /** Tham gia phòng đấu giá của sản phẩm */
    public void joinAuction(String itemId) {
        if (isConnected()) {
            webSocketClient.sendJoinRoom(itemId);
        }
    }

    /** Đặt giá cho sản phẩm */
    public void placeBid(String itemId, long amount) {
        if (!isConnected()) {
            Platform.runLater(() -> ui.showNotification("❌ Mất kết nối server!", "error"));
            return;
        }
        webSocketClient.sendBid(itemId, amount);
    }

    /**
     * Refresh danh sách sản phẩm.
     * Gửi action GET_ITEMS nếu đang kết nối, ngược lại reconnect.
     * KHÔNG đóng kết nối hiện tại để tránh vòng lặp reconnect.
     */
    public void fetchInitialProducts() {
        if (isConnected()) {
            // Yêu cầu server gửi lại INITIAL_ITEMS
            webSocketClient.send(String.format(
                    "{\"action\":\"GET_ITEMS\",\"token\":\"%s\"}", jwtToken));
        } else {
            connectToServer(); // kết nối lại nếu mất
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
    public void onGlobalPriceUpdate(String itemId, String user, double newPrice) {
        Platform.runLater(() -> ui.updatePrice(itemId, newPrice, user));
    }

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

    /** @deprecated Dùng constructor 3-param thay thế */
    public void setJwtToken(String token) { /* kept for compatibility */ }
}
