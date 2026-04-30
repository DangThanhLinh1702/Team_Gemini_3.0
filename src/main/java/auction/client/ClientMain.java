package auction.client;

import auction.client.ui.LoginView;
import auction.client.ui.SignUpView;
import auction.client.ui.AuctionUI;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * ClientMain - Điểm khởi động của ứng dụng client
 */
public class ClientMain extends Application {

    // Tạo một biến static để lưu trữ cửa sổ chính, giúp chuyển Scene dễ dàng hơn
    private static Stage window;
    // Lưu trữ username hiện tại
    private static String currentUsername = null;

    @Override
    public void start(Stage primaryStage) throws Exception {
        window = primaryStage;

        // Cài đặt chung cho cửa sổ
        window.setTitle("Auction App - Login");
        window.setMinWidth(600);
        window.setMinHeight(500);

        // 1. Tải màn hình Login đầu tiên
        // Truyền hàm showAuctionScreen vào LoginView để nó biết cần làm gì khi login thành công
        // Truyền hàm showSignUpScreen để nó biết cần làm gì khi click Sign up
        LoginView loginRoot = new LoginView((username, role) -> showAuctionScreen(username, role), this::showSignUpScreen);
        Scene loginScene = new Scene(loginRoot, 800, 600);

        window.setScene(loginScene);
        window.show();
    }

    /**
     * Chuyển sang màn hình Auction và truyền username và role
     */
    public void showAuctionScreen(String username, String role) {
        try {
            // Lưu username hiện tại
            currentUsername = username;

            // Tải file FXML giao diện chính
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/auction/client/ui/AuctionUI.fxml"));
            Parent root = loader.load();

            // Lấy controller của AuctionUI để truyền username
            AuctionUI controller = loader.getController();
            if (controller != null && username != null) {
                controller.initializeWithUser(username, role);
            }

            Scene auctionScene = new Scene(root, 900, 600);

            // Chuyển cảnh trên cửa sổ hiện tại (window)
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
            SignUpView signUpRoot = new SignUpView((username, role) -> showAuctionScreen(username, role), this::showLoginScreen);
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
            LoginView loginRoot = new LoginView((username, role) -> showAuctionScreen(username, role), this::showSignUpScreen);
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