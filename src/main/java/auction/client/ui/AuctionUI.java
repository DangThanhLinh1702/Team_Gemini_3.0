package auction.client.ui;

import auction.client.controller.AuctionController;
import auction.shared.dto.ItemDTO;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.net.URL;
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

    private AuctionController controller;
    private ObservableList<ProductItem> productList;
    private ProductItem selectedProduct;
    private String currentUsername;
    private String currentRole;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        productList = FXCollections.observableArrayList();
        // Đã xóa bỏ hoàn toàn setupTable và setupTableSelectionListener
        startCountdownTimer();
        appendLog("Ứng dụng khởi động thành công.");
        btnBid.setDisable(true);

    }

    public void initializeWithUser(String username, String role) {
        this.currentUsername = username;
        this.currentRole = role;
        controller = new AuctionController(this, username);
        updateRoleUI();

        // Tự động tải danh sách sản phẩm từ database lên UI ngay khi vừa đăng nhập thành công
        Platform.runLater(this::handleRefresh);
    }

    @FXML
    private void handleToggleRole() {
        if (currentRole == null) return;

        if ("SELLER".equalsIgnoreCase(currentRole)) {
            currentRole = "BIDDER";
        } else {
            currentRole = "SELLER";
        }

        updateRoleUI();
        appendLog("Đã chủ động đổi vai trò sang: " + currentRole);
        showNotification("Đã chuyển sang quyền: " + currentRole, "info");
    }

    private void updateRoleUI() {
        Platform.runLater(() -> {
            lblCurrentUser.setText(currentUsername + " (" + currentRole + ")");

            if (btnPostItem != null) {
                btnPostItem.setVisible("SELLER".equalsIgnoreCase(currentRole));
            }

            if ("SELLER".equalsIgnoreCase(currentRole)) {
                btnBid.setDisable(true);
            } else {
                btnBid.setDisable(false);
            }
        });
    }

    private void startCountdownTimer() {
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            if (selectedProduct != null) {
                long timeRemaining = selectedProduct.getEndTime() - System.currentTimeMillis();
                if (timeRemaining > 0) {
                    long seconds = timeRemaining / 1000;
                    long m = (seconds % 3600) / 60;
                    long s = seconds % 60;
                    lblCountdown.setText(String.format("%02d:%02d", m, s));
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
            productGrid.getChildren().clear(); // Dọn dẹp các ô vuông cũ trên màn hình
        });
    }
    @FXML
    private void handlePostItem() {
        Dialog<ItemDTO> dialog = new Dialog<>();
        dialog.setTitle("Đăng sản phẩm mới");
        ButtonType postBtnType = new ButtonType("Đăng bán", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(postBtnType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20, 10, 10, 10));

        TextField name = new TextField();
        TextField desc = new TextField();
        TextField price = new TextField();

        grid.add(new Label("Tên SP:"), 0, 0); grid.add(name, 1, 0);
        grid.add(new Label("Mô tả:"), 0, 1); grid.add(desc, 1, 1);
        grid.add(new Label("Giá sàn:"), 0, 2); grid.add(price, 1, 2);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == postBtnType) {
                try { return new ItemDTO(name.getText(), desc.getText(), Double.parseDouble(price.getText()), currentUsername);
                } catch (Exception e) { return null; }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(itemDTO -> {
            if (controller != null) {
                controller.postNewItem(itemDTO);
            }
        });
    }

    @FXML
    private void handleRefresh() {
        // 1. Reset sản phẩm đang chọn về null để tránh lỗi đặt giá nhầm dữ liệu cũ
        this.selectedProduct = null;
        // 2. Khóa nút đặt giá lại ngay lập tức
        btnBid.setDisable(true);
        // 3. Xóa chữ hiển thị ở Panel bên phải về trạng thái ban đầu
        lblSelectedProduct.setText("(Chưa chọn sản phẩm)");
        lblCurrentPrice.setText("0 VNĐ");
        lblLeader.setText("---");
        lblCountdown.setText("00:00");
        // 4. Gọi API kéo dữ liệu mới từ Database về (Logic cũ của bạn)
        if (controller != null) {
            controller.fetchInitialProducts();
        }
        appendLog("Đã làm mới danh sách và hủy chọn sản phẩm.");
    }

    public void addProduct(ProductItem item) {
        productList.add(item);
        javafx.application.Platform.runLater(() -> {
            javafx.scene.Node card = createProductCard(item);
            productGrid.getChildren().add(card);
        });
    }

    @FXML
    private void handleBackToGrid() {
        this.selectedProduct = null; // Hủy chọn
        btnBid.setDisable(true);      // Khóa nút đặt giá
        detailPanel.setVisible(false);
        gridScrollPane.setVisible(true);
    }

    public void updatePrice(String productId, double newPrice, String leader) {
        Platform.runLater(() -> {
            for (ProductItem item : productList) {
                if (item.getProductId().equals(productId)) {
                    item.setRawPrice((long) newPrice);
                    // Chữ trên thẻ và chữ bên panel phải sẽ tự động cập nhật nhờ Binding
                    item.setCurrentPrice(String.format("%,.0f VNĐ", newPrice));
                    item.setLeader(leader);
                    break;
                }
            }
        });
    }

    public void showAuctionEnded(String productId, String winner, double price) {
        Platform.runLater(() -> {
            String productName = "Sản phẩm";
            for (ProductItem item : productList) {
                if (item.getProductId().equals(productId)) {
                    productName = item.getProductName();
                    item.setStatus("Kết thúc");
                    break;
                }
            }
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Thông báo kết thúc đấu giá");
            alert.setHeaderText(null);
            alert.setContentText("Phiên đấu giá " + productName + " kết thúc, người thắng cuộc là " + winner + ".");
            alert.showAndWait();
        });
    }

    public void updateProductDetail(ProductItem product) {
        Platform.runLater(() -> {
            lblSelectedProduct.setText(product.getProductName());

            // Phải gỡ liên kết (unbind) cũ trước khi gán dữ liệu mới để tránh lỗi RuntimeException
            lblCurrentPrice.textProperty().unbind();
            lblCurrentPrice.textProperty().bind(product.currentPriceProperty());

            lblLeader.textProperty().unbind();
            lblLeader.textProperty().bind(product.leaderProperty());
        });
    }

    public void setCurrentUser(String user) { Platform.runLater(() -> lblCurrentUser.setText(user)); }
    public void enableBidButton() { btnBid.setDisable(false); }
    public void appendLog(String msg) { Platform.runLater(() -> txtLog.appendText("[LOG] " + msg + "\n")); }
    public void showNotification(String msg, String type) { Platform.runLater(() -> lblNotification.setText(msg)); }


    @FXML
    private void handleBid() {
        if (selectedProduct == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Cảnh báo");
            alert.setHeaderText(null);
            alert.setContentText("Vui lòng chọn một sản phẩm từ danh sách trước khi đặt giá!");
            alert.showAndWait();
            return;
        }

        try {
            long amt = Long.parseLong(txtBidAmount.getText());

            if (amt <= selectedProduct.getRawPrice()) {
                showNotification("Giá đặt phải lớn hơn giá hiện tại!", "error");
                return;
            }

            if (controller != null) {
                controller.placeBid(selectedProduct.getProductId(), amt);
            }
        } catch (NumberFormatException e) {
            showNotification("Vui lòng nhập số tiền hợp lệ", "error");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private javafx.scene.Node createProductCard(ProductItem item) {
        VBox card = new VBox(5);
        card.setPrefSize(180, 220);
        card.setStyle("-fx-background-color: white; -fx-border-color: #bdc3c7; -fx-border-radius: 8; -fx-padding: 10; -fx-cursor: hand;");

        // Đổi màu khi di chuột vào (Hover effect)
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #f0f8ff; -fx-border-color: #3498db; -fx-border-radius: 8; -fx-padding: 10; -fx-cursor: hand;"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: white; -fx-border-color: #bdc3c7; -fx-border-radius: 8; -fx-padding: 10; -fx-cursor: hand;"));

        // Khung ảnh nhỏ giả lập
        StackPane imgBox = new StackPane();
        imgBox.setPrefSize(160, 120);
        imgBox.setStyle("-fx-background-color: #ecf0f1;");
        imgBox.getChildren().add(new Label("Ảnh"));

        // Tên và Giá (Dùng Data Binding để giá tự động nhảy)
        Label nameLbl = new Label(item.getProductName());
        nameLbl.setStyle("-fx-font-weight: bold;");
        nameLbl.setWrapText(true);

        Label priceLbl = new Label();
        priceLbl.textProperty().bind(item.currentPriceProperty());
        priceLbl.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");

        card.getChildren().addAll(imgBox, nameLbl, priceLbl);

        // click chuột vào thẻ
        card.setOnMouseClicked(e -> {
            // 1. Cập nhật Panel đặt giá (CỘT PHẢI)
            this.selectedProduct = item;
            updateProductDetail(item);

            // Mở khóa nút "Đặt giá" ngay khi click vào thẻ (thay thế chức năng của nút btnJoin cũ)
            if (!"SELLER".equalsIgnoreCase(currentRole) || !item.getSeller().equals(currentUsername)) {
                btnBid.setDisable(false);
            }

            // Gọi JOIN room qua WebSocket để Server biết mình đang xem sản phẩm này
            if (controller != null) {
                controller.joinAuction(item.getProductId());
                appendLog("Đã tham gia xem đấu giá: " + item.getProductName());
            }

            // 2. Chuyển CỘT TRÁI sang màn hình Chi Tiết (có ảnh bự, lịch sử)
            lblDetailName.setText("Tên: " + item.getProductName());
            lblDetailSeller.setText("Người bán: " + item.getSeller());

            gridScrollPane.setVisible(false);
            detailPanel.setVisible(true);
        });

        return card;
    }
}