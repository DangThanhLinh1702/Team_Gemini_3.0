package auction.client.ui;

import auction.client.network.HttpClientUtil;
import auction.shared.util.JsonUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class LoginView extends VBox {

    // Hai hành động này được truyền từ ClientMain vào
    public LoginView(Consumer<String> onLoginSuccess, Runnable onSignUpRequested) {
        this.setSpacing(15);
        this.setPadding(new Insets(50));
        this.setAlignment(Pos.CENTER);
        this.setStyle("-fx-background-color: #f4f4f4;");

        Label lblTitle = new Label("ĐĂNG NHẬP");
        lblTitle.setFont(Font.font("System", FontWeight.BOLD, 30));

        TextField txtUsername = new TextField();
        txtUsername.setPromptText("Tên đăng nhập");
        txtUsername.setMaxWidth(300);

        PasswordField txtPassword = new PasswordField();
        txtPassword.setPromptText("Mật khẩu");
        txtPassword.setMaxWidth(300);

        Button btnLogin = new Button("Đăng nhập");
        btnLogin.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; -fx-font-weight: bold;");
        btnLogin.setPrefWidth(300);

        Hyperlink hlSignUp = new Hyperlink("Chưa có tài khoản? Đăng ký ngay");

        btnLogin.setOnAction(e -> {
            String username = txtUsername.getText().trim();
            String password = txtPassword.getText().trim();

            if (username.isEmpty() || password.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Thông báo", "Vui lòng nhập đầy đủ thông tin!");
                return;
            }

            try {
                // 1. Tạo nội dung JSON
                String jsonBody = String.format("{\"username\":\"%s\", \"password\":\"%s\"}", username, password);

                // 2. Gửi yêu cầu POST đến Server (Đã sửa "/auth/login" thành "/login")
                String response = HttpClientUtil.sendPost("/login", jsonBody);

                // THÊM DÒNG NÀY ĐỂ DEBUG XEM SERVER TRẢ VỀ CÁI GÌ NHÉ
                System.out.println("=== PHẢN HỒI TỪ SERVER ===: " + response);

                // 3. Kiểm tra phản hồi từ Server
                if (response != null && response.contains("\"status\":\"success\"")) {
                    onLoginSuccess.accept(username); // Chuyển sang màn hình Đấu giá
                } else {
                    showAlert(Alert.AlertType.ERROR, "Thất bại", "Tài khoản hoặc mật khẩu không chính xác!");
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Lỗi kết nối",
                        "Không thể kết nối đến máy chủ! (Hãy chắc chắn bạn đã chạy ServerMain)");
            }
        });

        hlSignUp.setOnAction(e -> onSignUpRequested.run());

        this.getChildren().addAll(lblTitle, txtUsername, txtPassword, btnLogin, hlSignUp);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
