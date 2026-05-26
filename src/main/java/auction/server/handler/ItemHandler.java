package auction.server.handler;

import auction.server.core.AuctionManager;
import auction.server.model.Item;
import auction.server.model.Admin;
import auction.server.repository.ItemRepository;
import auction.server.service.UserService;
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
    private final UserService userService = new UserService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod().toUpperCase();

            // 1. LẤY DANH SÁCH (GET)
            if ("GET".equals(method)) {
                var listItem = itemRepository.getAllItemsFromDatabase();
                ResponseDTO response = new ResponseDTO("success", "Lấy danh sách sản phẩm thành công", listItem);
                HttpResponseUtil.sendHttpResponse(exchange, 200, response);
                return;
            }

            // 2. THÊM MỚI (POST)
            if ("POST".equals(method)) {
                String jsonBody = HttpServerUtil.readRequestBody(exchange);
                if (jsonBody.isEmpty()) {
                    HttpResponseUtil.sendHttpResponse(exchange, 400, new ResponseDTO("fail", "Dữ liệu trống"));
                    return;
                }

                ItemDTO itemDTO = gson.fromJson(jsonBody, ItemDTO.class);
                String sellerUsername = getUsernameFromToken(exchange);

                if (sellerUsername == null || sellerUsername.isEmpty()) sellerUsername = itemDTO.getSellerUsername();
                if (sellerUsername == null || sellerUsername.isEmpty()) sellerUsername = "UnknownSeller";

                long endTime = System.currentTimeMillis() + (60 * 1000L); // Mặc định 60 giây
                String imageBase64 = "";
                try {
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

            // 3. SỬA SẢN PHẨM (PUT) - CHỈ ADMIN
            if ("PUT".equals(method)) {
                String jsonBody = HttpServerUtil.readRequestBody(exchange);

                String requestUser = getUsernameFromToken(exchange);
                if (!isAdmin(requestUser)) {
                    HttpResponseUtil.sendHttpResponse(exchange, 403, new ResponseDTO("fail", "Từ chối truy cập! Chỉ ADMIN mới có quyền sửa."));
                    return;
                }

                // ✅ ĐÃ SỬA: Dùng hàm mới để lấy ID linh hoạt
                int itemId = getIdFromRequest(exchange);
                if (itemId == -1) {
                    HttpResponseUtil.sendHttpResponse(exchange, 400, new ResponseDTO("fail", "Thiếu hoặc sai ID sản phẩm hợp lệ"));
                    return;
                }

                ItemDTO itemDTO = gson.fromJson(jsonBody, ItemDTO.class);

                boolean isUpdated = itemRepository.updateItem(itemId, itemDTO.getName(), itemDTO.getDescription(), itemDTO.getStartingPrice(), itemDTO.getImage());
                if (isUpdated) {
                    HttpResponseUtil.sendHttpResponse(exchange, 200, new ResponseDTO("success", "Cập nhật thông tin sản phẩm thành công!"));
                } else {
                    HttpResponseUtil.sendHttpResponse(exchange, 404, new ResponseDTO("fail", "Không tìm thấy sản phẩm hoặc lỗi CSDL"));
                }
                return;
            }

            // 4. XÓA SẢN PHẨM (DELETE) - CHỈ ADMIN
            if ("DELETE".equals(method)) {
                String requestUser = getUsernameFromToken(exchange);
                if (!isAdmin(requestUser)) {
                    HttpResponseUtil.sendHttpResponse(exchange, 403, new ResponseDTO("fail", "Từ chối truy cập! Chỉ ADMIN mới có quyền xóa."));
                    return;
                }

                // ✅ ĐÃ SỬA: Dùng hàm mới để lấy ID linh hoạt
                int itemId = getIdFromRequest(exchange);
                if (itemId == -1) {
                    HttpResponseUtil.sendHttpResponse(exchange, 400, new ResponseDTO("fail", "Thiếu hoặc sai ID sản phẩm hợp lệ"));
                    return;
                }

                boolean isDeleted = itemRepository.deleteItemById(itemId);
                if (isDeleted) {
                    HttpResponseUtil.sendHttpResponse(exchange, 200, new ResponseDTO("success", "Xóa sản phẩm thành công khỏi hệ thống!"));
                } else {
                    HttpResponseUtil.sendHttpResponse(exchange, 404, new ResponseDTO("fail", "Không tìm thấy sản phẩm cần xóa hoặc lỗi CSDL"));
                }
                return;
            }

            HttpResponseUtil.sendHttpResponse(exchange, 405, new ResponseDTO("fail", "Phương thức không hỗ trợ"));

        } catch (Exception e) {
            e.printStackTrace();
            HttpResponseUtil.sendHttpResponse(exchange, 500, new ResponseDTO("error", "Lỗi xử lý Server: " + e.getMessage()));
        }
    }

    private String getUsernameFromToken(HttpExchange exchange) {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                DecodedJWT jwt = JwtUtil.verifyToken(token);
                if (jwt != null) return jwt.getSubject();
            } catch (Exception e) {
                System.out.println("[WARNING] Lỗi giải mã Token.");
            }
        }
        return null;
    }

    private boolean isAdmin(String username) {
        if (username == null || username.isEmpty()) return false;
        var user = userService.getUserByUsername(username);
        return user instanceof Admin;
    }

    // ✅ ĐÃ SỬA: Hàm mới hỗ trợ lấy ID từ URL (/items/1) VÀ từ Query (?id=1)
    private int getIdFromRequest(HttpExchange exchange) {
        try {
            // 1. Thử lấy từ đường dẫn Path (RESTful - VD: /items/1)
            String path = exchange.getRequestURI().getPath();
            if (path != null) {
                String[] segments = path.split("/");
                // segments của "/items/1" sẽ là: ["", "items", "1"]
                if (segments.length >= 3 && segments[1].equals("items")) {
                    try {
                        return Integer.parseInt(segments[2]);
                    } catch (NumberFormatException ignored) {}
                }
            }

            // 2. Thử lấy từ Query String (VD: ?id=1)
            String query = exchange.getRequestURI().getQuery();
            if (query != null && query.contains("id=")) {
                String[] pairs = query.split("&");
                for (String pair : pairs) {
                    if (pair.startsWith("id=")) {
                        return Integer.parseInt(pair.substring(3));
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Lỗi phân tích ID từ URL: " + e.getMessage());
        }
        return -1; // Trả về -1 nếu không tìm thấy
    }
}