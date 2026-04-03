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
    public void showAuctionScreen() {
        try {
            // Tải file FXML lên
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/auction/client/ui/AuctionUI.fxml")
            );

            Parent root = loader.load();

            // Cập nhật lại cửa sổ sang màn hình Auction
            window.setTitle("Auction App - Dashboard");
            window.setScene(new Scene(root, 900, 600));
            window.setMinWidth(700);
            window.setMinHeight(500);

        } catch (Exception e) {
            System.err.println("Lỗi khi tải màn hình Auction FXML!");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}