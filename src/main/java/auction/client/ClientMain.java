package auction.client;

import auction.client.ui.LoginView;
import auction.client.ui.SignUpView;
import auction.client.ui.AuctionUI;
import auction.client.ui.LoginSuccessHandler;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * ClientMain - Điểm khởi động của ứng dụng client
 */
public class ClientMain extends Application {

    private static Stage window;
    private static String currentUsername = null;

    // =========================================================================
    // ĐÃ THÊM: Biến static và hàm Getter/Setter để lưu trữ Token toàn cục
    // =========================================================================
    private static String jwtToken = null;

    public static String getJwtToken() {
        return jwtToken;
    }

    public static void setJwtToken(String token) {
        jwtToken = token;
    }
    // =========================================================================

    @Override
    public void start(Stage primaryStage) throws Exception {
        window = primaryStage;

        window.setTitle("Auction App - Login");
        window.setMinWidth(600);
        window.setMinHeight(500);

        LoginView loginRoot = new LoginView(
                (username, role, token) -> showAuctionScreen(username, role, token),
                this::showSignUpScreen
        );
        Scene loginScene = new Scene(loginRoot, 800, 600);

        window.setScene(loginScene);
        window.show();
    }

    /**
     * Chuyển sang màn hình Auction và truyền username, role cùng với token xác thực
     */
    public void showAuctionScreen(String username, String role, String token) {
        try {
            currentUsername = username;

            // LƯU TOKEN VÀO BIẾN TOÀN CỤC NGAY KHI ĐĂNG NHẬP THÀNH CÔNG
            ClientMain.setJwtToken(token);

            // Tải file FXML giao diện chính
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/auction/client/ui/AuctionUI.fxml"));
            Parent root = loader.load();

            // Lấy controller của AuctionUI để truyền dữ liệu người dùng
            AuctionUI controller = loader.getController();
            if (controller != null && username != null) {
                controller.initializeWithUser(username, role);
                controller.setJwtToken(token);
            }

            Scene auctionScene = new Scene(root, 940, 650);

            window.setScene(auctionScene);
            window.setTitle("Hệ thống Đấu giá trực tuyến");
            window.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Không thể tải màn hình Auction: " + e.getMessage());
        }
    }

    /**
     * Chuyển sang màn hình Sign Up
     */
    public void showSignUpScreen() {
        try {
            SignUpView signUpRoot = new SignUpView((username, role) -> showAuctionScreen(username, role, null), this::showLoginScreen);
            Scene signUpScene = new Scene(signUpRoot, 800, 600);

            window.setScene(signUpScene);
            window.setTitle("Auction App - Sign Up");
            window.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Không thể tải màn hình Sign Up: " + e.getMessage());
        }
    }

    /**
     * Chuyển lại màn hình Login
     */
    public void showLoginScreen() {
        try {
            LoginView loginRoot = new LoginView(
                    (username, role, token) -> showAuctionScreen(username, role, token),
                    this::showSignUpScreen
            );
            Scene loginScene = new Scene(loginRoot, 800, 600);

            window.setScene(loginScene);
            window.setTitle("Auction App - Login");
            window.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Không thể tải màn hình Login: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}