package auction.server.handler;

import auction.server.core.AuctionManager;
import auction.server.model.Item;
import auction.server.repository.ItemRepository;
import auction.server.service.ItemService;
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
import java.sql.Timestamp;

public class ItemHandler implements HttpHandler {
    private final ItemService itemService = new ItemService();
    private final Gson gson = new Gson();
    private final ItemRepository itemRepository = new ItemRepository();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod().toUpperCase();

            // === GET: Lấy danh sách sản phẩm ===
            if ("GET".equals(method)) {
                var listItem = itemService.getAllItem();
                ResponseDTO response = new ResponseDTO("success", "Lấy danh sách sản phẩm thành công", listItem);
                HttpResponseUtil.sendHttpResponse(exchange, 200, response);
                return;
            }

            // === POST: Thêm sản phẩm (cần SELLER auth) ===
            if ("POST".equals(method)) {
                String jsonBody = HttpServerUtil.readRequestBody(exchange);
                if (jsonBody.isEmpty()) {
                    HttpResponseUtil.sendHttpResponse(exchange, 400, new ResponseDTO("fail", "Dữ liệu trống"));
                    return;
                }

                // Kiểm tra Authorization header
                String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    HttpResponseUtil.sendHttpResponse(exchange, 401, new ResponseDTO("fail", "Vui lòng đăng nhập"));
                    return;
                }

                String token = authHeader.substring(7);
                DecodedJWT jwt = JwtUtil.verifyToken(token);
                if (jwt == null) {
                    HttpResponseUtil.sendHttpResponse(exchange, 401, new ResponseDTO("fail", "Token không hợp lệ"));
                    return;
                }

                // Kiểm tra role = SELLER
                if (!"SELLER".equals(jwt.getClaim("role").asString())) {
                    HttpResponseUtil.sendHttpResponse(exchange, 403, new ResponseDTO("fail", "Chỉ SELLER mới có quyền thêm sản phẩm"));
                    return;
                }

                // Parse ItemDTO
                ItemDTO itemDTO = gson.fromJson(jsonBody, ItemDTO.class);
                String sellerUsername = jwt.getSubject();

                // Thêm item vào database
                String resultMessage = itemService.addItem(
                        itemDTO.getName(),
                        itemDTO.getDescription(),
                        itemDTO.getStartingPrice(),
                        sellerUsername
                );
                if ("success".equals(resultMessage)) {
                    // Lấy item vừa thêm
                    Item newItem = itemService.getLastInsertedItem();

                    // Tạo phiên đấu giá mới với ID thật
                    AuctionManager.getInstance().createNewSession(
                            newItem.getId(),
                            newItem.getSellerId(),
                            itemDTO.getStartingPrice(),
                            60
                    );

                    HttpResponseUtil.sendHttpResponse(exchange, 201,
                            new ResponseDTO("success", "Thêm sản phẩm thành công! Mã: " + newItem.getId()));
                } else {
                    HttpResponseUtil.sendHttpResponse(exchange, 400, new ResponseDTO("fail", resultMessage));
                }
                return;
            }

            HttpResponseUtil.sendHttpResponse(exchange, 405, new ResponseDTO("fail", "Phương thức không hỗ trợ"));

        } catch (Exception e) {
            HttpResponseUtil.sendHttpResponse(exchange, 500, new ResponseDTO("error", "Lỗi Server: " + e.getMessage()));
        }
    }
}
