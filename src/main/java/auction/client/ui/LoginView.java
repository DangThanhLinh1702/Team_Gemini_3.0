package auction.client.ui;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class LoginView extends StackPane {

    // Thay đổi BiConsumer thành LoginSuccessHandler
    private final LoginSuccessHandler onLoginSuccess;
    private final Runnable onSignUp;

    public LoginView(LoginSuccessHandler onLoginSuccess, Runnable onSignUp) {
        this.onLoginSuccess = onLoginSuccess;
        this.onSignUp = onSignUp;

        // 1. TẠO BACKGROUND GRADIENT CHO MÀN HÌNH CHÍNH
        Stop[] bgStops = new Stop[]{
                new Stop(0, Color.web("#4facfe")),
                new Stop(1, Color.web("#00f2fe"))
        };
        LinearGradient bgGradient = new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE, bgStops);
        this.setBackground(new Background(new BackgroundFill(bgGradient, CornerRadii.EMPTY, Insets.EMPTY)));

        // 2. TẠO KHUNG ĐĂNG NHẬP (CARD)
        VBox card = new VBox(20);
        card.setMaxSize(400, 450);
        card.setPadding(new Insets(40));
        card.setAlignment(Pos.CENTER);
        card.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(15), Insets.EMPTY)));

        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.color(0, 0, 0, 0.25));
        shadow.setRadius(20);
        shadow.setOffsetY(10);
        card.setEffect(shadow);

        // 3. CÁC THÀNH PHẦN BÊN TRONG CARD
        Label title = new Label("Welcome Back");
        title.setFont(Font.font("System", FontWeight.BOLD, 28));
        title.setTextFill(Color.web("#333333"));

        Label subtitle = new Label("Please login to your auction account");
        subtitle.setTextFill(Color.web("#777777"));
        VBox.setMargin(subtitle, new Insets(0, 0, 10, 0));

        TextField usernameField = createCustomTextField("Username");
        PasswordField passwordField = createCustomPasswordField("Password");

        Button loginBtn = createCustomButton("LOGIN");

        // XỬ LÝ ĐĂNG NHẬP
        loginBtn.setOnAction(e -> {
            String user = usernameField.getText();
            String pass = passwordField.getText();

            if (user.isEmpty() || pass.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Lỗi", "Vui lòng nhập đầy đủ thông tin.");
                return;
            }

            new Thread(() -> {
                try {
                    String jsonBody = String.format("{\"username\":\"%s\", \"password\":\"%s\"}", user, pass);
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create("http://localhost:8080/login"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                            .build();

                    HttpClient client = HttpClient.newHttpClient();
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                    Platform.runLater(() -> {
                        if (response.statusCode() == 200) {
                            // IN RA ĐỂ KIỂM TRA DỮ LIỆU THẬT CỦA SERVER
                            System.out.println("=== PHẢN HỒI TỪ SERVER (LOGIN) ===");
                            System.out.println(response.body());

                            try {
                                JsonObject jsonObject = new Gson().fromJson(response.body(), JsonObject.class);
                                String role = null;
                                String token = null; // Khởi tạo biến lưu token

                                // Xử lý an toàn: Lấy data, bóc tách role và token
                                if (jsonObject.has("data") && jsonObject.get("data").isJsonObject()) {
                                    JsonObject dataObj = jsonObject.getAsJsonObject("data");
                                    if (dataObj.has("role")) {
                                        role = dataObj.get("role").getAsString();
                                    }
                                    if (dataObj.has("token")) {
                                        token = dataObj.get("token").getAsString(); // Lấy token từ JSON
                                    }
                                } else {
                                    // Trường hợp fallback nếu structure không nằm trong 'data'
                                    if (jsonObject.has("role")) {
                                        role = jsonObject.get("role").getAsString();
                                    }
                                    if (jsonObject.has("token")) {
                                        token = jsonObject.get("token").getAsString();
                                    }
                                }

                                if (role == null || role.trim().isEmpty() || token == null || token.trim().isEmpty()) {
                                    showAlert(Alert.AlertType.ERROR, "Lỗi đăng nhập", "Server không trả về đủ thông tin (role/token). Vui lòng thử lại.");
                                    return;
                                }

                                // Gọi callback và TRUYỀN TOKEN KÈM THEO
                                this.onLoginSuccess.handle(user, role.toUpperCase(), token);

                            } catch (Exception ex) {
                                System.err.println("Lỗi khi đọc JSON từ server: " + ex.getMessage());
                                showAlert(Alert.AlertType.ERROR, "Lỗi", "Phản hồi từ server không hợp lệ.");
                            }
                        } else {
                            showAlert(Alert.AlertType.ERROR, "Đăng nhập thất bại", "Sai tài khoản hoặc mật khẩu!");
                        }
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi Mạng", "Không thể kết nối đến Server!"));
                }
            }).start();
        });

        HBox signUpBox = new HBox(5);
        signUpBox.setAlignment(Pos.CENTER);
        VBox.setMargin(signUpBox, new Insets(15, 0, 0, 0));

        Label askLabel = new Label("Don't have an account?");
        askLabel.setTextFill(Color.web("#777777"));

        Label signUpLabel = new Label("Sign up");
        signUpLabel.setTextFill(Color.web("#4facfe"));
        signUpLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        signUpLabel.setCursor(Cursor.HAND);

        signUpLabel.setOnMouseEntered(e -> signUpLabel.setUnderline(true));
        signUpLabel.setOnMouseExited(e -> signUpLabel.setUnderline(false));
        signUpLabel.setOnMouseClicked(e -> this.onSignUp.run());

        signUpBox.getChildren().addAll(askLabel, signUpLabel);
        card.getChildren().addAll(title, subtitle, usernameField, passwordField, loginBtn, signUpBox);
        this.getChildren().add(card);
    }

    private TextField createCustomTextField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        styleInput(tf);
        return tf;
    }

    private PasswordField createCustomPasswordField(String prompt) {
        PasswordField pf = new PasswordField();
        pf.setPromptText(prompt);
        styleInput(pf);
        return pf;
    }

    private void styleInput(TextField input) {
        input.setPadding(new Insets(12, 15, 12, 15));
        input.setFont(Font.font(14));

        Background normalBg = new Background(new BackgroundFill(Color.web("#fafafa"), new CornerRadii(8), Insets.EMPTY));
        Background focusBg = new Background(new BackgroundFill(Color.WHITE, new CornerRadii(8), Insets.EMPTY));

        Border normalBorder = new Border(new BorderStroke(Color.web("#cccccc"), BorderStrokeStyle.SOLID, new CornerRadii(8), new BorderWidths(1)));
        Border focusBorder = new Border(new BorderStroke(Color.web("#4facfe"), BorderStrokeStyle.SOLID, new CornerRadii(8), new BorderWidths(1)));

        input.setBackground(normalBg);
        input.setBorder(normalBorder);

        input.focusedProperty().addListener((obs, oldVal, isFocused) -> {
            input.setBorder(isFocused ? focusBorder : normalBorder);
            input.setBackground(isFocused ? focusBg : normalBg);
        });
    }

    private Button createCustomButton(String text) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPadding(new Insets(12, 20, 12, 20));
        btn.setTextFill(Color.WHITE);
        btn.setFont(Font.font("System", FontWeight.BOLD, 16));
        btn.setCursor(Cursor.HAND);

        Stop[] btnStopsNormal = new Stop[]{new Stop(0, Color.web("#4facfe")), new Stop(1, Color.web("#00f2fe"))};
        Stop[] btnStopsHover = new Stop[]{new Stop(0, Color.web("#00f2fe")), new Stop(1, Color.web("#4facfe"))};

        Background normalBg = new Background(new BackgroundFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE, btnStopsNormal), new CornerRadii(8), Insets.EMPTY));
        Background hoverBg = new Background(new BackgroundFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE, btnStopsHover), new CornerRadii(8), Insets.EMPTY));

        btn.setBackground(normalBg);
        btn.setOnMouseEntered(e -> btn.setBackground(hoverBg));
        btn.setOnMouseExited(e -> btn.setBackground(normalBg));

        return btn;
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}