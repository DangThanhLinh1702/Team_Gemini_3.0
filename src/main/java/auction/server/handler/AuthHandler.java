package auction.server.handler;

import auction.server.model.User;
import auction.server.service.UserService;
import auction.server.util.HttpServerUtil;
import auction.shared.dto.ResponseDTO;
import auction.shared.dto.UserDTO;
import auction.shared.util.HttpResponseUtil;
import auction.shared.util.JwtUtil;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AuthHandler implements HttpHandler {
    private final UserService userService = new UserService();
    private final Gson gson = new Gson();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String httpMethod = exchange.getRequestMethod().toUpperCase();
            String path = exchange.getRequestURI().getPath();

            // ── GET /users — lấy danh sách tất cả user (ADMIN) ───────────────
            if ("GET".equals(httpMethod) && "/users".equals(path)) {
                String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7);
                    DecodedJWT jwt = JwtUtil.verifyToken(token);
                    if (jwt != null && "ADMIN".equals(jwt.getClaim("role").asString())) {
                        var userList = userService.getAllUsers();
                        ResponseDTO response = new ResponseDTO("success", "Lấy danh sách thành công", userList);
                        HttpResponseUtil.sendHttpResponse(exchange, 200, response);
                        return;
                    }
                }
                HttpResponseUtil.sendHttpResponse(exchange, 403, new ResponseDTO("fail", "Chỉ ADMIN mới có quyền xem"));
                return;
            }

            // ── DELETE /users/{username} — xóa user (ADMIN) ──────────────────
            if ("DELETE".equals(httpMethod) && path.startsWith("/users/")) {
                if (!isAdminRequest(exchange)) {
                    HttpResponseUtil.sendHttpResponse(exchange, 403, new ResponseDTO("fail", "Chỉ ADMIN mới có quyền này"));
                    return;
                }
                String rawUsername = path.substring("/users/".length());
                String targetUsername = java.net.URLDecoder.decode(rawUsername, java.nio.charset.StandardCharsets.UTF_8);
                if (targetUsername.isEmpty()) {
                    HttpResponseUtil.sendHttpResponse(exchange, 400, new ResponseDTO("fail", "Thiếu username"));
                    return;
                }
                boolean deleted = userService.deleteUser(targetUsername);
                if (deleted) {
                    HttpResponseUtil.sendHttpResponse(exchange, 200, new ResponseDTO("success", "Đã xóa tài khoản: " + targetUsername));
                } else {
                    HttpResponseUtil.sendHttpResponse(exchange, 404, new ResponseDTO("fail", "Không tìm thấy tài khoản"));
                }
                return;
            }

            // ── POST /users/{username}/block — block user (ADMIN) ─────────────
            if ("POST".equals(httpMethod) && path.matches("/users/.+/block")) {
                if (!isAdminRequest(exchange)) {
                    HttpResponseUtil.sendHttpResponse(exchange, 403, new ResponseDTO("fail", "Chỉ ADMIN mới có quyền này"));
                    return;
                }
                String rawUsername = path.replaceFirst("/users/", "").replace("/block", "");
                String targetUsername = java.net.URLDecoder.decode(rawUsername, java.nio.charset.StandardCharsets.UTF_8);
                boolean blocked = userService.blockUser(targetUsername);
                if (blocked) {
                    HttpResponseUtil.sendHttpResponse(exchange, 200, new ResponseDTO("success", "Đã block tài khoản: " + targetUsername));
                } else {
                    HttpResponseUtil.sendHttpResponse(exchange, 404, new ResponseDTO("fail", "Không tìm thấy tài khoản"));
                }
                return;
            }

            // ── POST /users/{username}/unblock — unblock user (ADMIN) ─────────
            if ("POST".equals(httpMethod) && path.matches("/users/.+/unblock")) {
                if (!isAdminRequest(exchange)) {
                    HttpResponseUtil.sendHttpResponse(exchange, 403, new ResponseDTO("fail", "Chỉ ADMIN mới có quyền này"));
                    return;
                }
                String rawUsername = path.replaceFirst("/users/", "").replace("/unblock", "");
                String targetUsername = java.net.URLDecoder.decode(rawUsername, java.nio.charset.StandardCharsets.UTF_8);
                boolean unblocked = userService.unblockUser(targetUsername);
                if (unblocked) {
                    HttpResponseUtil.sendHttpResponse(exchange, 200, new ResponseDTO("success", "Đã unblock tài khoản: " + targetUsername));
                } else {
                    HttpResponseUtil.sendHttpResponse(exchange, 404, new ResponseDTO("fail", "Không tìm thấy tài khoản"));
                }
                return;
            }

            if ("GET".equals(httpMethod) && "/me".equals(path)) {
                handleGetMe(exchange);
                return;
            }

            if (!"POST".equals(httpMethod)) {
                HttpResponseUtil.sendHttpResponse(exchange, 405, new ResponseDTO("fail", "Phương thức không hỗ trợ"));
                return;
            }

            String jsonBody = HttpServerUtil.readRequestBody(exchange);
            UserDTO userDTO = gson.fromJson(jsonBody, UserDTO.class);

            if ("/login".equals(path)) {
                handleLogin(exchange, userDTO);
            } else if ("/register".equals(path)) {
                handleRegister(exchange, userDTO);
            } else {
                HttpResponseUtil.sendHttpResponse(exchange, 404, new ResponseDTO("fail", "Đường dẫn không tồn tại"));
            }

        } catch (Exception e) {
            HttpResponseUtil.sendHttpResponse(exchange, 500, new ResponseDTO("error", "Lỗi Server: " + e.getMessage()));
        }
    }

    // Helper: kiểm tra request có phải từ ADMIN không
    private boolean isAdminRequest(HttpExchange exchange) {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            DecodedJWT jwt = JwtUtil.verifyToken(token);
            return jwt != null && "ADMIN".equals(jwt.getClaim("role").asString());
        }
        return false;
    }

    private void handleLogin(HttpExchange exchange, UserDTO userDTO) throws IOException {
        User user = userService.loginAndGetUser(userDTO.getUsername(), userDTO.getPassword());
        if (user != null) {
            String token = JwtUtil.createToken(user.getUsername(), user.getRole());

            // Trả về data dạng Object gồm token + role để client đọc được
            Map<String, String> data = new HashMap<>();
            data.put("token", token);
            data.put("role", user.getRole());

            ResponseDTO successResponse = new ResponseDTO("success", "Đăng nhập thành công", data);
            HttpResponseUtil.sendHttpResponse(exchange, 200, successResponse);
        } else {
            HttpResponseUtil.sendHttpResponse(exchange, 401, new ResponseDTO("fail", "Sai tài khoản hoặc mật khẩu"));
        }
    }

    private void handleRegister(HttpExchange exchange, UserDTO userDTO) throws IOException {
        String result = userService.register(userDTO.getUsername(), userDTO.getPassword(), userDTO.getRole());
        if ("success".equals(result)) {
            HttpResponseUtil.sendHttpResponse(exchange, 201, new ResponseDTO("success", "Đăng ký thành công"));
        } else {
            HttpResponseUtil.sendHttpResponse(exchange, 400, new ResponseDTO("fail", result));
        }
    }

    private void handleGetMe(HttpExchange exchange) throws IOException {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String username = JwtUtil.getUsernameFromToken(token);

            User user = userService.getUserByUsername(username);
            if (user != null) {
                ResponseDTO response = new ResponseDTO("success", "Lấy thông tin người dùng thành công", user);
                HttpResponseUtil.sendHttpResponse(exchange, 200, response);
            } else {
                HttpResponseUtil.sendHttpResponse(exchange, 404, new ResponseDTO("fail", "Người dùng không tồn tại"));
            }
        } else {
            HttpResponseUtil.sendHttpResponse(exchange, 401, new ResponseDTO("fail", "Không có quyền truy cập"));
        }
    }
}