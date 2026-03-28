package auction.server.handler;

import auction.server.service.UserService;
import auction.shared.dto.ResponseDTO;
import auction.shared.dto.UserDTO;
import auction.shared.util.HttpResponseUtil;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class AuthHandler implements HttpHandler {
    private UserService userService = new UserService();
    private Gson gson = new Gson();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String httpMethod = exchange.getRequestMethod().toUpperCase();

        String path = exchange.getRequestURI().getPath();
        if ("GET".equals(httpMethod) && "/users".equals(path)) {
            var userList = userService.getAllUsers();

            ResponseDTO response = new ResponseDTO("success", "Lấy danh sách thành công", userList);
            HttpResponseUtil.sendHttpResponse(exchange, 200, response);
            return;
        }
        else if (!"POST".equals(httpMethod)) {
            ResponseDTO response = new ResponseDTO("fail", "chỉ cho POST");
            HttpResponseUtil.sendHttpResponse(exchange, 405, response);
            return;
        }

        // 405 error Method not allowed: server khong cho dung
        // lay du lieu tu client: doc bytes tu inputStream -> mang bytes -> string(json) -> object
        byte[] requestBytes = exchange.getRequestBody().readAllBytes();
        String jsonBody = new String(requestBytes, StandardCharsets.UTF_8);
        UserDTO userDTO = gson.fromJson(jsonBody, UserDTO.class);
        // lay duong dan
        String resultMessage = "";
        if ("/login".equals(path)) {
            resultMessage = userService.login(userDTO.getUsername(), userDTO.getPassword());
        } else if ("/register".equals(path)) {
            resultMessage = userService.register(userDTO.getUsername(), userDTO.getPassword());
        } else {
            HttpResponseUtil.sendHttpResponse(exchange, 404, new ResponseDTO("fail", "Đường dẫn không tồn tại"));
        }

        if ("success".equals(resultMessage)) {
            ResponseDTO successResponse = new ResponseDTO("success", "thao tác thành công");
            HttpResponseUtil.sendHttpResponse(exchange, 200, successResponse);
        } else {
            ResponseDTO failResponse = new ResponseDTO("fail", resultMessage);
            HttpResponseUtil.sendHttpResponse(exchange, 401, failResponse);
        }
    }
}