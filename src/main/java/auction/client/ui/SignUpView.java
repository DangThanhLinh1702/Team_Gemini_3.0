package auction.client.ui;

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
import java.util.function.BiConsumer;

public class SignUpView extends StackPane {

    private final BiConsumer<String, String> onSignUpSuccess;
    private final Runnable onBackToLogin;

    public SignUpView(BiConsumer<String, String> onSignUpSuccess, Runnable onBackToLogin) {
        this.onSignUpSuccess = onSignUpSuccess;
        this.onBackToLogin = onBackToLogin;

        // 1. TẠO BACKGROUND GRADIENT
        Stop[] bgStops = new Stop[]{
                new Stop(0, Color.web("#4facfe")),
                new Stop(1, Color.web("#00f2fe"))
        };
        LinearGradient bgGradient = new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE, bgStops);
        this.setBackground(new Background(new BackgroundFill(bgGradient, CornerRadii.EMPTY, Insets.EMPTY)));

        // 2. TẠO KHUNG ĐĂNG KÝ (CARD)
        VBox card = new VBox(15);
        card.setMaxSize(400, 550);
        card.setPadding(new Insets(40));
        card.setAlignment(Pos.TOP_CENTER);

        card.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(15), Insets.EMPTY)));

        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.color(0, 0, 0, 0.25));
        shadow.setRadius(20);
        shadow.setOffsetY(10);
        card.setEffect(shadow);

        // 3. CÁC THÀNH PHẦN BÊN TRONG CARD
        Label title = new Label("Create Account");
        title.setFont(Font.font("System", FontWeight.BOLD, 28));
        title.setTextFill(Color.web("#333333"));

        Label subtitle = new Label("Join our auction platform");
        subtitle.setTextFill(Color.web("#777777"));
        VBox.setMargin(subtitle, new Insets(0, 0, 10, 0));

        TextField usernameField = createCustomTextField("Username");
        PasswordField passwordField = createCustomPasswordField("Password");
        PasswordField confirmPasswordField = createCustomPasswordField("Confirm Password");

        ComboBox<String> roleComboBox = new ComboBox<>();
        roleComboBox.getItems().addAll("Bidder", "Seller");
        roleComboBox.setValue("Bidder");
        roleComboBox.setMaxWidth(Double.MAX_VALUE);
        roleComboBox.setStyle("-fx-font-size: 14px; -fx-padding: 8; -fx-border-radius: 6; -fx-background-radius: 6; -fx-border-color: #3498db;");

        Button signUpBtn = createCustomButton("SIGN UP");

        // 🌟 THAY ĐỔI TẠI ĐÂY: Thêm luồng gọi API Đăng ký lên Server
        signUpBtn.setOnAction(e -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText();
            String confirmPassword = confirmPasswordField.getText();
            String role = roleComboBox.getValue();

            if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Lỗi", "Vui lòng nhập đầy đủ thông tin.");
                return;
            } else if (username.length() < 3) {
                showAlert(Alert.AlertType.WARNING, "Lỗi", "Username phải ít nhất 3 ký tự.");
                return;
            } else if (password.length() < 6) {
                showAlert(Alert.AlertType.WARNING, "Lỗi", "Mật khẩu phải ít nhất 6 ký tự.");
                return;
            } else if (!password.equals(confirmPassword)) {
                showAlert(Alert.AlertType.WARNING, "Lỗi", "Mật khẩu xác nhận không khớp.");
                return;
            }

            // Gửi dữ liệu Đăng ký lên Server
            new Thread(() -> {
                try {
                    String jsonBody = String.format("{\"username\":\"%s\", \"password\":\"%s\", \"role\":\"%s\"}", username, password, role);
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create("http://localhost:8080/register"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                            .build();

                    HttpClient client = HttpClient.newHttpClient();
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                    Platform.runLater(() -> {
                        if (response.statusCode() == 200 || response.statusCode() == 201) {
                            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Tạo tài khoản thành công! Bấm OK để tiếp tục");
                            if (this.onSignUpSuccess != null) {
                                this.onSignUpSuccess.accept(username, role);
                            }
                        } else {
                            showAlert(Alert.AlertType.ERROR, "Đăng ký thất bại", "Tài khoản đã tồn tại hoặc có lỗi từ máy chủ!");
                        }
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi Mạng", "Không thể kết nối đến Server!"));
                }
            }).start();
        });

        HBox backBox = new HBox(5);
        backBox.setAlignment(Pos.CENTER);
        VBox.setMargin(backBox, new Insets(10, 0, 0, 0));

        Label askLabel = new Label("Already have an account?");
        askLabel.setTextFill(Color.web("#777777"));

        Label backLabel = new Label("Log in");
        backLabel.setTextFill(Color.web("#4facfe"));
        backLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        backLabel.setCursor(Cursor.HAND);

        backLabel.setOnMouseEntered(e -> backLabel.setUnderline(true));
        backLabel.setOnMouseExited(e -> backLabel.setUnderline(false));
        backLabel.setOnMouseClicked(e -> {
            if (onBackToLogin != null) {
                onBackToLogin.run();
            }
        });

        backBox.getChildren().addAll(askLabel, backLabel);
        card.getChildren().addAll(title, subtitle, usernameField, passwordField, confirmPasswordField, roleComboBox, signUpBtn, backBox);
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

        Background normalBg = new Background(new BackgroundFill(
                new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE, btnStopsNormal),
                new CornerRadii(8), Insets.EMPTY));

        Background hoverBg = new Background(new BackgroundFill(
                new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE, btnStopsHover),
                new CornerRadii(8), Insets.EMPTY));

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