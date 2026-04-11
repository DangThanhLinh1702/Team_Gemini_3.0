package auction.shared.util;

import auction.shared.dto.ResponseDTO;
import com.google.gson.Gson;

/**
 * Lớp tiện ích chuyên trách việc xử lý JSON.
 * Giúp thống nhất cấu trúc phản hồi (Response) cho toàn hệ thống.
 */
public class JsonUtil {
    private static final Gson gson = new Gson();

    // Biến bất kỳ đối tượng nào thành chuỗi JSON
    public static String toJson(Object object) {
        return gson.toJson(object);
    }

    // Tạo nhanh một chuỗi JSON theo cấu trúc ResponseDTO chuẩn
    public static String buildResponse(String status, String message, Object data) {
        ResponseDTO response = new ResponseDTO(status, message, data);
        return gson.toJson(response);
    }
}