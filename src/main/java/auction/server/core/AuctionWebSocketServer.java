package auction.server.core;

import auction.server.model.AuctionSession;
import auction.server.model.Item;
import auction.server.service.ItemService;
import auction.shared.dto.WebSocketRequestDTO;
import auction.shared.util.JwtUtil;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.Gson;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionWebSocketServer extends WebSocketServer {
    private final Gson gson = new Gson();
    private final ConcurrentHashMap<String, Set<WebSocket>> auctionRooms = new ConcurrentHashMap<>();
    private final ItemService itemService = new ItemService();

    public AuctionWebSocketServer(int port) {
        super(new InetSocketAddress(port));

        // ĐÃ THÊM: Lắng nghe sự kiện kết thúc phiên từ AuctionManager
        AuctionManager.getInstance().setOnAuctionEndCallback(session -> {
            Map<String, Object> data = new HashMap<>();
            data.put("type", "AUCTION_ENDED");
            data.put("itemId", session.getIdItem());
            data.put("winner", session.getHighestBidder());
            data.put("price", session.getCurrentPrice());

            Map<String, Object> resp = new HashMap<>();
            resp.put("status", "success");
            resp.put("data", data);

            // Chỉ gửi cho những người trong Room (đã ấn Tham gia)
            broadcastToRoom(session.getIdItem(), gson.toJson(resp));
        });
    }

    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        System.out.println("✅ Kết nối mới: " + webSocket.getRemoteSocketAddress());

        List<Map<String, Object>> itemsToSend = new ArrayList<>();
        for (Item item : itemService.getAllItem()) {
            Map<String, Object> mapData = new HashMap<>();
            mapData.put("id", item.getId());
            mapData.put("name", item.getName());
            mapData.put("description", item.getDescription());
            mapData.put("seller", item.getSellerUserName());

            AuctionSession session = AuctionManager.getInstance().getSession(item.getId());
            if (session != null) {
                mapData.put("startingPrice", session.getCurrentPrice());
                mapData.put("endTime", session.getEndTime());
            } else {
                mapData.put("startingPrice", item.getStartingPrice());
                mapData.put("endTime", 0L);
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
            if(jwt == null){
                sendError(webSocket, "Token không hợp lệ");
                return;
            }
            String username = jwt.getSubject();

            switch (request.getAction()){
                case "POST_ITEM":
                    String result = itemService.addItem(request.getName(), request.getDescription(), request.getPrice(), username);
                    if ("success".equals(result)) {
                        List<Item> allItems = itemService.getAllItem();
                        Item newItem = allItems.get(allItems.size() - 1);
                        String realId = newItem.getId();

                        AuctionManager.getInstance().createNewSession(realId, request.getName(), request.getPrice(), 120);

                        Map<String, Object> data = new HashMap<>();
                        data.put("type", "NEW_ITEM_ADDED");
                        data.put("itemId", realId);
                        data.put("name", request.getName());
                        data.put("price", request.getPrice());
                        data.put("seller", username);

                        Map<String, Object> resp = new HashMap<>();
                        resp.put("status", "success");
                        resp.put("data", data);
                        broadcast(gson.toJson(resp));
                    }
                    break;
                case "JOIN":
                    joinRoom(webSocket, request.getItemId(), username);
                    break;
                case "BID":
                    handleBid(webSocket, request.getItemId(), username, request.getPrice());
                    break;
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void joinRoom(WebSocket conn, String itemId, String username){
        auctionRooms.putIfAbsent(itemId, ConcurrentHashMap.newKeySet());
        auctionRooms.get(itemId).add(conn);
    }

    private void handleBid(WebSocket conn, String itemId, String username, long price){
        Item targetItem = null;
        for (Item i : itemService.getAllItem()) {
            if (i.getId().equals(itemId)) {
                targetItem = i;
                break;
            }
        }

        if (targetItem != null && targetItem.getSellerUserName().equals(username)) {
            sendError(conn, "Bạn không thể tham gia đấu giá ở tài khoản Seller");
            return;
        }

        AuctionSession session = AuctionManager.getInstance().getSession(itemId);

        if (session == null) {
            sendError(conn, "Sản phẩm này chưa được tạo phiên đấu giá!");
            return;
        }

        if (session.placeBid(username, price)) {
            Map<String, Object> data = new HashMap<>();
            data.put("type", "UPDATE_PRICE");
            data.put("itemId", itemId);
            data.put("user", username);
            data.put("price", price);

            Map<String, Object> resp = new HashMap<>();
            resp.put("status", "success");
            resp.put("data", data);
            broadcastToRoom(itemId, gson.toJson(resp));
        } else {
            sendError(conn, "Đặt giá thất bại! Vui lòng đặt mức giá cao hơn.");
        }
    }

    private void broadcastToRoom(String itemId, String msg){
        Set<WebSocket> room = auctionRooms.get(itemId);
        if(room != null) {
            for (WebSocket c : room) if(c.isOpen()) c.send(msg);
        }
    }

    private void sendError(WebSocket ws, String msg) {
        Map<String, Object> r = new HashMap<>();
        r.put("status", "error");
        r.put("message", msg);
        ws.send(gson.toJson(r));
    }

    @Override public void onClose(WebSocket ws, int i, String s, boolean b) {}
    @Override public void onError(WebSocket ws, Exception e) {}
    @Override public void onStart() { System.out.println("Server started on 8081"); }
}