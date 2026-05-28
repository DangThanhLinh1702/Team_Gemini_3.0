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
 * WebSocket Server xử lý toàn bộ giao tiếp realtime.
 *
 * Fix:
 * - AUCTION_ENDED broadcast kèm PRICE_UPDATE_GLOBAL để card ngoài grid cũng cập nhật
 * - SESSION_STATE trả về endTime để client tính countdown đúng
 * - sendInitialItems lấy đúng giá/leader từ DB (qua AuctionRepository) khi session không có trong RAM
 */
public class AuctionWebSocketServer extends WebSocketServer {

    private final Gson gson = new Gson();

    // Map: itemId → tập hợp client đang ở trong phòng đấu giá đó
    private final ConcurrentHashMap<Integer, Set<WebSocket>> auctionRooms = new ConcurrentHashMap<>();

    private final ItemService itemService = new ItemService();
    private final UserRepository userRepository = new UserRepository();
    private final auction.server.repository.AuctionRepository auctionRepository =
            new auction.server.repository.AuctionRepository();

    public AuctionWebSocketServer(int port) {
        super(new InetSocketAddress(port));

        // Khi phiên hết giờ → broadcast AUCTION_ENDED + PRICE_UPDATE_GLOBAL (cập nhật card)
        AuctionManager.getInstance().setOnAuctionEndCallback(session -> {
            int itemId = session.getItemId();

            // Broadcast AUCTION_ENDED đến client đang trong phòng
            Map<String, Object> endData = new HashMap<>();
            endData.put("type",       "AUCTION_ENDED");
            endData.put("itemId",     itemId);
            endData.put("winner",     session.getHighestBidder());
            endData.put("price",      session.getCurrentPrice());
            endData.put("bidHistory", session.getBidHistory());

            Map<String, Object> endResp = new HashMap<>();
            endResp.put("status", "success");
            endResp.put("data", endData);
            broadcastToRoom(itemId, gson.toJson(endResp));

            // Broadcast PRICE_UPDATE_GLOBAL để card ngoài grid cũng cập nhật người thắng + giá cuối
            broadcastPriceUpdateGlobal(itemId, session.getHighestBidder(),
                    (long) session.getCurrentPrice(), true);
        });
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    public void onOpen(WebSocket ws, ClientHandshake handshake) {
        System.out.println("✅ Client kết nối: " + ws.getRemoteSocketAddress());
        sendInitialItems(ws);
    }

    @Override
    public void onClose(WebSocket ws, int code, String reason, boolean remote) {
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

    private void sendInitialItems(WebSocket ws) {
        List<Map<String, Object>> itemsToSend = new ArrayList<>();

        for (Item item : itemService.getAllItem()) {
            Map<String, Object> itemData = new HashMap<>();
            itemData.put("id",          item.getId());
            itemData.put("name",        item.getName());
            itemData.put("description", item.getDescription());
            itemData.put("seller",      item.getSellerUserName());
            itemData.put("image",       item.getImage() != null ? item.getImage() : "");

            // Ưu tiên lấy từ RAM (session đang chạy)
            AuctionSession session = AuctionManager.getInstance().getSession(item.getId());
            if (session != null) {
                itemData.put("startingPrice",  session.getCurrentPrice());
                itemData.put("endTime",        session.getEndTime().getTime());
                itemData.put("highestBidder",  session.getHighestBidder());
                itemData.put("isFinished",     session.isFinished());
            } else {
                // Không có trong RAM → lấy từ DB (phiên đã kết thúc trước đó)
                AuctionSession dbSession = auctionRepository.findLatestByItemId(item.getId());
                if (dbSession != null) {
                    itemData.put("startingPrice",  dbSession.getCurrentPrice());
                    itemData.put("endTime",        dbSession.getEndTime().getTime());
                    itemData.put("highestBidder",  dbSession.getHighestBidder() != null
                            ? dbSession.getHighestBidder() : "Chưa có ai");
                    itemData.put("isFinished",     dbSession.isFinished());
                } else {
                    itemData.put("startingPrice",  item.getStartingPrice());
                    itemData.put("endTime",        item.getEndTime());
                    itemData.put("highestBidder",  "Chưa có ai");
                    itemData.put("isFinished",     false);
                }
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

    private void handlePostItem(WebSocket ws, WebSocketRequestDTO request, String username) {
        int duration = request.getDuration() > 0 ? request.getDuration() : 120;
        long endTime = System.currentTimeMillis() + (duration * 1000L);

        String result = itemService.addItem(
                request.getName(), request.getDescription(),
                request.getPrice(), username,
                request.getImage(), endTime);

        if (!"success".equals(result)) {
            sendError(ws, "Không thể đăng sản phẩm: " + result);
            return;
        }

        Item newItem = itemService.getLastInsertedItem();
        if (newItem == null) {
            sendError(ws, "Lỗi hệ thống: Không thể lấy thông tin sản phẩm vừa đăng!");
            return;
        }
        int itemId = newItem.getId();

        int sellerId = userRepository.getUserIdByUsername(username);
        if (sellerId == -1) {
            sendError(ws, "Không tìm thấy thông tin người bán trong hệ thống!");
            return;
        }

        AuctionManager.getInstance().createNewSession(itemId, sellerId, request.getPrice(), duration);

        auctionRooms.putIfAbsent(itemId, ConcurrentHashMap.newKeySet());
        auctionRooms.get(itemId).add(ws);

        Map<String, Object> data = new HashMap<>();
        data.put("type",          "NEW_ITEM_ADDED");
        data.put("itemId",        itemId);
        data.put("name",          request.getName());
        data.put("price",         request.getPrice());
        data.put("seller",        username);
        data.put("endTime",       endTime);
        data.put("image",         request.getImage() != null ? request.getImage() : "");
        data.put("highestBidder", "Chưa có ai");

        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "success");
        resp.put("data",   data);
        broadcast(gson.toJson(resp));

        System.out.println("📦 Sản phẩm mới | item_id=" + itemId + " | người bán=" + username);
    }

    // ── Tham gia phòng đấu giá ───────────────────────────────────────────────

    private void handleJoinRoom(WebSocket ws, int itemId, String username) {
        auctionRooms.putIfAbsent(itemId, ConcurrentHashMap.newKeySet());
        auctionRooms.get(itemId).add(ws);

        List<String> historyFromDb = AuctionManager.getInstance().getBidHistoryFromDb(itemId);

        AuctionSession session = AuctionManager.getInstance().getSession(itemId);
        if (session != null) {
            Map<String, Object> data = new HashMap<>();
            data.put("type",          "SESSION_STATE");
            data.put("itemId",        itemId);
            data.put("currentPrice",  session.getCurrentPrice());
            data.put("highestBidder", session.getHighestBidder());
            data.put("bidHistory",    historyFromDb.isEmpty() ? session.getBidHistory() : historyFromDb);
            data.put("isFinished",    session.isFinished());
            data.put("endTime",       session.getEndTime().getTime());

            Map<String, Object> resp = new HashMap<>();
            resp.put("status", "success");
            resp.put("data",   data);
            ws.send(gson.toJson(resp));
        } else {
            // Phiên không có trong RAM → lấy từ DB
            AuctionSession dbSession = auctionRepository.findLatestByItemId(itemId);
            if (dbSession != null) {
                Map<String, Object> data = new HashMap<>();
                data.put("type",          "SESSION_STATE");
                data.put("itemId",        itemId);
                data.put("currentPrice",  dbSession.getCurrentPrice());
                data.put("highestBidder", dbSession.getHighestBidder() != null
                        ? dbSession.getHighestBidder() : "Chưa có ai");
                data.put("bidHistory",    historyFromDb);
                data.put("isFinished",    dbSession.isFinished());
                data.put("endTime",       dbSession.getEndTime().getTime());

                Map<String, Object> resp = new HashMap<>();
                resp.put("status", "success");
                resp.put("data",   data);
                ws.send(gson.toJson(resp));
            } else {
                // Không có phiên nào → báo lỗi cho client
                sendError(ws, "Sản phẩm này chưa được tạo phiên đấu giá!");
            }
        }

        System.out.println("👤 " + username + " đã JOIN phòng item_id=" + itemId);
    }

    // ── Đặt giá ─────────────────────────────────────────────────────────────

    private void handleBid(WebSocket ws, int itemId, String username, long price) {
        Item item = itemService.findItemById(itemId);
        if (item != null && item.getSellerUserName().equals(username)) {
            sendError(ws, "Bạn không thể đấu giá sản phẩm do chính mình đăng bán!");
            return;
        }

        AuctionSession session = AuctionManager.getInstance().getSession(itemId);
        if (session == null) {
            sendError(ws, "Sản phẩm này chưa được tạo phiên đấu giá!");
            return;
        }

        if (session.isFinished()) {
            sendError(ws, "Phiên đấu giá đã kết thúc!");
            return;
        }
        if (!session.isAuctionRunning()) {
            sendError(ws, "Phiên đấu giá chưa bắt đầu hoặc đã hết giờ!");
            return;
        }

        boolean success = AuctionManager.getInstance().placeBid(itemId, username, price);

        if (success) {
            session = AuctionManager.getInstance().getSession(itemId);

            // Broadcast UPDATE_PRICE (kèm lịch sử) cho client đang trong phòng
            Map<String, Object> roomData = new HashMap<>();
            roomData.put("type",       "UPDATE_PRICE");
            roomData.put("itemId",     itemId);
            roomData.put("user",       username);
            roomData.put("price",      (double) price);
            roomData.put("bidHistory", session.getBidHistory());

            Map<String, Object> roomResp = new HashMap<>();
            roomResp.put("status", "success");
            roomResp.put("data",   roomData);
            broadcastToRoom(itemId, gson.toJson(roomResp));

            // Broadcast PRICE_UPDATE_GLOBAL cho TẤT CẢ client để cập nhật card ngoài grid
            broadcastPriceUpdateGlobal(itemId, username, price, false);

            System.out.println("💰 Đặt giá | item_id=" + itemId
                    + " | " + username + " → " + String.format("%,.0f", (double) price) + " VNĐ");
        } else {
            sendError(ws, String.format(
                    "Giá đặt phải cao hơn giá hiện tại (%,.0f VNĐ)!",
                    session.getCurrentPrice()));
        }
    }

    // ── Broadcast helpers ────────────────────────────────────────────────────

    private void broadcastToRoom(int itemId, String message) {
        Set<WebSocket> room = auctionRooms.get(itemId);
        if (room == null) return;
        for (WebSocket client : room) {
            if (client.isOpen()) client.send(message);
        }
    }

    /**
     * Broadcast giá/leader mới đến TẤT CẢ client để cập nhật card trên grid.
     * @param isFinished true nếu phiên đã kết thúc (đổi label sang "Người thắng")
     */
    private void broadcastPriceUpdateGlobal(int itemId, String username, long price, boolean isFinished) {
        Map<String, Object> data = new HashMap<>();
        data.put("type",       "PRICE_UPDATE_GLOBAL");
        data.put("itemId",     itemId);
        data.put("user",       username);
        data.put("price",      (double) price);
        data.put("isFinished", isFinished);

        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "success");
        resp.put("data",   data);
        broadcast(gson.toJson(resp));
    }

    private void sendError(WebSocket ws, String message) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("status",  "error");
        resp.put("message", message);
        ws.send(gson.toJson(resp));
    }
}