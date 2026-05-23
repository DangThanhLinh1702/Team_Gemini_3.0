package auction.client.ui;

import auction.client.controller.AuctionController;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.Base64;
import java.util.ResourceBundle;

public class AuctionUI implements Initializable {

    @FXML private Label lblCurrentUser;
    @FXML private Label lblSelectedProduct;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblLeader;
    @FXML private Label lblCountdown;
    @FXML private Label lblNotification;
    @FXML private TextField txtBidAmount;
    @FXML private Button btnBid;
    @FXML private Button btnPostItem;
    @FXML private Button btnRefresh;
    @FXML private Button btnChangeRole;
    @FXML private TextArea txtLog;

    @FXML private ScrollPane gridScrollPane;
    @FXML private FlowPane productGrid;
    @FXML private VBox detailPanel;
    @FXML private Label lblDetailName;
    @FXML private Label lblDetailSeller;
    @FXML private ListView<String> listBidHistory;

    // --- THÊM Ô CHỨA ẢNH CHI TIẾT (fx:id="imgDetailPreview") ---
    @FXML private ImageView imgDetailPreview;

    private AuctionController controller;
    private ObservableList<ProductItem> productList;
    private ProductItem selectedProduct;
    private String currentUsername;
    private String currentRole;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        productList = FXCollections.observableArrayList();
        startCountdownTimer();
        appendLog("Ứng dụng khởi động thành công.");
        btnBid.setDisable(true);
    }

    public void initializeWithUser(String username, String role) {
        this.currentUsername = username;
        this.currentRole = role;
        controller = new AuctionController(this, username);
        updateRoleUI();
        Platform.runLater(this::handleRefresh);
    }

    @FXML
    private void handleToggleRole() {
        if (currentRole == null) return;
        currentRole = "SELLER".equalsIgnoreCase(currentRole) ? "BIDDER" : "SELLER";
        updateRoleUI();
        appendLog("Đã đổi vai trò sang: " + currentRole);
        showNotification("Đã chuyển sang quyền: " + currentRole, "info");
    }

    private void updateRoleUI() {
        Platform.runLater(() -> {
            lblCurrentUser.setText(currentUsername + " (" + currentRole + ")");
            if (btnPostItem != null) btnPostItem.setVisible("SELLER".equalsIgnoreCase(currentRole));
            btnBid.setDisable("SELLER".equalsIgnoreCase(currentRole));
        });
    }

    private void startCountdownTimer() {
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            if (selectedProduct != null) {
                long timeRemaining = selectedProduct.getEndTime() - System.currentTimeMillis();
                if (timeRemaining > 0) {
                    long seconds = timeRemaining / 1000;
                    lblCountdown.setText(String.format("%02d:%02d", (seconds % 3600) / 60, seconds % 60));
                } else {
                    lblCountdown.setText("00:00 (Kết thúc)");
                    btnBid.setDisable(true);
                    selectedProduct.setStatus("Kết thúc");
                }
            } else {
                lblCountdown.setText("00:00");
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    public void clearTable() {
        Platform.runLater(() -> {
            productList.clear();
            productGrid.getChildren().clear();
        });
    }

    @FXML
    private void handlePostItem() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/auction/client/ui/PostItemDialog.fxml"));
            Parent root = loader.load();

            PostItemController postController = loader.getController();
            if (this.controller != null) {
                postController.setAuctionController(this.controller);
            } else {
                appendLog("Lỗi: Hệ thống mạng chưa được khởi tạo!");
                return;
            }

            Stage stage = new Stage();
            stage.setTitle("Đăng bán sản phẩm mới");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            appendLog("Lỗi mở form đăng bán: " + e.getMessage());
        }
    }

    @FXML
    private void handleRefresh() {
        this.selectedProduct = null;
        btnBid.setDisable(true);
        lblSelectedProduct.setText("(Chưa chọn sản phẩm)");
        lblCurrentPrice.setText("0 VNĐ");
        lblLeader.setText("---");
        lblCountdown.setText("00:00");
        if (controller != null) controller.fetchInitialProducts();
        appendLog("Đã làm mới danh sách.");
    }

    public void addProduct(ProductItem item) {
        productList.add(item);
        Platform.runLater(() -> productGrid.getChildren().add(createProductCard(item)));
    }

    @FXML
    private void handleBackToGrid() {
        this.selectedProduct = null;
        btnBid.setDisable(true);
        detailPanel.setVisible(false);
        gridScrollPane.setVisible(true);
    }

    public void updatePrice(String productId, double newPrice, String leader) {
        Platform.runLater(() -> {
            for (ProductItem item : productList) {
                if (item.getProductId().equals(productId)) {
                    item.setRawPrice((long) newPrice);
                    item.setCurrentPrice(String.format("%,.0f VNĐ", newPrice));
                    item.setLeader(leader);
                    break;
                }
            }
        });
    }

    public void showAuctionEnded(String productId, String winner, double price) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Kết thúc");
            alert.setContentText("Người thắng cuộc là " + winner);
            alert.showAndWait();
        });
    }

    // =====================================================================
    // SỬA HÀM NÀY: Thêm logic giải mã và hiển thị ảnh Base64
    // =====================================================================
    public void updateProductDetail(ProductItem product) {
        Platform.runLater(() -> {
            lblSelectedProduct.setText(product.getProductName());
            lblCurrentPrice.textProperty().unbind();
            lblCurrentPrice.textProperty().bind(product.currentPriceProperty());
            lblLeader.textProperty().unbind();
            lblLeader.textProperty().bind(product.leaderProperty());

            // --- ĐOẠN CODE MỚI ĐỂ HIỂN THỊ ẢNH TRONG CHI TIẾT ---
            if (imgDetailPreview != null) { // Kiểm tra xem ô chứa ảnh có tồn tại không
                if (product.getImageBase64() != null && !product.getImageBase64().isEmpty()) {
                    try {
                        // Giải mã Base64 thành byte array
                        byte[] imgBytes = Base64.getDecoder().decode(product.getImageBase64());
                        // Chuyển byte array thành Image JavaFX
                        Image img = new Image(new ByteArrayInputStream(imgBytes));
                        imgDetailPreview.setImage(img); // Thiết lập ảnh cho ô chứa
                    } catch (Exception e) {
                        System.err.println("Lỗi giải mã ảnh chi tiết: " + e.getMessage());
                        // Nếu lỗi, có thể đặt một ảnh mặc định hoặc xóa ảnh cũ
                        imgDetailPreview.setImage(null);
                    }
                } else {
                    // Không có dữ liệu ảnh, xóa ảnh cũ (nếu có)
                    imgDetailPreview.setImage(null);
                }
            }
            // ----------------------------------------------------
        });
    }

    @FXML
    private void handleBid() {
        if (selectedProduct == null) return;
        try {
            long amt = Long.parseLong(txtBidAmount.getText());
            if (amt > selectedProduct.getRawPrice() && controller != null) {
                controller.placeBid(selectedProduct.getProductId(), amt);
            } else {
                showNotification("Giá đặt phải lớn hơn giá hiện tại!", "error");
            }
        } catch (Exception e) {
            showNotification("Giá không hợp lệ", "error");
        }
    }

    public void setCurrentUser(String user) {
        Platform.runLater(() -> lblCurrentUser.setText(user));
    }

    public void enableBidButton() {
        btnBid.setDisable(false);
    }

    public void appendLog(String msg) {
        Platform.runLater(() -> {
            if (txtLog != null) {
                txtLog.appendText("[LOG] " + msg + "\n");
            }
        });
    }

    public void showNotification(String msg, String type) {
        Platform.runLater(() -> {
            if (lblNotification != null) {
                lblNotification.setText(msg);
            }
        });
    }

    private javafx.scene.Node createProductCard(ProductItem item) {
        VBox card = new VBox(5);
        card.setPrefSize(180, 240); // Tăng chiều cao một chút cho ảnh đẹp
        card.setStyle("-fx-background-color: white; -fx-border-color: #bdc3c7; -fx-border-radius: 8; -fx-padding: 10; -fx-cursor: hand;");

        StackPane imgBox = new StackPane();
        imgBox.setPrefSize(160, 120);
        imgBox.setStyle("-fx-background-color: #ecf0f1; -fx-background-radius: 5;");

        if (item.getImageBase64() != null && !item.getImageBase64().isEmpty()) {
            try {
                // Giải mã Base64 thành Byte
                byte[] imgBytes = Base64.getDecoder().decode(item.getImageBase64());
                // Chuyển Byte thành Image JavaFX
                Image img = new Image(new ByteArrayInputStream(imgBytes));
                ImageView imgView = new ImageView(img);

                // Căn chỉnh ảnh cho đẹp
                imgView.setFitWidth(150);
                imgView.setFitHeight(110);
                imgView.setPreserveRatio(true); // Giữ đúng tỉ lệ ảnh, không bị méo
                imgBox.getChildren().add(imgView);
            } catch (Exception e) {
                imgBox.getChildren().add(new Label("Lỗi ảnh"));
            }
        } else {
            imgBox.getChildren().add(new Label("Không có ảnh"));
        }

        Label nameLbl = new Label(item.getProductName());
        nameLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label priceLbl = new Label();
        priceLbl.textProperty().bind(item.currentPriceProperty());
        priceLbl.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");

        card.getChildren().addAll(imgBox, nameLbl, priceLbl);

        card.setOnMouseClicked(e -> {
            this.selectedProduct = item;
            updateProductDetail(item);
            if (controller != null) controller.joinAuction(item.getProductId());
            if (lblDetailName != null) lblDetailName.setText("Tên: " + item.getProductName());
            if (lblDetailSeller != null) lblDetailSeller.setText("Người bán: " + item.getSeller());
            if (gridScrollPane != null && detailPanel != null) {
                gridScrollPane.setVisible(false);
                detailPanel.setVisible(true);
            }
        });
        return card;
    }
}