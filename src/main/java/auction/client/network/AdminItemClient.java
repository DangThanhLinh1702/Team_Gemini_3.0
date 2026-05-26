package auction.client.network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * HTTP client dành riêng cho ADMIN: sửa và xóa sản phẩm/phiên đấu giá.
 * Mọi request đều gửi kèm JWT token để server xác thực quyền ADMIN.
 */
public class AdminItemClient {

    private static final String SERVER_URL = "http://localhost:8080";
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private static final Gson gson = new Gson();

    // ─── Kết quả trả về cho UI ────────────────────────────────────────────────
    public static class Result {
        public final boolean success;
        public final String message;

        public Result(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    /**
     * Gửi DELETE /items/{itemId} — xóa sản phẩm + phiên đấu giá.
     *
     * @param jwtToken token của tài khoản ADMIN
     * @param itemId   ID sản phẩm cần xóa
     */
    public static Result deleteItem(String jwtToken, String itemId) {
        if (jwtToken == null || jwtToken.isBlank()) {
            return new Result(false, "Token không hợp lệ.");
        }
        if (itemId == null || itemId.isBlank()) {
            return new Result(false, "Item ID không hợp lệ.");
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(SERVER_URL + "/items/" + itemId))
                    .header("Authorization", "Bearer " + jwtToken)
                    .DELETE()
                    .timeout(Duration.ofSeconds(5))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            return parseServerResult(response);

        } catch (java.net.ConnectException e) {
            return new Result(false, "Không thể kết nối tới server.");
        } catch (Exception e) {
            System.err.println("[AdminItemClient] deleteItem lỗi: " + e.getMessage());
            return new Result(false, "Lỗi: " + e.getMessage());
        }
    }

    /**
     * Gửi PUT /items/{itemId} — sửa tên, mô tả, giá khởi điểm.
     *
     * @param jwtToken     token của tài khoản ADMIN
     * @param itemId       ID sản phẩm cần sửa
     * @param name         tên mới (không được rỗng)
     * @param description  mô tả mới
     * @param startingPrice giá mới (phải > 0)
     */
    public static Result updateItem(String jwtToken, String itemId,
                                    String name, String description,
                                    double startingPrice) {
        // ── Validate trước khi gọi mạng ──────────────────────────────────────
        if (jwtToken == null || jwtToken.isBlank())
            return new Result(false, "Token không hợp lệ.");
        if (itemId == null || itemId.isBlank())
            return new Result(false, "Item ID không hợp lệ.");
        if (name == null || name.isBlank())
            return new Result(false, "Tên sản phẩm không được để trống.");
        if (startingPrice <= 0)
            return new Result(false, "Giá khởi điểm phải lớn hơn 0.");

        try {
            // ── Build JSON body ───────────────────────────────────────────────
            JsonObject body = new JsonObject();

            //  ĐÃ SỬA: Thêm thuộc tính "id" kiểu số nguyên vào body để đồng bộ với ItemDTO của Server
            body.addProperty("id", Integer.parseInt(itemId.trim()));

            body.addProperty("name",         name.trim());
            body.addProperty("description",  description == null ? "" : description.trim());
            body.addProperty("startingPrice", startingPrice);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(SERVER_URL + "/items/" + itemId))
                    .header("Content-Type",  "application/json")
                    .header("Authorization", "Bearer " + jwtToken)
                    .PUT(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                    .timeout(Duration.ofSeconds(5))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            return parseServerResult(response);

        } catch (java.net.ConnectException e) {
            return new Result(false, "Không thể kết nối tới server.");
        } catch (Exception e) {
            System.err.println("[AdminItemClient] updateItem lỗi: " + e.getMessage());
            return new Result(false, "Lỗi: " + e.getMessage());
        }
    }

    // ─── Đọc { status, message } từ response server ───────────────────────────
    private static Result parseServerResult(HttpResponse<String> response) {
        try {
            String body = response.body();
            if (body == null || body.isBlank()) {
                // Không có body — dựa vào HTTP status code
                boolean ok = response.statusCode() >= 200 && response.statusCode() < 300;
                return new Result(ok, ok ? "Thành công." : "Server trả về lỗi " + response.statusCode());
            }

            JsonObject json = JsonParser.parseString(body).getAsJsonObject();

            // Đọc message từ server nếu có, fallback về HTTP status
            String msg = json.has("message")
                    ? json.get("message").getAsString()
                    : "HTTP " + response.statusCode();

            String status = json.has("status") ? json.get("status").getAsString() : "";
            boolean ok = "success".equalsIgnoreCase(status)
                    || (response.statusCode() >= 200 && response.statusCode() < 300);

            return new Result(ok, msg);

        } catch (Exception e) {
            // Body không phải JSON — dựa vào HTTP status
            boolean ok = response.statusCode() >= 200 && response.statusCode() < 300;
            return new Result(ok, ok ? "Thành công." : "Lỗi server: " + response.statusCode());
        }
    }
}