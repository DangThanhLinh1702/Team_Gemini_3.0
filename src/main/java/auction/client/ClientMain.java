package auction.client;

import auction.client.ui.LoginView;
import auction.client.ui.SignUpView;
import auction.client.ui.AuctionUI;
import auction.client.ui.AddProductView; // Nhớ import cái này
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ClientMain extends Application {

    private static Stage window;
    private static String currentUsername = null;
    private static ClientMain instance; // Thêm instance để gọi các hàm non-static nếu cần

    @Override
    public void start(Stage primaryStage) throws Exception {
        window = primaryStage;
        instance = this; // Lưu instance của Application

        window.setTitle("Auction App - Login");
        window.setMinWidth(600);
        window.setMinHeight(500);

        showLoginScreen(); // Gọi hàm show login khi bắt đầu
        window.show();
    }

    // 1. Hàm showAuctionScreen (Đã đổi thành static)
    public static void showAuctionScreen(String username) {
        try {
            currentUsername = username;
            FXMLLoader loader = new FXMLLoader(ClientMain.class.getResource("/auction/client/ui/AuctionUI.fxml"));
            Parent root = loader.load();

            AuctionUI controller = loader.getController();
            if (controller != null && username != null) {
                controller.initializeWithUser(username, role);
            }

            Scene auctionScene = new Scene(root, 900, 600);
            window.setScene(auctionScene);
            window.setTitle("Hệ thống Đấu giá trực tuyến");
            window.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 2. Hàm showAddProductScreen (Đã đổi thành static)
    public static void showAddProductScreen(String username) {
        try {
            currentUsername = username;
            // Tạo view mới, khi nhấn Back thì quay lại màn hình Auction
            AddProductView addProductRoot = new AddProductView(currentUsername, () -> showAuctionScreen(currentUsername));

            Scene addProductScene = new Scene(addProductRoot, 600, 500);
            window.setScene(addProductScene);
            window.setTitle("Seller Dashboard - Đăng Sản Phẩm");
            window.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showSignUpScreen() {
        try {
            // Lưu ý: vì các hàm trên là static, ta dùng ClientMain::showAuctionScreen
            SignUpView signUpRoot = new SignUpView(ClientMain::showAuctionScreen, ClientMain::showLoginScreen);
            Scene signUpScene = new Scene(signUpRoot, 800, 600);
            window.setScene(signUpScene);
            window.setTitle("Auction App - Sign Up");
            window.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showLoginScreen() {
        try {
            LoginView loginRoot = new LoginView(ClientMain::showAuctionScreen, ClientMain::showSignUpScreen);
            Scene loginScene = new Scene(loginRoot, 800, 600);
            window.setScene(loginScene);
            window.setTitle("Auction App - Login");
            window.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}