package auction.shared.util;

import auction.shared.dto.ResponseDTO;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class HttpResponseUtil {
    private static final Gson gson = new Gson();
    public static void sendHttpResponse(HttpExchange exchange, int statusCode, ResponseDTO responseDto) throws IOException {
        // tra du lieu tu server -> client
        // bien object thanh json -> bytes -> gui qua outputStream

        String jsonResponse = gson.toJson(responseDto);
        byte[] responseBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
        // cai dat cau hinh cua header
        exchange.getResponseHeaders().add("content-type", "application/json; charsets=utf-8");
        exchange.sendResponseHeaders(statusCode,responseBytes.length);

        OutputStream outputStream = exchange.getResponseBody();
        outputStream.write(responseBytes);
        outputStream.close();
    }

}
