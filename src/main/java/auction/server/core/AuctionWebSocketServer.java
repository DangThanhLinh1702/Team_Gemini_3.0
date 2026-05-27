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

public class AuctionWebSocketServer extends WebSocketServer {
    private final Gson gson = new Gson();
    // itemId → set of connected clients trong phòng đó
    private final ConcurrentHashMap<Integer, Set<WebSocket>> auctionRooms = new ConcurrentHashMap<>();
    private final ItemService itemService = new ItemService();
    private final UserRepository userRepository = new UserRepository();

    public AuctionWebSocketServer(int port) {
        super(new InetSocketAddress(port));
        // Callback khi phiên tự động kết thúc (hết giờ)
        AuctionManager.getInstance().setOnAuctionEndCallback(session -> {
            Map<String, Object> data = new HashMap<>();
            data.put("type", "AUCTION_ENDED");
            data.put("itemId", session.getItemId());
            data.put("winner", session.getHighestBidder());
            data.put("price", session.getCurrentPrice());
            data.put("bidHistory", session.getBidHistory());
            Map<String, Object> resp = new HashMap<>();
            resp.put("status", "success");
            resp.put("data", data);
            broadcastToRoom(session.getItemId(), gson.toJson(resp));
        });
    }

    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        System.out.println("✅ Kết nối mới: " + webSocket.getRemoteSocketAddress());
        sendInitialItems(webSocket);
    }

    /**
     * Gửi toàn bộ danh sách sản phẩm + trạng thái phiên hiện tại cho client mới kết nối.
     */
    private void sendInitialItems(WebSocket webSocket) {
        List<Map<String, Object>> itemsToSend = new ArrayList<>();
        for (Item item : itemService.getAllItem()) {
            Map<String, Object> mapData = new HashMap<>();
            mapData.put("id", item.getId());
            mapData.put("name", item.getName());
            mapData.put("description", item.getDescription());
            mapData.put("seller", item.getSellerUserName());
            mapData.put("image", item.getImage());

            AuctionSession session = AuctionManager.getInstance().getSession(item.getId());
            if (session != null) {
                mapData.put("startingPrice", session.getCurrentPrice());
                mapData.put("endTime", session.getEndTime().getTime());
                mapData.put("highestBidder", session.getHighestBidder());
                mapData.put("isFinished", session.isFinished());
            } else {
                mapData.put("startingPrice", item.getStartingPrice());
                mapData.put("endTime", item.getEndTime());
                mapData.put("highestBidder", "Chưa có ai");
                mapData.put("isFinished", false);
            }
            itemsToSend.add(mapData);
        }
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        Map<String, Object> data = new HashMap<>();
        data.put("type", "INITIAL_ITEMS");
        data.put("items", itemsToSend);
        response.put("data", data);
        webSocket.send(gson.toJson(response));
    }

    @Override
    public void onMessage(WebSocket webSocket, String message) {
        try {
            WebSocketRequestDTO request = gson.fromJson(message, WebSocketRequestDTO.class);
            DecodedJWT jwt = JwtUtil.verifyToken(request.getToken());
            if (jwt == null) {
                sendError(webSocket, "Token không hợp lệ");
                return;
            }
            String username = jwt.getSubject();

            switch (request.getAction()) {
                case "POST_ITEM":
                    handlePostItem(webSocket, request, username);
                    break;
                case "JOIN":
                    joinRoom(webSocket, Integer.parseInt(request.getItemId()), username);
                    break;
                case "BID":
                    handleBid(webSocket, Integer.parseInt(request.getItemId()), username, (long) request.getPrice());
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handlePostItem(WebSocket webSocket, WebSocketRequestDTO request, String username) {
        int duration = request.getDuration() > 0 ? request.getDuration() : 120;
        long calculatedEndTime = System.currentTimeMillis() + (duration * 1000L);

        String result = itemService.addItem(
                request.getName(), request.getDescription(),
                request.getPrice(), username,
                request.getImage(), calculatedEndTime);

        if ("success".equals(result)) {
            List<Item> allItems = itemService.getAllItem();
            Item newItem = allItems.get(allItems.size() - 1);
            int realId = newItem.getId();
            int sellerId = userRepository.getUserIdByUsername(username);
            if (sellerId == -1) {
                sendError(webSocket, "Không tìm thấy thông tin người bán trong hệ thống!");
                return;
            }

            AuctionManager.getInstance().createNewSession(realId, sellerId, request.getPrice(), duration);

            // Tự động JOIN seller vào phòng để nhận cập nhật giá realtime
            auctionRooms.putIfAbsent(realId, ConcurrentHashMap.newKeySet());
            auctionRooms.get(realId).add(webSocket);

            Map<String, Object> data = new HashMap<>();
            data.put("type", "NEW_ITEM_ADDED");
            data.put("itemId", realId);
            data.put("name", request.getName());
            data.put("price", request.getPrice());
            data.put("seller", username);
            data.put("endTime", calculatedEndTime);
            data.put("image", request.getImage());
            data.put("highestBidder", "Chưa có ai");

            Map<String, Object> resp = new HashMap<>();
            resp.put("status", "success");
            resp.put("data", data);
            broadcast(gson.toJson(resp));
        }
    }

    /**
     * Khi client JOIN phòng: thêm vào room và gửi ngay trạng thái hiện tại của phiên
     * (giá hiện tại, người dẫn đầu, toàn bộ lịch sử đấu giá).
     */
    private void joinRoom(WebSocket conn, int itemId, String username) {
        auctionRooms.putIfAbsent(itemId, ConcurrentHashMap.newKeySet());
        auctionRooms.get(itemId).add(conn);

        // Gửi lại trạng thái phiên hiện tại cho client vừa JOIN
        AuctionSession session = AuctionManager.getInstance().getSession(itemId);
        if (session != null) {
            Map<String, Object> data = new HashMap<>();
            data.put("type", "SESSION_STATE");
            data.put("itemId", itemId);
            data.put("currentPrice", session.getCurrentPrice());
            data.put("highestBidder", session.getHighestBidder());
            data.put("bidHistory", session.getBidHistory());
            data.put("isFinished", session.isFinished());
            data.put("endTime", session.getEndTime().getTime());

            Map<String, Object> resp = new HashMap<>();
            resp.put("status", "success");
            resp.put("data", data);
            conn.send(gson.toJson(resp));
        }
        System.out.println("👤 " + username + " đã JOIN phòng itemId=" + itemId);
    }

    /**
     * Xử lý BID: validate → placeBid → broadcast UPDATE_PRICE kèm bidHistory mới nhất.
     */
    private void handleBid(WebSocket conn, int itemId, String username, long price) {
        // Không cho phép seller tự bid sản phẩm của mình
        Item targetItem = itemService.findItemById(itemId);
        if (targetItem != null && targetItem.getSellerUserName().equals(username)) {
            sendError(conn, "Bạn không thể đấu giá sản phẩm do chính mình đăng bán!");
            return;
        }

        AuctionSession session = AuctionManager.getInstance().getSession(itemId);
        if (session == null) {
            sendError(conn, "Sản phẩm này chưa được tạo phiên đấu giá!");
            return;
        }
        if (session.isFinished()) {
            sendError(conn, "Phiên đấu giá đã kết thúc!");
            return;
        }
        if (!session.isAuctionRunning()) {
            sendError(conn, "Phiên đấu giá chưa bắt đầu hoặc đã kết thúc!");
            return;
        }

        if (session.placeBid(username, price)) {
            // Broadcast UPDATE_PRICE kèm lịch sử mới nhất cho toàn phòng
            Map<String, Object> data = new HashMap<>();
            data.put("type", "UPDATE_PRICE");
            data.put("itemId", itemId);
            data.put("user", username);
            data.put("price", (double) price);
            data.put("bidHistory", session.getBidHistory()); // ← lịch sử đầy đủ

            Map<String, Object> resp = new HashMap<>();
            resp.put("status", "success");
            resp.put("data", data);
            String json = gson.toJson(resp);

            // Broadcast tới phòng ĐỒNG THỜI broadcast nhẹ giá mới tới tất cả (cập nhật card)
            broadcastToRoom(itemId, json);
            broadcastPriceUpdate(itemId, username, price); // cập nhật card ở màn hình grid
        } else {
            sendError(conn, "Giá đặt phải cao hơn giá hiện tại (" +
                    String.format("%,.0f", session.getCurrentPrice()) + " VNĐ)!");
        }
    }

    /**
     * Broadcast giá mới tới TẤT CẢ client (kể cả không trong phòng) để cập nhật card grid.
     */
    private void broadcastPriceUpdate(int itemId, String username, long price) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "PRICE_UPDATE_GLOBAL");
        data.put("itemId", itemId);
        data.put("user", username);
        data.put("price", (double) price);
        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "success");
        resp.put("data", data);
        broadcast(gson.toJson(resp));
    }

    private void broadcastToRoom(int itemId, String msg) {
        Set<WebSocket> room = auctionRooms.get(itemId);
        if (room != null) {
            for (WebSocket c : room) {
                if (c.isOpen()) c.send(msg);
            }
        }
    }

    private void sendError(WebSocket ws, String msg) {
        Map<String, Object> r = new HashMap<>();
        r.put("status", "error");
        r.put("message", msg);
        ws.send(gson.toJson(r));
    }

    @Override public void onClose(WebSocket ws, int i, String s, boolean b) {
        // Dọn dẹp khỏi tất cả các phòng
        for (Set<WebSocket> room : auctionRooms.values()) {
            room.remove(ws);
        }
    }
    @Override public void onError(WebSocket ws, Exception e) { e.printStackTrace(); }
    @Override public void onStart() { System.out.println("🚀 WebSocket Server started on port 8081"); }
}