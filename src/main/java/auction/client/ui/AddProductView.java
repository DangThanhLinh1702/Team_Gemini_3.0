package auction.client.ui;

import auction.client.network.HttpClientUtil;
import auction.shared.dto.ItemDTO;
import auction.shared.util.JsonUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class AddProductView extends VBox {

    public AddProductView(String sellerUsername, Runnable onBack) {
        this.setSpacing(15);
        this.setPadding(new Insets(40));
        this.setAlignment(Pos.TOP_CENTER);

        Label title = new Label("Đăng Sản Phẩm Mới");
        title.setFont(Font.font("System", FontWeight.BOLD, 24));

        TextField txtName = new TextField();
        txtName.setPromptText("Tên sản phẩm (VD: Đồng hồ Rolex)");

        TextArea txtDescription = new TextArea();
        txtDescription.setPromptText("Mô tả chi tiết...");
        txtDescription.setPrefRowCount(4);

        TextField txtStartingPrice = new TextField();
        txtStartingPrice.setPromptText("Giá khởi điểm (VNĐ)");
        // Chỉ cho phép nhập số
        txtStartingPrice.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                txtStartingPrice.setText(newVal.replaceAll("[^\\d]", ""));
            }
        });

        Button btnAdd = new Button("Đăng Sản Phẩm");
        btnAdd.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
        btnAdd.setMaxWidth(Double.MAX_VALUE);
        
        Button btnBack = new Button("Quay Lại");
        btnBack.setMaxWidth(Double.MAX_VALUE);

        // Xử lý khi bấm nút Đăng
        btnAdd.setOnAction(e -> {
            String name = txtName.getText().trim();
            String desc = txtDescription.getText().trim();
            String priceStr = txtStartingPrice.getText().trim();

            if (name.isEmpty() || priceStr.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng nhập tên và giá sản phẩm.");
                return;
            }

            try {
                double price = Double.parseDouble(priceStr);
                // 1. Gói dữ liệu vào DTO
                ItemDTO newItem = new ItemDTO(name, desc, price, sellerUsername);
                
                // 2. Chuyển thành JSON (dùng hàm JsonUtil nhóm bạn đã viết)
                String jsonRequest = JsonUtil.toJson(newItem);

                // 3. Gửi lên Server
                String responseJson = HttpClientUtil.sendPost("/items", jsonRequest);
                
                if (responseJson.contains("\"status\":\"success\"")) {
                    showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã đăng sản phẩm lên sảnh chờ!");
                    txtName.clear();
                    txtDescription.clear();
                    txtStartingPrice.clear();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Thất bại", "Lỗi từ server: " + responseJson);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", "Không thể kết nối tới Server!");
            }
        });

        // Xử lý khi bấm Quay lại
        btnBack.setOnAction(e -> onBack.run());

        this.getChildren().addAll(title, txtName, txtDescription, txtStartingPrice, btnAdd, btnBack);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}