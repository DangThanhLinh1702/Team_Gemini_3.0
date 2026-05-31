package auction.server.handler;

import auction.server.service.ItemService;
import auction.shared.dto.ItemDTO;
import auction.shared.dto.ResponseDTO;
import auction.server.util.HttpServerUtil;
import auction.shared.util.HttpResponseUtil;
import auction.shared.util.JwtUtil;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

public class AddItemHandler implements HttpHandler {
    private final ItemService itemService = new ItemService();
    private final Gson gson = new Gson();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if (!"POST".equals(exchange.getRequestMethod().toUpperCase())) {
                HttpResponseUtil.sendHttpResponse(exchange, 405, new ResponseDTO("fail", "Phương thức không hỗ trợ"));
                return;
            }

            String requestBody = HttpServerUtil.readRequestBody(exchange);
            if (requestBody == null || requestBody.isEmpty()) {
                HttpResponseUtil.sendHttpResponse(exchange, 400, new ResponseDTO("fail", "Dữ liệu trống"));
                return;
            }

            ItemDTO itemDTO = gson.fromJson(requestBody, ItemDTO.class);

            String sellerUsername = null;
            String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                try {
                    String token = authHeader.substring(7);
                    DecodedJWT jwt = JwtUtil.verifyToken(token);
                    if (jwt != null) {
                        sellerUsername = jwt.getSubject();
                    }
                } catch (Exception e) {
                    System.out.println("[WARNING] Lỗi giải mã Token.");
                }
            }

            if (sellerUsername == null || sellerUsername.isEmpty()) {
                sellerUsername = "UnknownSeller";
            }

            // Đã sửa: Tính toán thời gian kết thúc (mặc định thêm 120 giây phiên đấu giá)
            long endTime = System.currentTimeMillis() + (120 * 1000L);

            // Đã sửa: Lấy chuỗi ảnh từ DTO gửi lên
            String imageBase64 = "";
            if (itemDTO.getImage() != null) {
                imageBase64 = itemDTO.getImage();
            }

            // Đã sửa: Truyền chuẩn chỉ toàn bộ 6 tham số vào Service để ghi xuống Database
            String result = itemService.addItem(
                    itemDTO.getName(),
                    itemDTO.getDescription(),
                    itemDTO.getStartingPrice(),
                    sellerUsername,
                    imageBase64,
                    endTime
            );

            if ("success".equals(result)) {
                HttpResponseUtil.sendHttpResponse(exchange, 201, new ResponseDTO("success", "Thêm sản phẩm thành công"));
            } else {
                HttpResponseUtil.sendHttpResponse(exchange, 400, new ResponseDTO("fail", result));
            }

        } catch (Exception e) {
            e.printStackTrace();
            HttpResponseUtil.sendHttpResponse(exchange, 500, new ResponseDTO("error", "Lỗi Server: " + e.getMessage()));
        }
    }
}