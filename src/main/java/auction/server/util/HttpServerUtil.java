package auction.server.util;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class HttpServerUtil {
    private static final Gson GSON = new Gson();
    public static String readRequestBody(HttpExchange exchange) throws IOException {
        byte[] requestByte = exchange.getRequestBody().readAllBytes();
        return new String(requestByte, StandardCharsets.UTF_8);
    }
}
