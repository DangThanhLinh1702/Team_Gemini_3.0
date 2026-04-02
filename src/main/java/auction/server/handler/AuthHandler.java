package auction.server.handler;

import auction.server.service.UserService;
import auction.server.util.HttpServerUtil;
import auction.shared.dto.ResponseDTO;
import auction.shared.dto.UserDTO;
import auction.shared.util.HttpResponseUtil;
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
            } else if (!"POST".equals(httpMethod)) {
                ResponseDTO response = new ResponseDTO("fail", "phương thức không hỗ trợ");
                HttpResponseUtil.sendHttpResponse(exchange, 405, response);
                return;
            }

            // 405 error Method not allowed: server khong cho dung
            // lay du lieu tu client: doc bytes tu inputStream -> mang bytes -> string(json) -> object

            String jsonBody = HttpServerUtil.readRequestBody(exchange);
            UserDTO userDTO = gson.fromJson(jsonBody, UserDTO.class);

            String resultMessage = "";
            if ("/login".equals(path)) {
                resultMessage = userService.login(userDTO.getUsername(), userDTO.getPassword());
            } else if ("/register".equals(path)) {
                resultMessage = userService.register(userDTO.getUsername(), userDTO.getPassword(), userDTO.getRole());
            } else {
                HttpResponseUtil.sendHttpResponse(exchange, 404, new ResponseDTO("fail", "Đường dẫn không tồn tại"));
                return;
            }

            if ("success".equals(resultMessage) && "/login".equals(path)) {
                ResponseDTO successResponse = new ResponseDTO("success", "đăng nhập thành công");
                HttpResponseUtil.sendHttpResponse(exchange, 200, successResponse);
                return;
            } else if ("success".equals(resultMessage) && "/register".equals(path)) {
                ResponseDTO successResponse = new ResponseDTO("success", "đăng kí thành công");
                HttpResponseUtil.sendHttpResponse(exchange, 200, successResponse);
            } else {
                ResponseDTO failResponse = new ResponseDTO("fail", resultMessage);
                HttpResponseUtil.sendHttpResponse(exchange, 401, failResponse);
            }
        }catch (Exception e) {
            HttpResponseUtil.sendHttpResponse(exchange, 500, new ResponseDTO("error", "Lỗi nội bộ Server: " + e.getMessage()));
        }
    }
}