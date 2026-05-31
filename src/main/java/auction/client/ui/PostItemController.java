package auction.client.ui;

import auction.client.controller.AuctionController;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.nio.file.Files;
import java.util.Base64;

public class PostItemController {

    @FXML private TextField txtName;
    @FXML private TextField txtDescription;
    @FXML private TextField txtPrice;
    @FXML private TextField txtDuration; // Ô nhập thời gian (giây)
    @FXML private ImageView imgPreview;

    private String base64Image = "";

    // Cầu nối để gọi lệnh gửi lên Server
    private AuctionController auctionController;

    // AuctionUI sẽ dùng hàm này để truyền quyền điều khiển mạng sang
    public void setAuctionController(AuctionController controller) {
        this.auctionController = controller;
    }

    @FXML
    private void handleChooseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        File selectedFile = fileChooser.showOpenDialog(null);

        if (selectedFile != null) {
            try {
                imgPreview.setImage(new Image(selectedFile.toURI().toString()));
                byte[] fileBytes = Files.readAllBytes(selectedFile.toPath());
                this.base64Image = Base64.getEncoder().encodeToString(fileBytes);
                System.out.println("Đã chọn ảnh thành công. Độ dài chuỗi: " + base64Image.length());
            } catch (Exception e) {
                System.err.println("Lỗi khi xử lý file ảnh: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleSubmit() {
        String name = txtName.getText().trim();
        String desc = txtDescription.getText().trim();
        String priceStr = txtPrice.getText().trim();
        String durationStr = txtDuration.getText().trim();

        if (name.isEmpty() || desc.isEmpty() || priceStr.isEmpty() || durationStr.isEmpty()) {
            System.out.println("Lỗi: Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        // Ảnh không bắt buộc - nếu không có thì gửi chuỗi rỗng
        try {
            double price = Double.parseDouble(priceStr);
            int durationInSeconds = Integer.parseInt(durationStr); // Đọc thời gian theo GIÂY

            if (auctionController != null) {
                // Đẩy dữ liệu sang controller để đóng gói gửi đi
                auctionController.postNewItem(name, desc, price, durationInSeconds, base64Image);
                System.out.println("Đã gửi yêu cầu đăng bán lên Server (Thời gian: " + durationInSeconds + " giây)!");

                // Gửi xong thì tự động đóng Popup
                handleCancel();
            } else {
                System.out.println("Lỗi: Chưa kết nối được với Server (AuctionController bị null)!");
            }

        } catch (NumberFormatException e) {
            System.out.println("Lỗi: Giá tiền và Thời gian phải là một số hợp lệ!");
        }
    }

    @FXML
    private void handleCancel() {
        Stage stage = (Stage) txtName.getScene().getWindow();
        stage.close();
    }
}