package auction.server.handler;

import auction.server.model.User;
import auction.server.service.UserService;
import auction.server.util.HttpServerUtil;
import auction.shared.dto.ResponseDTO;
import auction.shared.dto.UserDTO;
import auction.shared.util.HttpResponseUtil;
import auction.shared.util.JwtUtil;
import com.auth0.jwt.JWT;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

public class AuthHandler implements HttpHandler {
    private final UserService userService = new UserService();
    private final Gson gson = new Gson();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String httpMethod = exchange.getRequestMethod().toUpperCase();
            String path = exchange.getRequestURI().getPath();

            if ("GET".equals(httpMethod) && "/users".equals(path)) {
                var userList = userService.getAllUsers();
                ResponseDTO response = new ResponseDTO("success", "Lấy danh sách thành công", userList);
                HttpResponseUtil.sendHttpResponse(exchange, 200, response);
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

    private void handleLogin(HttpExchange exchange, UserDTO userDTO) throws IOException {
        User user = userService.loginAndGetUser(userDTO.getUsername(), userDTO.getPassword());
        if (user != null) {
            String token = JwtUtil.createToken(user.getUsername(), user.getRole());

            ResponseDTO successResponse = new ResponseDTO("success", "Đăng nhập thành công", token);
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
}