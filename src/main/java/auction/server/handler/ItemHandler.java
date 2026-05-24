package auction.server.handler;

import auction.server.core.AuctionManager;
import auction.server.model.Item;
import auction.server.repository.ItemRepository;
import auction.server.util.HttpServerUtil;
import auction.shared.dto.ItemDTO;
import auction.shared.dto.ResponseDTO;
import auction.shared.util.HttpResponseUtil;
import auction.shared.util.JwtUtil;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

public class ItemHandler implements HttpHandler {
    private final Gson gson = new Gson();
    private final ItemRepository itemRepository = new ItemRepository();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod().toUpperCase();

            // === GET: Lấy danh sách sản phẩm ===
            if ("GET".equals(method)) {
                var listItem = itemRepository.getAllItemsFromDatabase();
                ResponseDTO response = new ResponseDTO("success", "Lấy danh sách sản phẩm thành công", listItem);
                HttpResponseUtil.sendHttpResponse(exchange, 200, response);
                return;
            }

            // === POST: Thêm sản phẩm ===
            if ("POST".equals(method)) {
                String jsonBody = HttpServerUtil.readRequestBody(exchange);
                if (jsonBody.isEmpty()) {
                    HttpResponseUtil.sendHttpResponse(exchange, 400, new ResponseDTO("fail", "Dữ liệu trống"));
                    return;
                }

                ItemDTO itemDTO = gson.fromJson(jsonBody, ItemDTO.class);

                // Khởi tạo tên người bán
                String sellerUsername = null;
                String authHeader = exchange.getRequestHeaders().getFirst("Authorization");

                // Đọc thông tin từ Token JWT nếu có
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    try {
                        String token = authHeader.substring(7);
                        DecodedJWT jwt = JwtUtil.verifyToken(token);
                        if (jwt != null) {
                            sellerUsername = jwt.getSubject();
                        }
                    } catch (Exception e) {
                        System.out.println("[WARNING] Lỗi giải mã Token, chuyển sang cơ chế fallback.");
                    }
                }

                // ĐÃ SỬA: Thay thế hoàn toàn getSeller() thành getSellerUsername() chuẩn khớp với ItemDTO của bạn
                if (sellerUsername == null || sellerUsername.isEmpty()) {
                    sellerUsername = itemDTO.getSellerUsername();
                }

                // Nếu cả 2 cách trên vẫn không lấy được (do test giả lập), gán giá trị mặc định để tránh lỗi NULL trong DB
                if (sellerUsername == null || sellerUsername.isEmpty()) {
                    sellerUsername = "UnknownSeller";
                }

                // Tạo đối tượng model và thực hiện lưu vào database thông qua Repository
                Item itemToSave = new Item(itemDTO.getName(), itemDTO.getDescription(), itemDTO.getStartingPrice(), sellerUsername);
                itemRepository.saveItem(itemToSave);

                // Nếu item_id tự tăng sinh ra lớn hơn 0 tức là đã insert vào Database thành công
                if (itemToSave.getId() > 0) {
                    // Kích hoạt phiên đấu giá mới với thời gian mặc định 60 giây
                    AuctionManager.getInstance().createNewSession(
                            itemToSave.getId(),
                            itemToSave.getId(), // Tạm thời dùng ID của Item thay cho Seller ID nếu chưa liên kết bảng Users
                            itemToSave.getStartingPrice(),
                            60
                    );

                    System.out.println("🚀 [SERVER] Đăng bán sản phẩm và kích hoạt phiên đấu giá thành công cho mã sản phẩm: " + itemToSave.getId());
                    HttpResponseUtil.sendHttpResponse(exchange, 201,
                            new ResponseDTO("success", "Thêm sản phẩm thành công!", itemToSave.getId()));
                } else {
                    HttpResponseUtil.sendHttpResponse(exchange, 500, new ResponseDTO("fail", "Không thể ghi sản phẩm vào Cơ sở dữ liệu"));
                }
                return;
            }

            HttpResponseUtil.sendHttpResponse(exchange, 405, new ResponseDTO("fail", "Phương thức không hỗ trợ"));

        } catch (Exception e) {
            e.printStackTrace();
            HttpResponseUtil.sendHttpResponse(exchange, 500, new ResponseDTO("error", "Lỗi xử lý Server: " + e.getMessage()));
        }
    }
}