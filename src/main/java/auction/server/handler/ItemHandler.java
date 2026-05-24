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

            if ("GET".equals(method)) {
                var listItem = itemRepository.getAllItemsFromDatabase();
                ResponseDTO response = new ResponseDTO("success", "Lấy danh sách sản phẩm thành công", listItem);
                HttpResponseUtil.sendHttpResponse(exchange, 200, response);
                return;
            }

            if ("POST".equals(method)) {
                String jsonBody = HttpServerUtil.readRequestBody(exchange);
                if (jsonBody.isEmpty()) {
                    HttpResponseUtil.sendHttpResponse(exchange, 400, new ResponseDTO("fail", "Dữ liệu trống"));
                    return;
                }

                ItemDTO itemDTO = gson.fromJson(jsonBody, ItemDTO.class);

                String sellerUsername = null;
                String authHeader = exchange.getRequestHeaders().getFirst("Authorization");

                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    try {
                        String token = authHeader.substring(7);
                        DecodedJWT jwt = JwtUtil.verifyToken(token);
                        if (jwt != null) sellerUsername = jwt.getSubject();
                    } catch (Exception e) {
                        System.out.println("[WARNING] Lỗi giải mã Token.");
                    }
                }

                if (sellerUsername == null || sellerUsername.isEmpty()) sellerUsername = itemDTO.getSellerUsername();
                if (sellerUsername == null || sellerUsername.isEmpty()) sellerUsername = "UnknownSeller";

                // ĐÃ SỬA: Tính toán thời gian và nhét Ảnh vào đối tượng lưu DB
                long endTime = System.currentTimeMillis() + (60 * 1000L); // Mặc định 60 giây
                String imageBase64 = "";
                try {
                    // Cố gắng lấy ảnh từ DTO nếu Client có gửi
                    if (itemDTO.getImage() != null) imageBase64 = itemDTO.getImage();
                } catch (Exception e) {}

                Item itemToSave = new Item(itemDTO.getName(), itemDTO.getDescription(), itemDTO.getStartingPrice(), sellerUsername, imageBase64, endTime);
                itemRepository.saveItem(itemToSave);

                if (itemToSave.getId() > 0) {
                    AuctionManager.getInstance().createNewSession(
                            itemToSave.getId(),
                            itemToSave.getId(),
                            itemToSave.getStartingPrice(),
                            60
                    );
                    HttpResponseUtil.sendHttpResponse(exchange, 201,
                            new ResponseDTO("success", "Thêm sản phẩm thành công!", itemToSave.getId()));
                } else {
                    HttpResponseUtil.sendHttpResponse(exchange, 500, new ResponseDTO("fail", "Lỗi CSDL"));
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