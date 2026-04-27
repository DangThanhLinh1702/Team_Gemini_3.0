package auction.server.handler;

import auction.server.model.User;
import auction.server.repository.UserRepository;
import auction.shared.dto.LoginRequestDTO;
import auction.shared.dto.ResponseDTO;
import auction.shared.util.HttpResponseUtil;
import auction.shared.util.JsonUtil;
import auction.shared.util.JwtUtil;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class LoginHandler implements HttpHandler {
    private final Gson gson = new Gson();
    private final UserRepository userRepository = new UserRepository();
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if(!"POST".equals(exchange.getRequestMethod())){
            HttpResponseUtil.sendHttpResponse(exchange, 405, new ResponseDTO("fail", "phương thức không hỗ trợ", null));
            return;
        }
        try {
            String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8); // Đọc dữ liệu JSON từ body
            LoginRequestDTO loginData = gson.fromJson(requestBody, LoginRequestDTO.class);
            User user = userRepository.authenticate(loginData.getUsername(), loginData.getPassword());
            if (user != null) {
                // Đăng nhập đúng -> Tạo Token
                String token = JwtUtil.createToken(user.getUsername(), user.getRole());

                // Gói token vào data trả về
                Map<String, String> data = new HashMap<>();
                data.put("token", token);
                data.put("role", user.getRole());
                HttpResponseUtil.sendHttpResponse(exchange, 200, new ResponseDTO("success", "Đăng nhập thành công!", data));
            } else {
                HttpResponseUtil.sendHttpResponse(exchange, 401, new ResponseDTO("fail", "Sai tên đăng nhập hoặc mật khẩu!", null));
            }
        } catch (Exception e) {
            HttpResponseUtil.sendHttpResponse(exchange, 500, new ResponseDTO("error", "Lỗi Server: " + e.getMessage(), null));
        }
    }
}
