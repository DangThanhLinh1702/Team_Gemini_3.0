package auction.client.ui;

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

public class LoginView extends StackPane {

    // Biến lưu trữ hành động chuyển trang khi đăng nhập thành công
    private final Runnable onLoginSuccess;

    public LoginView(Runnable onLoginSuccess) {
        this.onLoginSuccess = onLoginSuccess;

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

        // Bo góc nền trắng cho Card
        card.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(15), Insets.EMPTY)));

        // Đổ bóng (DropShadow)
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.color(0, 0, 0, 0.25));
        shadow.setRadius(20);
        shadow.setOffsetY(10);
        card.setEffect(shadow);

        // 3. CÁC THÀNH PHẦN BÊN TRONG CARD
        // Tiêu đề
        Label title = new Label("Welcome Back");
        title.setFont(Font.font("System", FontWeight.BOLD, 28));
        title.setTextFill(Color.web("#333333"));

        Label subtitle = new Label("Please login to your auction account");
        subtitle.setTextFill(Color.web("#777777"));
        VBox.setMargin(subtitle, new Insets(0, 0, 10, 0));

        // Ô nhập liệu
        TextField usernameField = createCustomTextField("Username");
        PasswordField passwordField = createCustomPasswordField("Password");

        // Nút Login
        Button loginBtn = createCustomButton("LOGIN");
        loginBtn.setOnAction(e -> {
            String user = usernameField.getText();
            String pass = passwordField.getText();

            // Kiểm tra nhập liệu
            if (user.isEmpty() || pass.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Lỗi", "Vui lòng nhập đầy đủ thông tin.");
            } else {
                // ĐĂNG NHẬP THÀNH CÔNG -> GỌI HÀM CHUYỂN CẢNH BÊN CLIENTMAIN
                if (this.onLoginSuccess != null) {
                    this.onLoginSuccess.run();
                }
            }
        });

        // Khu vực Đăng ký
        HBox signUpBox = new HBox(5);
        signUpBox.setAlignment(Pos.CENTER);
        VBox.setMargin(signUpBox, new Insets(15, 0, 0, 0));

        Label askLabel = new Label("Don't have an account?");
        askLabel.setTextFill(Color.web("#777777"));

        Label signUpLabel = new Label("Sign up");
        signUpLabel.setTextFill(Color.web("#4facfe"));
        signUpLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        signUpLabel.setCursor(Cursor.HAND);

        // Hiệu ứng hover cho chữ Sign up
        signUpLabel.setOnMouseEntered(e -> signUpLabel.setUnderline(true));
        signUpLabel.setOnMouseExited(e -> signUpLabel.setUnderline(false));
        signUpLabel.setOnMouseClicked(e -> showAlert(Alert.AlertType.INFORMATION, "Chuyển trang", "Chuyển sang màn Đăng Ký... (Chưa code)"));

        signUpBox.getChildren().addAll(askLabel, signUpLabel);

        // Thêm tất cả vào Card
        card.getChildren().addAll(title, subtitle, usernameField, passwordField, loginBtn, signUpBox);

        // Thêm Card vào giữa Màn hình chính (StackPane)
        this.getChildren().add(card);
    }

    // --- CÁC HÀM TIỆN ÍCH ĐỂ TẠO UI KHÔNG DÙNG CSS ---

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

    // Dùng chung một hàm để style cho cả TextField và PasswordField
    private void styleInput(TextField input) {
        input.setPadding(new Insets(12, 15, 12, 15));
        input.setFont(Font.font(14));

        Background normalBg = new Background(new BackgroundFill(Color.web("#fafafa"), new CornerRadii(8), Insets.EMPTY));
        Background focusBg = new Background(new BackgroundFill(Color.WHITE, new CornerRadii(8), Insets.EMPTY));

        Border normalBorder = new Border(new BorderStroke(Color.web("#cccccc"), BorderStrokeStyle.SOLID, new CornerRadii(8), new BorderWidths(1)));
        Border focusBorder = new Border(new BorderStroke(Color.web("#4facfe"), BorderStrokeStyle.SOLID, new CornerRadii(8), new BorderWidths(1)));

        input.setBackground(normalBg);
        input.setBorder(normalBorder);

        // Hiệu ứng đổi màu viền và nền khi click vào ô nhập (Focus)
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

        // Gradient cho trạng thái bình thường và trạng thái hover
        Stop[] btnStopsNormal = new Stop[]{new Stop(0, Color.web("#4facfe")), new Stop(1, Color.web("#00f2fe"))};
        Stop[] btnStopsHover = new Stop[]{new Stop(0, Color.web("#00f2fe")), new Stop(1, Color.web("#4facfe"))};

        Background normalBg = new Background(new BackgroundFill(
                new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE, btnStopsNormal),
                new CornerRadii(8), Insets.EMPTY));

        Background hoverBg = new Background(new BackgroundFill(
                new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE, btnStopsHover),
                new CornerRadii(8), Insets.EMPTY));

        btn.setBackground(normalBg);

        // Hiệu ứng đổi màu Gradient khi di chuột qua Nút
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