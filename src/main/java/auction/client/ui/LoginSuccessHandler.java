package auction.client.ui;

// 1. TẠO INTERFACE MỚI ĐỂ TRUYỀN 3 THAM SỐ (Username, Role, Token)
@FunctionalInterface
public interface LoginSuccessHandler {
    void handle(String username, String role, String token);
}
