package auction.server.handler;

import auction.server.service.ItemService;
import auction.shared.dto.ItemDTO;
import auction.shared.dto.ResponseDTO;
import auction.shared.util.HttpResponseUtil;
import auction.shared.util.JwtUtil;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class AddItemHandler implements HttpHandler {
    private final Gson gson = new Gson();
    private final ItemService itemService = new ItemService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            HttpResponseUtil.sendHttpResponse(exchange, 405, new ResponseDTO("fail", "phương thức không hỗ trợ", null));
            return;
        }
        try {
            String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                HttpResponseUtil.sendHttpResponse(exchange, 401, new ResponseDTO("fail", "Chưa đăng nhập!", null));
                return;
            }
            String token = authHeader.substring(7); // cắt chuỗi token từ vị trí số 7 trở đi
            DecodedJWT jwt = JwtUtil.verifyToken(token);
            if (jwt == null) {
                HttpResponseUtil.sendHttpResponse(exchange, 401, new ResponseDTO("fail", "Token không hợp lệ!", null));
                return;
            }
            if (!"SELLER".equals(jwt.getClaim("role").asString())) {
                HttpResponseUtil.sendHttpResponse(exchange, 403, new ResponseDTO("fail", "Bạn là Bidder, không có quyền thêm sản phẩm!", null));
                return;
            }
            String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            if (requestBody.isEmpty()) {
                HttpResponseUtil.sendHttpResponse(exchange, 400, new ResponseDTO("fail", "Dữ liệu trống!", null));
                return;
            }
            ItemDTO itemDTO = gson.fromJson(requestBody, ItemDTO.class);
            String sellerUsername = jwt.getSubject(); // Lấy username từ token
            String result = itemService.addItem(
                    itemDTO.getName(),
                    itemDTO.getDescription(),
                    itemDTO.getStartingPrice(),
                    sellerUsername
            );
            if ("success".equals(result)) {
                HttpResponseUtil.sendHttpResponse(exchange, 201, new ResponseDTO("success", "Thêm sản phẩm thành công!", null));

            } else {
                HttpResponseUtil.sendHttpResponse(exchange, 400, new ResponseDTO("fail", result, null));
            }
        } catch (Exception e) {
            HttpResponseUtil.sendHttpResponse(exchange, 500, new ResponseDTO("error", "Lỗi Server: " + e.getMessage(), null));
        }
    }
}
