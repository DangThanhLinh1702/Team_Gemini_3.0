package auction.client.network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Lớp chuyên xử lý các API gọi lên Server (Đăng nhập, Đăng ký)
 */
public class AuthClient {

    // Thay đổi port nếu Server HTTP của bạn chạy port khác (ví dụ: 8080)
    private static final String SERVER_URL = "http://localhost:8080";
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final Gson gson = new Gson();

    /**
     * Hàm gửi yêu cầu Đăng Nhập
     * @return Chuỗi JWT Token nếu thành công, hoặc null nếu thất bại
     */
    public static String login(String username, String password) {
        try {
            // Tạo JSON chứa thông tin đăng nhập
            String jsonBody = String.format("{\"username\":\"%s\", \"password\":\"%s\"}", username, password);

            // Tạo gói hàng HTTP POST
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(SERVER_URL + "/api/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            // Gửi đi và chờ phản hồi
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // Giả sử Server trả về JSON chứa token: { "status": "success", "data": { "token": "eyJhb..." } }
                JsonObject jsonObject = gson.fromJson(response.body(), JsonObject.class);
                if (jsonObject.has("data")) {
                    return jsonObject.getAsJsonObject("data").get("token").getAsString();
                }
            } else {
                System.err.println("Đăng nhập thất bại. Mã lỗi: " + response.statusCode());
            }
        } catch (Exception e) {
            System.err.println("Lỗi kết nối tới Server Đăng nhập: " + e.getMessage());
        }
        return null;
    }

    /**
     * Hàm gửi yêu cầu Đăng Ký
     */
    public static boolean register(String username, String password, String role) {
        try {
            String jsonBody = String.format("{\"username\":\"%s\", \"password\":\"%s\", \"role\":\"%s\"}", username, password, role);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(SERVER_URL + "/api/register"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            return response.statusCode() == 200; // Thành công nếu mã 200 OK
        } catch (Exception e) {
            System.err.println("Lỗi kết nối tới Server Đăng ký: " + e.getMessage());
            return false;
        }
    }
}