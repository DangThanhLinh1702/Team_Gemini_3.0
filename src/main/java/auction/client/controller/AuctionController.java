package auction.client.controller;

import auction.client.network.AuctionWebSocketClient;
import auction.client.ui.AuctionUI;
import auction.client.ui.ProductItem;
import auction.shared.dto.ItemDTO;
import auction.shared.util.JwtUtil;
import auction.database.AuctionService;
import auction.shared.model.AuctionItem;
import javafx.application.Platform;
import java.net.URI;
import java.util.List;
import java.util.Map;

public class AuctionController implements AuctionWebSocketClient.MessageListener {
    private final AuctionUI ui;
    private String currentUsername;
    private AuctionWebSocketClient webSocketClient;
    private final AuctionService auctionService;

    public AuctionController(AuctionUI ui) { this(ui, "Guest"); }

    public AuctionController(AuctionUI ui, String username) {
        this.ui = ui;
        this.currentUsername = username;
        this.auctionService = new AuctionService();
        connectToServer();
        syncDataFromDatabase();
    }

    private void connectToServer() {
        try {
            String token = JwtUtil.createToken(currentUsername, "USER");
            webSocketClient = new AuctionWebSocketClient(new URI("ws://localhost:8081/auction"), token);
            webSocketClient.setMessageListener(this);
            webSocketClient.connect();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void postNewItem(ItemDTO itemDTO) {
        if (webSocketClient != null && webSocketClient.isOpen()) {
            String token = JwtUtil.createToken(currentUsername, "SELLER");
            String json = String.format(
                    "{\"action\":\"POST_ITEM\", \"token\":\"%s\", \"name\":\"%s\", \"description\":\"%s\", \"price\":%f}",
                    token, itemDTO.getName(), itemDTO.getDescription(), itemDTO.getStartingPrice()
            );
            webSocketClient.send(json);
        }
    }

    @Override
    public void onInitialItemsReceived(List<Map<String, Object>> items) {
        Platform.runLater(() -> {
            ui.clearTable();
            for (Map<String, Object> item : items) {
                String id = (String) item.get("id");
                String name = (String) item.get("name");
                long price = ((Number) item.get("startingPrice")).longValue();

                String seller = (String) item.get("seller");
                long endTime = 0;
                if (item.containsKey("endTime")) {
                    endTime = ((Number) item.get("endTime")).longValue();
                }
                ui.addProduct(new ProductItem(id, name, price, "---", "Đang đấu", seller, endTime));
            }
        });
    }

    @Override
    public void onNewItemAdded(String itemId, String name, double price, String seller) {
        Platform.runLater(() -> {
            long endTime = System.currentTimeMillis() + (120 * 1000L);
            ui.addProduct(new ProductItem(itemId, name, (long)price, "---", "Đang đấu", seller, endTime));
            ui.showNotification("✨ Sản phẩm mới: " + name, "success");
        });
    }

    @Override public void onPriceUpdated(String id, String u, double p) { ui.updatePrice(id, (long)p, u); }

    // ĐÃ SỬA: Cập nhật hàm này để truyền cả id sản phẩm xuống UI
    @Override public void onAuctionEnded(String id, String w, double p) { ui.showAuctionEnded(id, w, (long)p); }

    @Override public void onError(String msg) { Platform.runLater(() -> ui.showNotification(msg, "error")); }

    public void joinAuction(String id) {
        if (webSocketClient != null) webSocketClient.sendJoinRoom(id);
        ui.enableBidButton();
    }

    public void placeBid(String id, long a) {
        if (webSocketClient != null) webSocketClient.sendBid(id, a);
    }
    
    // Đồng bộ dữ liệu từ database lên UI
    public void syncDataFromDatabase() {
        new Thread(() -> {
            try {
                auctionService.syncDataToUI(ui);
                auctionService.checkAndUpdateExpiredAuctions();
            } catch (Exception e) {
                Platform.runLater(() -> {
                    ui.showNotification("Lỗi đồng bộ dữ liệu: " + e.getMessage(), "error");
                    ui.appendLog("Lỗi khi đồng bộ dữ liệu từ database: " + e.getMessage());
                });
            }
        }).start();
    }
    
    // Làm mới dữ liệu từ database
    public void refreshData() {
        syncDataFromDatabase();
    }
    
    // Thêm sản phẩm mới vào database
    public void addNewItemToDatabase(ItemDTO itemDTO) {
        new Thread(() -> {
            try {
                AuctionItem auctionItem = new AuctionItem(
                    null,
                    itemDTO.getName(),
                    itemDTO.getDescription(),
                    (long) itemDTO.getStartingPrice(),
                    itemDTO.getSellerUsername(),
                    java.time.LocalDateTime.now().plusMinutes(120) // 2 phút
                );
                
                boolean success = auctionService.addAuctionItem(auctionItem);
                
                Platform.runLater(() -> {
                    if (success) {
                        ui.showNotification("Đăng sản phẩm thành công", "success");
                        ui.appendLog("Đã đăng sản phẩm: " + itemDTO.getName());
                        syncDataFromDatabase(); // Làm mới UI
                    } else {
                        ui.showNotification("Đăng sản phẩm thất bại", "error");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    ui.showNotification("Lỗi: " + e.getMessage(), "error");
                    ui.appendLog("Lỗi khi đăng sản phẩm: " + e.getMessage());
                });
            }
        }).start();
    }
}