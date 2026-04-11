package auction.client;

import auction.client.ui.LoginView;
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

    @Override
    public void start(Stage primaryStage) throws Exception {
        window = primaryStage;

        // Cài đặt chung cho cửa sổ
        window.setTitle("Auction App - Login");
        window.setMinWidth(600);
        window.setMinHeight(500);

        // 1. Tải màn hình Login đầu tiên
        // Truyền hàm showAuctionScreen vào LoginView để nó biết cần làm gì khi login thành công
        LoginView loginRoot = new LoginView(this::showAuctionScreen);
        Scene loginScene = new Scene(loginRoot, 800, 600);

        window.setScene(loginScene);
        window.show();
    }

    /**
     * Hàm này chứa code cũ của bạn, dùng để chuyển sang màn hình chính (FXML)
     */
    // Trong ClientMain.java
    public void showAuctionScreen() {
        try {
            // Tải file FXML giao diện chính
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/auction/client/ui/AuctionUI.fxml"));
            Parent root = loader.load();

            // Lấy controller của AuctionUI để có thể truyền dữ liệu (nếu cần)
            // AuctionUI controller = loader.getController();

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

    public static void main(String[] args) {
        launch(args);
    }
}