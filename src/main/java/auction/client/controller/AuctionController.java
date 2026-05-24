package auction.client.controller;

import auction.client.network.AuctionWebSocketClient;
import auction.client.ui.AuctionUI;
import auction.client.ui.ProductItem;
import auction.shared.dto.ItemDTO;
import auction.shared.util.JwtUtil;
import com.google.gson.Gson;
import javafx.application.Platform;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuctionController implements AuctionWebSocketClient.MessageListener {
    private final AuctionUI ui;
    private String currentUsername;
    private AuctionWebSocketClient webSocketClient;
    private final Gson gson = new Gson();

    public AuctionController(AuctionUI ui) { this(ui, "Guest"); }

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

    public void postNewItem(ItemDTO itemDTO) {
        if (webSocketClient != null && webSocketClient.isOpen()) {
            String token = JwtUtil.createToken(currentUsername, "SELLER");

            Map<String, Object> messageMap = new HashMap<>();
            messageMap.put("action", "POST_ITEM");
            messageMap.put("token", token);
            messageMap.put("name", itemDTO.getName());
            messageMap.put("description", itemDTO.getDescription());
            messageMap.put("price", itemDTO.getStartingPrice());

            String jsonPayload = gson.toJson(messageMap);
            webSocketClient.send(jsonPayload);

            Platform.runLater(() -> {
                try { Thread.sleep(400); } catch (Exception ignored) {}
                refreshData();
            });
        }
    }

    // ĐÃ SỬA: Bọc kiểm tra dữ liệu thông minh chống Null và lỗi lệch cấu trúc JSON giữa các phiên bản Server
    public void refreshData() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/items"))
                    .GET()
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpResponse::body)
                    .thenAccept(body -> {
                        Map<String, Object> responseMap = gson.fromJson(body, Map.class);
                        if (responseMap != null && "success".equals(responseMap.get("status"))) {
                            List<Map<String, Object>> items = (List<Map<String, Object>>) responseMap.get("data");
                            Platform.runLater(() -> {
                                ui.clearTable();
                                for (Map<String, Object> item : items) {
                                    // Sửa lỗi NullPointerException: Thử lấy cả 2 key phổ biến nhất từ server
                                    Object idObj = item.get("item_id");
                                    if (idObj == null) {
                                        idObj = item.get("id");
                                    }

                                    // Chuyển đổi ID sang String an toàn tuyệt đối không lo lỗi kiểu dữ liệu
                                    String id = "0";
                                    if (idObj instanceof Double) {
                                        id = String.valueOf(((Double) idObj).intValue());
                                    } else if (idObj != null) {
                                        id = idObj.toString();
                                    }

                                    String name = item.get("name") != null ? item.get("name").toString() : "Không tên";

                                    Object priceObj = item.get("starting_price");
                                    if (priceObj == null) priceObj = item.get("startingPrice");
                                    long price = priceObj instanceof Number ? ((Number) priceObj).longValue() : 0L;

                                    Object sellerObj = item.get("seller_username");
                                    if (sellerObj == null) sellerObj = item.get("seller");
                                    String seller = sellerObj != null ? sellerObj.toString() : "Hệ thống";

                                    long endTime = System.currentTimeMillis() + (120 * 1000L);
                                    ui.addProduct(new ProductItem(id, name, price, "---", "Đang đấu", seller, endTime));
                                }
                                ui.appendLog("Đã cập nhật danh sách từ Database qua HTTP thành công.");
                            });
                        }
                    }).exceptionally(ex -> {
                        Platform.runLater(() -> ui.appendLog("Lỗi nạp HTTP: " + ex.getMessage()));
                        return null;
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ĐÃ SỬA: Ép kiểu ID an toàn bằng .toString() để tránh lỗi ClassCastException sập WebSocket
    @Override
    public void onInitialItemsReceived(List<Map<String, Object>> items) {
        Platform.runLater(() -> {
            ui.clearTable();
            for (Map<String, Object> item : items) {
                Object idObj = item.get("id") != null ? item.get("id") : item.get("item_id");
                String id = idObj != null ? idObj.toString() : "0";
                if (idObj instanceof Double) {
                    id = String.valueOf(((Double) idObj).intValue());
                }

                String name = item.get("name") != null ? item.get("name").toString() : "";

                Object priceObj = item.get("startingPrice") != null ? item.get("startingPrice") : item.get("starting_price");
                long price = priceObj instanceof Number ? ((Number) priceObj).longValue() : 0L;

                Object sellerObj = item.get("seller") != null ? item.get("seller") : item.get("seller_username");
                String seller = sellerObj != null ? sellerObj.toString() : "";

                long endTime = 0;
                if (item.containsKey("endTime") && item.get("endTime") != null) {
                    endTime = ((Number) item.get("endTime")).longValue();
                }
                ui.addProduct(new ProductItem(id, name, price, "---", "Đang đấu", seller, endTime));
            }
        });
    }

    // ĐÃ SỬA: Bảo vệ hàm nhận dữ liệu realtime khi có sản phẩm mới
    @Override
    public void onNewItemAdded(String itemId, String name, double price, String seller) {
        Platform.runLater(() -> {
            // Chuyển đổi định dạng ID nếu bị dính phần thập phân của WebSocket (Ví dụ "5.0" -> "5")
            String cleanId = itemId;
            if (itemId != null && itemId.contains(".")) {
                try {
                    cleanId = String.valueOf((int) Double.parseDouble(itemId));
                } catch (Exception ignored) {}
            }
            long endTime = System.currentTimeMillis() + (120 * 1000L);
            ui.addProduct(new ProductItem(cleanId, name, (long)price, "---", "Đang đấu", seller, endTime));
            ui.showNotification("✨ Sản phẩm mới: " + name, "success");
        });
    }

    @Override public void onPriceUpdated(String id, String u, double p) { ui.updatePrice(id, (long)p, u); }
    @Override public void onAuctionEnded(String id, String w, double p) { ui.showAuctionEnded(id, w, (long)p); }
    @Override public void onError(String msg) { Platform.runLater(() -> ui.showNotification(msg, "error")); }

    public void joinAuction(String id) {
        if (webSocketClient != null) webSocketClient.sendJoinRoom(id);
        ui.enableBidButton();
    }

    public void placeBid(String id, long a) {
        if (webSocketClient != null) webSocketClient.sendBid(id, a);
    }
    /**
     * Gọi API GET /items để lấy toàn bộ sản phẩm từ Database khi khởi động
     */
    public void fetchInitialProducts() {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/items"))
                .GET()
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(responseBody -> {
                    try {
                        JsonObject json = gson.fromJson(responseBody, JsonObject.class);
                        if ("success".equals(json.get("status").getAsString())) {
                            JsonArray items = json.getAsJsonArray("data");

                            Platform.runLater(() -> {
                                ui.clearTable(); // Xóa sạch dữ liệu cũ trên màn hình

                                for (JsonElement elem : items) {
                                    JsonObject item = elem.getAsJsonObject();
                                    String id = item.get("id").getAsString();
                                    String name = item.get("name").getAsString();
                                    long price = item.get("startingPrice").getAsLong();

                                    // Xử lý tên người bán (Seller)
                                    String seller = "Unknown";
                                    if (item.has("sellerUserName") && !item.get("sellerUserName").isJsonNull()) {
                                        seller = item.get("sellerUserName").getAsString();
                                    }

                                    // Tạm thời cho mỗi phiên kéo dài 2 phút từ lúc lấy lên UI
                                    long endTime = System.currentTimeMillis() + (120 * 1000L);

                                    // Thêm vào lưới
                                    ui.addProduct(new ProductItem(id, name, price, "---", "Đang đấu", seller, endTime));
                                }
                                ui.appendLog("Đã tải " + items.size() + " sản phẩm từ Database.");
                            });
                        }
                    } catch (Exception e) {
                        System.err.println("Lỗi parse JSON danh sách sản phẩm: " + e.getMessage());
                    }
                })
                .exceptionally(e -> {
                    System.err.println("Không thể kết nối API lấy sản phẩm: " + e.getMessage());
                    return null;
                });
    }
}