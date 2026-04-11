package auction.shared.util;

import auction.shared.dto.ResponseDTO;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class HttpResponseUtil {

    public static void sendHttpResponse(HttpExchange exchange, int statusCode, ResponseDTO responseDto) throws IOException {
        // Sử dụng JsonUtil để lấy chuỗi JSON thay vì tự tạo
        String jsonResponse = JsonUtil.toJson(responseDto);
        byte[] responseBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(responseBytes);
        }
    }
}