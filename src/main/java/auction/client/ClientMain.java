package auction.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * ClientMain - Điểm khởi động của ứng dụng client
 *
 * Cách chạy: Click chuột phải vào file này → Run 'ClientMain.main()'
 */
public class ClientMain extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {

        // Tải file FXML lên
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/auction/client/ui/AuctionUI.fxml")
        );

        Parent root = loader.load();

        // Cài đặt cửa sổ
        primaryStage.setTitle("Auction App");
        primaryStage.setScene(new Scene(root, 900, 600));
        primaryStage.setMinWidth(700);
        primaryStage.setMinHeight(500);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}