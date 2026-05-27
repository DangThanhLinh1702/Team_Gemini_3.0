package auction.server.core;

import auction.server.model.AuctionSession;
import auction.server.model.Item;
import auction.server.service.ItemService;
import auction.server.repository.UserRepository;
import auction.shared.dto.WebSocketRequestDTO;
import auction.shared.util.JwtUtil;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.Gson;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket Server xử lý toàn bộ giao tiếp realtime giữa server và các client.
 *
 * Các action được hỗ trợ:
 *  - POST_ITEM : Đăng sản phẩm mới, tạo phiên đấu giá, broadcast toàn client
 *  - JOIN      : Tham gia phòng đấu giá, nhận trạng thái hiện tại
 *  - BID       : Đặt giá, validate, cập nhật DB, broadcast toàn phòng + grid
 */
public class AuctionWebSocketServer extends WebSocketServer {

    private final Gson gson = new Gson();

    // Map: itemId → tập hợp client đang ở trong phòng đấu giá đó
    private final ConcurrentHashMap<Integer, Set<WebSocket>> auctionRooms = new ConcurrentHashMap<>();

    private final ItemService itemService = new ItemService();
    private final UserRepository userRepository = new UserRepository();

    public AuctionWebSocketServer(int port) {
        super(new InetSocketAddress(port));

        // Đăng ký callback: khi phiên hết giờ → broadcast AUCTION_ENDED đến toàn phòng
        AuctionManager.getInstance().setOnAuctionEndCallback(session -> {
            Map<String, Object> data = new HashMap<>();
            data.put("type",       "AUCTION_ENDED");
            data.put("itemId",     session.getItemId());
            data.put("winner",     session.getHighestBidder());
            data.put("price",      session.getCurrentPrice());
            data.put("bidHistory", session.getBidHistory());

            Map<String, Object> resp = new HashMap<>();
            resp.put("status", "success");
            resp.put("data", data);
            broadcastToRoom(session.getItemId(), gson.toJson(resp));
        });
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    public void onOpen(WebSocket ws, ClientHandshake handshake) {
        System.out.println("✅ Client kết nối: " + ws.getRemoteSocketAddress());
        // Gửi ngay danh sách sản phẩm + trạng thái phiên cho client mới
        sendInitialItems(ws);
    }

    @Override
    public void onClose(WebSocket ws, int code, String reason, boolean remote) {
        // Dọn dẹp client khỏi tất cả các phòng khi ngắt kết nối
        for (Set<WebSocket> room : auctionRooms.values()) {
            room.remove(ws);
        }
        System.out.println("⚠ Client ngắt kết nối: " + ws.getRemoteSocketAddress());
    }

    @Override
    public void onError(WebSocket ws, Exception e) {
        System.err.println("❌ WebSocket error: " + e.getMessage());
    }

    @Override
    public void onStart() {
        System.out.println("🚀 WebSocket Server khởi động tại cổng 8081");
    }

    // ── Gửi danh sách ban đầu cho client mới ────────────────────────────────

    /**
     * Gửi toàn bộ danh sách sản phẩm + trạng thái phiên hiện tại cho client vừa kết nối.
     * Client sẽ dùng dữ liệu này để render grid sản phẩm ngay lập tức.
     */
    private void sendInitialItems(WebSocket ws) {
        List<Map<String, Object>> itemsToSend = new ArrayList<>();

        for (Item item : itemService.getAllItem()) {
            Map<String, Object> itemData = new HashMap<>();
            itemData.put("id",          item.getId());
            itemData.put("name",        item.getName());
            itemData.put("description", item.getDescription());
            itemData.put("seller",      item.getSellerUserName());
            itemData.put("image",       item.getImage() != null ? item.getImage() : "");

            // Lấy trạng thái phiên từ RAM (nếu có) để gửi giá & người dẫn đầu thực tế
            AuctionSession session = AuctionManager.getInstance().getSession(item.getId());
            if (session != null) {
                itemData.put("startingPrice",  session.getCurrentPrice());
                itemData.put("endTime",        session.getEndTime().getTime());
                itemData.put("highestBidder",  session.getHighestBidder());
                itemData.put("isFinished",     session.isFinished());
            } else {
                itemData.put("startingPrice",  item.getStartingPrice());
                itemData.put("endTime",        item.getEndTime());
                itemData.put("highestBidder",  "Chưa có ai");
                itemData.put("isFinished",     false);
            }
            itemsToSend.add(itemData);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("type",  "INITIAL_ITEMS");
        data.put("items", itemsToSend);

        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "success");
        resp.put("data",   data);

        ws.send(gson.toJson(resp));
    }

    // ── Xử lý message từ client ──────────────────────────────────────────────

    @Override
    public void onMessage(WebSocket ws, String message) {
        try {
            WebSocketRequestDTO request = gson.fromJson(message, WebSocketRequestDTO.class);

            // Xác thực JWT token — mọi action đều phải có token hợp lệ
            DecodedJWT jwt = JwtUtil.verifyToken(request.getToken());
            if (jwt == null) {
                sendError(ws, "Token không hợp lệ hoặc đã hết hạn. Vui lòng đăng nhập lại!");
                return;
            }
            String username = jwt.getSubject();

            switch (request.getAction()) {
                case "POST_ITEM":
                    handlePostItem(ws, request, username);
                    break;
                case "JOIN":
                    handleJoinRoom(ws, Integer.parseInt(request.getItemId()), username);
                    break;
                case "BID":
                    handleBid(ws, Integer.parseInt(request.getItemId()), username, (long) request.getPrice());
                    break;
                case "GET_ITEMS":
                    // Client yêu cầu làm mới danh sách (ví dụ nhấn nút Refresh)
                    sendInitialItems(ws);
                    break;
                default:
                    sendError(ws, "Action không hợp lệ: " + request.getAction());
            }
        } catch (Exception e) {
            System.err.println("❌ Lỗi xử lý message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── Đăng sản phẩm mới ───────────────────────────────────────────────────

    /**
     * Xử lý POST_ITEM:
     * 1. Tính thời gian kết thúc
     * 2. Lưu item vào DB
     * 3. Tạo phiên đấu giá (AuctionManager lưu DB)
     * 4. Broadcast NEW_ITEM_ADDED đến tất cả client
     */
    private void handlePostItem(WebSocket ws, WebSocketRequestDTO request, String username) {
        // Mặc định 120 giây nếu client không gửi duration
        int duration = request.getDuration() > 0 ? request.getDuration() : 120;
        long endTime = System.currentTimeMillis() + (duration * 1000L);

        // Lưu item vào DB
        String result = itemService.addItem(
                request.getName(), request.getDescription(),
                request.getPrice(), username,
                request.getImage(), endTime);

        if (!"success".equals(result)) {
            sendError(ws, "Không thể đăng sản phẩm: " + result);
            return;
        }

        // Lấy item vừa lưu để lấy ID thật từ DB (tránh race condition)
        Item newItem = itemService.getLastInsertedItem();
        if (newItem == null) {
            sendError(ws, "Lỗi hệ thống: Không thể lấy thông tin sản phẩm vừa đăng!");
            return;
        }
        int itemId = newItem.getId();

        // Lấy sellerId từ username
        int sellerId = userRepository.getUserIdByUsername(username);
        if (sellerId == -1) {
            sendError(ws, "Không tìm thấy thông tin người bán trong hệ thống!");
            return;
        }

        // Tạo phiên đấu giá → AuctionManager tự lưu DB và lên lịch kết thúc
        AuctionManager.getInstance().createNewSession(itemId, sellerId, request.getPrice(), duration);

        // Tự động thêm seller vào phòng để nhận cập nhật giá realtime
        auctionRooms.putIfAbsent(itemId, ConcurrentHashMap.newKeySet());
        auctionRooms.get(itemId).add(ws);

        // Broadcast sản phẩm mới đến TẤT CẢ client
        Map<String, Object> data = new HashMap<>();
        data.put("type",           "NEW_ITEM_ADDED");
        data.put("itemId",         itemId);
        data.put("name",           request.getName());
        data.put("price",          request.getPrice());
        data.put("seller",         username);
        data.put("endTime",        endTime);
        data.put("image",          request.getImage() != null ? request.getImage() : "");
        data.put("highestBidder",  "Chưa có ai");

        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "success");
        resp.put("data",   data);
        broadcast(gson.toJson(resp));

        System.out.println("📦 Sản phẩm mới | item_id=" + itemId + " | người bán=" + username);
    }

    // ── Tham gia phòng đấu giá ───────────────────────────────────────────────

    /**
     * Xử lý JOIN:
     * - Thêm client vào phòng
     * - Gửi ngay SESSION_STATE (giá hiện tại + người dẫn đầu + toàn bộ lịch sử)
     */
    private void handleJoinRoom(WebSocket ws, int itemId, String username) {
        auctionRooms.putIfAbsent(itemId, ConcurrentHashMap.newKeySet());
        auctionRooms.get(itemId).add(ws);

        // Gửi trạng thái phiên hiện tại cho client vừa JOIN
        AuctionSession session = AuctionManager.getInstance().getSession(itemId);
        if (session != null) {
            Map<String, Object> data = new HashMap<>();
            data.put("type",           "SESSION_STATE");
            data.put("itemId",         itemId);
            data.put("currentPrice",   session.getCurrentPrice());
            data.put("highestBidder",  session.getHighestBidder());
            data.put("bidHistory",     session.getBidHistory()); // Toàn bộ lịch sử trong RAM
            data.put("isFinished",     session.isFinished());
            data.put("endTime",        session.getEndTime().getTime());

            Map<String, Object> resp = new HashMap<>();
            resp.put("status", "success");
            resp.put("data",   data);
            ws.send(gson.toJson(resp));
        }

        System.out.println("👤 " + username + " đã JOIN phòng item_id=" + itemId);
    }

    // ── Đặt giá ─────────────────────────────────────────────────────────────

    /**
     * Xử lý BID:
     * 1. Kiểm tra seller không tự bid sản phẩm của mình
     * 2. Gọi AuctionManager.placeBid() → validate + cập nhật RAM + cập nhật DB
     * 3. Broadcast UPDATE_PRICE (kèm lịch sử) đến toàn phòng
     * 4. Broadcast PRICE_UPDATE_GLOBAL đến tất cả client ngoài grid
     */
    private void handleBid(WebSocket ws, int itemId, String username, long price) {
        // Kiểm tra seller không được tự bid sản phẩm của mình
        Item item = itemService.findItemById(itemId);
        if (item != null && item.getSellerUserName().equals(username)) {
            sendError(ws, "Bạn không thể đấu giá sản phẩm do chính mình đăng bán!");
            return;
        }

        // Kiểm tra phiên tồn tại
        AuctionSession session = AuctionManager.getInstance().getSession(itemId);
        if (session == null) {
            sendError(ws, "Sản phẩm này chưa được tạo phiên đấu giá!");
            return;
        }

        // Kiểm tra phiên còn chạy
        if (session.isFinished()) {
            sendError(ws, "Phiên đấu giá đã kết thúc!");
            return;
        }
        if (!session.isAuctionRunning()) {
            sendError(ws, "Phiên đấu giá chưa bắt đầu hoặc đã hết giờ!");
            return;
        }

        // Thực hiện đặt giá: AuctionManager xử lý RAM + DB trong một lần gọi
        boolean success = AuctionManager.getInstance().placeBid(itemId, username, price);

        if (success) {
            // Refresh session sau khi bid thành công
            session = AuctionManager.getInstance().getSession(itemId);

            // Broadcast UPDATE_PRICE + lịch sử đến TẤT CẢ client trong phòng
            Map<String, Object> roomData = new HashMap<>();
            roomData.put("type",       "UPDATE_PRICE");
            roomData.put("itemId",     itemId);
            roomData.put("user",       username);
            roomData.put("price",      (double) price);
            roomData.put("bidHistory", session.getBidHistory()); // Lịch sử đầy đủ

            Map<String, Object> roomResp = new HashMap<>();
            roomResp.put("status", "success");
            roomResp.put("data",   roomData);
            broadcastToRoom(itemId, gson.toJson(roomResp));

            // Broadcast PRICE_UPDATE_GLOBAL đến TẤT CẢ client (cập nhật card ngoài grid)
            broadcastPriceUpdateGlobal(itemId, username, price);

            System.out.println("💰 Đặt giá | item_id=" + itemId
                    + " | " + username + " → " + String.format("%,.0f", (double) price) + " VNĐ");
        } else {
            sendError(ws, String.format(
                    "Giá đặt phải cao hơn giá hiện tại (%,.0f VNĐ)!",
                    session.getCurrentPrice()));
        }
    }

    // ── Broadcast helpers ────────────────────────────────────────────────────

    /** Gửi message đến tất cả client đang trong phòng của itemId */
    private void broadcastToRoom(int itemId, String message) {
        Set<WebSocket> room = auctionRooms.get(itemId);
        if (room == null) return;
        for (WebSocket client : room) {
            if (client.isOpen()) {
                client.send(message);
            }
        }
    }

    /** Broadcast giá mới đến TẤT CẢ client (kể cả ngoài phòng) để cập nhật card trên grid */
    private void broadcastPriceUpdateGlobal(int itemId, String username, long price) {
        Map<String, Object> data = new HashMap<>();
        data.put("type",   "PRICE_UPDATE_GLOBAL");
        data.put("itemId", itemId);
        data.put("user",   username);
        data.put("price",  (double) price);

        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "success");
        resp.put("data",   data);
        broadcast(gson.toJson(resp));
    }

    /** Gửi thông báo lỗi về client */
    private void sendError(WebSocket ws, String message) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("status",  "error");
        resp.put("message", message);
        ws.send(gson.toJson(resp));
    }
}
