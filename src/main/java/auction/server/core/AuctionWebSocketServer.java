package auction.server.core;

import auction.server.model.AuctionSession;
import auction.shared.dto.WebSocketRequestDTO;
import auction.shared.util.JsonUtil;
import auction.shared.util.JwtUtil;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.Gson;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionWebSocketServer extends WebSocketServer {
    Gson gson = new Gson();
    private final ConcurrentHashMap<String, Set<WebSocket>> auctionRooms = new ConcurrentHashMap<>();
    // Key là Mã sản phẩm, Value là Danh sách các Client đang xem
    public AuctionWebSocketServer(int port) {
        super(new InetSocketAddress(port));
    }
    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        System.out.println("connected " + webSocket.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket webSocket, int i, String s, boolean b) {
        for(Set<WebSocket> room : auctionRooms.values()){
            room.remove(webSocket);
        }
        System.out.println("disconnected " + webSocket.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(WebSocket webSocket, String message) {
        try {
            WebSocketRequestDTO request = gson.fromJson(message, WebSocketRequestDTO.class);
            DecodedJWT jwt = JwtUtil.verifyToken(request.getToken());
            if(jwt == null){
                sendError(webSocket, "token không hợp lệ");
                return;
            }
            String username = jwt.getSubject();
            String role = jwt.getClaim("role").asString();
            String itemId = request.getItemId();
            switch (request.getAction()){
                case "JOIN":
                    joinRoom(webSocket, itemId, username);
                    break;
                case "BID":
                    if ("SELLER".equals(role)) {
                        sendError(webSocket, "Người bán không được phép tự đấu giá");
                        return;
                    }
                    handleBid(webSocket, itemId, username, request.getPrice());
                    break;
                default:
                    sendError(webSocket, "Hành động không được hỗ trợ");
                    break;
            }
        }catch (Exception e){
            sendError(webSocket, "Dữ liệu gửi lên không đúng định dạng JSON");
        }
    }

    @Override
    public void onError(WebSocket webSocket, Exception e) {
        System.err.println("Lỗi hệ thống Socket: " + e.getMessage());
    }

    @Override
    public void onStart() {
        System.out.println("WebSocket Server is starting: " + getPort());
    }
    // hàm thêm client vào room sản phẩm
    private void joinRoom(WebSocket conn, String itemId, String username){
        auctionRooms.putIfAbsent(itemId, ConcurrentHashMap.newKeySet());
        auctionRooms.get(itemId).add(conn);
        System.out.println(username + " đã tham gia phòng " + itemId);
    }
    // hàm xử lí client đặt giá
    private void handleBid(WebSocket conn, String itemId, String username, long price){
        AuctionSession session = AuctionManager.getInstance().getSession(itemId);
        if (session == null) {
            sendError(conn, "Sản phẩm không tồn tại hoặc đã kết thúc!");
            return;
        }
        boolean isSuccess = session.placeBid(username, price);
        if (isSuccess) {
            Map<String, Object> data = new HashMap<>();
            data.put("type", "UPDATE_PRICE");
            data.put("itemId", itemId);
            data.put("user", username);
            data.put("price", price);

            //Dùng JsonUtil để đóng gói thành json
            String jsonResponse = JsonUtil.buildResponse("success", "Đặt giá thành công!", data);
            broadcastToRoom(itemId, jsonResponse);
            System.out.println(username + " nâng giá " + itemId + " lên " + price);
        }
        else {
            sendError(conn, "Đặt giá thất bại! Giá phải cao hơn hiện tại hoặc phiên đã đóng.");
        }

    }
    // hàm gửi tin nhắn đến toàn bộ client trong 1 room sản phẩm
    private void broadcastToRoom(String itemId, String messageJson){
        Set<WebSocket> room = auctionRooms.get(itemId); // lay ve 1 phong dua vao itemId
        if(room != null){
            for (WebSocket client : room){
                if(client.isOpen()){
                    client.send(messageJson);
                }
            }
        }
    }
    private void sendError(WebSocket webSocket, String message) {
        // Không cần tạo Map thủ công nữa, dùng luôn JsonUtil
        String json = JsonUtil.buildResponse("error", message, null);
        webSocket.send(json);
    }
}