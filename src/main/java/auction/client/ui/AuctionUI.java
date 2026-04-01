package auction.client.ui;

import auction.client.controller.AuctionController;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * AuctionUI - Controller gắn với file AuctionUI.fxml
 *
 * Nhiệm vụ:
 *  - Hiển thị danh sách sản phẩm đấu giá
 *  - Cập nhật giá, người dẫn đầu, đếm ngược
 *  - Nhận input từ người dùng và chuyển sang AuctionController xử lý
 */
public class AuctionUI implements Initializable {

    // ==================== FXML Components ====================

    @FXML private Label lblCurrentUser;          // Tên người dùng hiện tại
    @FXML private Label lblSelectedProduct;      // Tên sản phẩm đang chọn
    @FXML private Label lblCurrentPrice;         // Giá hiện tại của sản phẩm
    @FXML private Label lblLeader;               // Người đang dẫn đầu
    @FXML private Label lblCountdown;            // Đồng hồ đếm ngược
    @FXML private Label lblNotification;         // Thông báo lỗi / trạng thái

    @FXML private TextField txtBidAmount;        // Ô nhập giá bid

    @FXML private Button btnJoin;                // Nút Tham gia
    @FXML private Button btnBid;                 // Nút Đặt giá

    @FXML private TextArea txtLog;               // Khu vực log hoạt động

    // TableView sản phẩm
    @FXML private TableView<ProductItem> tableProducts;
    @FXML private TableColumn<ProductItem, String> colProductName;
    @FXML private TableColumn<ProductItem, String> colCurrentPrice;
    @FXML private TableColumn<ProductItem, String> colLeader;
    @FXML private TableColumn<ProductItem, String> colStatus;

    // ==================== Biến nội bộ ====================

    private AuctionController controller;                        // Controller xử lý logic
    private ObservableList<ProductItem> productList;             // Dữ liệu bảng sản phẩm
    private ProductItem selectedProduct;                         // Sản phẩm đang được chọn

    // ==================== Khởi tạo ====================

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Khởi tạo controller logic
        controller = new AuctionController(this);

        // Khởi tạo danh sách sản phẩm
        productList = FXCollections.observableArrayList();

        // Cài đặt bảng sản phẩm
        setupTable();

        // Lắng nghe khi người dùng chọn sản phẩm trong bảng
        setupTableSelectionListener();

        // Chỉ cho nhập số trong ô bid
        setupBidInputValidator();

        appendLog("Ứng dụng khởi động. Vui lòng chọn sản phẩm để bắt đầu.");
    }

    // ==================== Setup ====================

    /**
     * Gắn các cột bảng với thuộc tính của ProductItem
     */
    private void setupTable() {
        colProductName.setCellValueFactory(new PropertyValueFactory<>("productName"));
        colCurrentPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        colLeader.setCellValueFactory(new PropertyValueFactory<>("leader"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        tableProducts.setItems(productList);
    }

    /**
     * Khi người dùng click chọn sản phẩm trên bảng → cập nhật panel bên phải
     */
    private void setupTableSelectionListener() {
        tableProducts.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        selectedProduct = newSelection;
                        updateProductDetail(newSelection);
                        appendLog("Đã chọn sản phẩm: " + newSelection.getProductName());
                    }
                }
        );
    }

    /**
     * Chỉ cho phép nhập số vào ô giá bid
     */
    private void setupBidInputValidator() {
        txtBidAmount.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                txtBidAmount.setText(newVal.replaceAll("[^\\d]", ""));
            }
        });
    }

    // ==================== Xử lý sự kiện từ FXML ====================

    /**
     * Xử lý nút "Tham gia đấu giá"
     * Gọi controller để xử lý logic tham gia
     */
    @FXML
    private void handleJoin() {
        if (selectedProduct == null) {
            showNotification("⚠️ Vui lòng chọn sản phẩm trước!", "warning");
            return;
        }
        controller.joinAuction(selectedProduct.getProductId());
    }

    /**
     * Xử lý nút "Đặt giá"
     * Lấy giá từ ô nhập → gọi controller kiểm tra và xử lý
     */
    @FXML
    private void handleBid() {
        String input = txtBidAmount.getText().trim();

        if (input.isEmpty()) {
            showNotification("⚠️ Vui lòng nhập số tiền muốn đặt!", "warning");
            return;
        }

        long bidAmount = Long.parseLong(input);
        controller.placeBid(selectedProduct.getProductId(), bidAmount);

        // Xóa ô nhập sau khi đặt giá
        txtBidAmount.clear();
    }

    // ==================== Các phương thức cập nhật UI ====================
    // Được gọi từ AuctionController để cập nhật giao diện
    // Luôn dùng Platform.runLater() để đảm bảo chạy trên JavaFX thread

    /**
     * Cập nhật tên người dùng hiện tại trên header
     */
    public void setCurrentUser(String username) {
        Platform.runLater(() -> lblCurrentUser.setText(username));
    }

    /**
     * Cập nhật thông tin chi tiết của sản phẩm đang chọn (panel phải)
     */
    public void updateProductDetail(ProductItem product) {
        Platform.runLater(() -> {
            lblSelectedProduct.setText(product.getProductName());
            lblCurrentPrice.setText(formatPrice(product.getRawPrice()) + " VNĐ");
            lblLeader.setText(product.getLeader().isEmpty() ? "---" : product.getLeader());
        });
    }

    /**
     * Cập nhật giá mới khi có người đặt giá
     *
     * @param productId ID sản phẩm
     * @param newPrice  Giá mới
     * @param leader    Người vừa đặt giá cao nhất
     */
    public void updatePrice(String productId, long newPrice, String leader) {
        Platform.runLater(() -> {
            // Cập nhật trong bảng
            for (ProductItem item : productList) {
                if (item.getProductId().equals(productId)) {
                    item.setRawPrice(newPrice);
                    item.setCurrentPrice(formatPrice(newPrice) + " VNĐ");
                    item.setLeader(leader);
                    break;
                }
            }
            tableProducts.refresh();

            // Nếu đây là sản phẩm đang được chọn → cập nhật panel phải
            if (selectedProduct != null && selectedProduct.getProductId().equals(productId)) {
                lblCurrentPrice.setText(formatPrice(newPrice) + " VNĐ");
                lblLeader.setText(leader);
            }

            appendLog(String.format("Giá mới: %s VNĐ — Người dẫn đầu: %s", formatPrice(newPrice), leader));
        });
    }

    /**
     * Cập nhật đồng hồ đếm ngược
     *
     * @param seconds Số giây còn lại
     */
    public void updateCountdown(int seconds) {
        Platform.runLater(() -> {
            int minutes = seconds / 60;
            int secs = seconds % 60;
            lblCountdown.setText(String.format("%02d:%02d", minutes, secs));

            // Đổi màu đỏ khi còn ít thời gian
            if (seconds <= 10) {
                lblCountdown.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");
            } else {
                lblCountdown.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #e67e22;");
            }
        });
    }

    /**
     * Thông báo phiên đấu giá kết thúc
     *
     * @param winner  Người thắng
     * @param price   Giá thắng
     */
    public void showAuctionEnded(String winner, long price) {
        Platform.runLater(() -> {
            lblCountdown.setText("KẾT THÚC");
            lblCountdown.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #8e44ad;");

            btnBid.setDisable(true);
            btnJoin.setDisable(true);

            String message = String.format("🏆 Đấu giá kết thúc!\nNgười thắng: %s\nGiá: %s VNĐ", winner, formatPrice(price));
            showNotification(message, "success");
            appendLog("=== Phiên đấu giá kết thúc. Người thắng: " + winner + " với giá " + formatPrice(price) + " VNĐ ===");

            // Hiện dialog thông báo
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Phiên đấu giá kết thúc");
            alert.setHeaderText("🏆 Kết quả đấu giá");
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * Kích hoạt nút "Đặt giá" sau khi người dùng đã tham gia
     */
    public void enableBidButton() {
        Platform.runLater(() -> {
            btnBid.setDisable(false);
            btnJoin.setDisable(true); // Đã tham gia rồi → ẩn nút Join
            appendLog("Bạn đã tham gia phiên đấu giá thành công.");
        });
    }

    /**
     * Thêm sản phẩm vào danh sách bảng
     */
    public void addProduct(ProductItem product) {
        Platform.runLater(() -> productList.add(product));
    }

    /**
     * Hiển thị thông báo lỗi hoặc trạng thái dưới panel đặt giá
     *
     * @param message Nội dung thông báo
     * @param type    "error" | "warning" | "success"
     */
    public void showNotification(String message, String type) {
        Platform.runLater(() -> {
            lblNotification.setText(message);
            switch (type) {
                case "error"   -> lblNotification.setStyle("-fx-font-size: 12px; -fx-text-fill: #e74c3c;");
                case "warning" -> lblNotification.setStyle("-fx-font-size: 12px; -fx-text-fill: #e67e22;");
                case "success" -> lblNotification.setStyle("-fx-font-size: 12px; -fx-text-fill: #27ae60;");
            }
        });
    }

    /**
     * Ghi log hoạt động xuống khu vực log phía dưới
     */
    public void appendLog(String message) {
        Platform.runLater(() -> {
            txtLog.appendText("[LOG] " + message + "\n");
        });
    }

    // ==================== Tiện ích ====================

    /**
     * Format số tiền thành dạng có dấu phẩy ngàn
     * Ví dụ: 1000000 → 1,000,000
     */
    private String formatPrice(long price) {
        return String.format("%,d", price);
    }
}
