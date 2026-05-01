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
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.util.Duration;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URL;
import java.util.ResourceBundle;
import auction.shared.util.JwtUtil;

public class AuctionUI implements Initializable {

    @FXML private Label lblCurrentUser;
    @FXML private Label lblSelectedProduct;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblLeader;
    @FXML private Label lblCountdown;
    @FXML private Label lblNotification;
    @FXML private TextField txtBidAmount;
    @FXML private Button btnJoin;
    @FXML private Button btnBid;
    @FXML private Button btnPostItem;
    @FXML private Button btnChangeRole;
    @FXML private TextArea txtLog;

    @FXML private TableView<ProductItem> tableProducts;
    @FXML private TableColumn<ProductItem, String> colProductName;
    @FXML private TableColumn<ProductItem, String> colCurrentPrice;
    @FXML private TableColumn<ProductItem, String> colLeader;
    @FXML private TableColumn<ProductItem, String> colStatus;

    private AuctionController controller;
    private ObservableList<ProductItem> productList;
    private ProductItem selectedProduct;
    private String currentUsername;
    private String currentRole;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        productList = FXCollections.observableArrayList();
        setupTable();
        setupTableSelectionListener();
        startCountdownTimer();
        appendLog("Ứng dụng khởi động thành công.");
    }

    public void initializeWithUser(String username, String role) {
        this.currentUsername = username;
        this.currentRole = role.toUpperCase();
        controller = new AuctionController(this, username);
        controller.updateRole(this.currentRole);
        Platform.runLater(() -> {
            lblCurrentUser.setText(username + " (" + currentRole + ")");
            if (btnPostItem != null) {
                btnPostItem.setVisible("SELLER".equalsIgnoreCase(currentRole));
            }
        });
    }

    @FXML
    private void handleChangeRole() {
        ChoiceDialog<String> dialog = new ChoiceDialog<>(currentRole, "BIDDER", "SELLER", "ADMIN");
        dialog.setTitle("Đổi vai trò");
        dialog.setHeaderText("Chọn vai trò mới");
        dialog.setContentText("Vai trò:");
        dialog.showAndWait().ifPresent(newRole -> {
            if (newRole.equalsIgnoreCase(currentRole)) {
                showNotification("Vai trò mới trùng vai trò hiện tại", "error");
                return;
            }
            try {
                String token = JwtUtil.createToken(currentUsername, currentRole);
                String body = String.format("{\"role\":\"%s\"}", newRole.toUpperCase());
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/change-role"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + token)
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
                HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    currentRole = newRole.toUpperCase();
                    lblCurrentUser.setText(currentUsername + " (" + currentRole + ")");
                    btnPostItem.setVisible("SELLER".equalsIgnoreCase(currentRole));
                    controller.updateRole(currentRole);
                    showNotification("Đổi vai trò thành công", "success");
                } else {
                    showNotification("Đổi vai trò thất bại: " + response.body(), "error");
                }
            } catch (Exception e) {
                showNotification("Lỗi đổi vai trò: " + e.getMessage(), "error");
            }
        });
    }

    private void setupTable() {
        colProductName.setCellValueFactory(new PropertyValueFactory<>("productName"));
        colCurrentPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        colLeader.setCellValueFactory(new PropertyValueFactory<>("leader"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        tableProducts.setItems(productList);
    }

    private void setupTableSelectionListener() {
        tableProducts.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        selectedProduct = newSelection;
                        updateProductDetail(newSelection);
                    }
                }
        );
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

    public void clearTable() { Platform.runLater(() -> productList.clear()); }

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
            if (controller != null) controller.postNewItem(itemDTO);
        });
    }

    public void addProduct(ProductItem product) { Platform.runLater(() -> productList.add(product)); }

    public void updatePrice(String productId, long newPrice, String leader) {
        Platform.runLater(() -> {
            for (ProductItem item : productList) {
                if (item.getProductId().equals(productId)) {
                    item.setRawPrice(newPrice);
                    item.setCurrentPrice(String.format("%,d VNĐ", newPrice));
                    item.setLeader(leader);

                    if (selectedProduct != null && selectedProduct.getProductId().equals(productId)) {
                        updateProductDetail(item);
                    }
                    break;
                }
            }
            tableProducts.refresh();
        });
    }

    // ĐÃ SỬA: Thêm tham số productId, hiển thị cửa sổ nhỏ (Alert) khi kết thúc đấu giá
    public void showAuctionEnded(String productId, String winner, long price) {
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
            lblCurrentPrice.setText(product.getCurrentPrice());
            lblLeader.setText(product.getLeader());
        });
    }

    public void setCurrentUser(String user) { Platform.runLater(() -> lblCurrentUser.setText(user)); }
    public void enableBidButton() { btnBid.setDisable(false); }
    public void appendLog(String msg) { Platform.runLater(() -> txtLog.appendText("[LOG] " + msg + "\n")); }
    public void showNotification(String msg, String type) { Platform.runLater(() -> lblNotification.setText(msg)); }

    @FXML
    private void handleJoin() {
        if (selectedProduct != null) {
            // ĐÃ SỬA: Hiển thị cửa sổ nhỏ (Alert) thay vì Label
            if (selectedProduct.getSeller().equals(currentUsername)) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Cảnh báo");
                alert.setHeaderText(null);
                alert.setContentText("Bạn không thể tham gia đấu giá ở tài khoản Seller");
                alert.showAndWait();
                return;
            }
            if (controller != null) controller.joinAuction(selectedProduct.getProductId());
        }
    }

    @FXML
    private void handleBid() {
        try {
            long amt = Long.parseLong(txtBidAmount.getText());
            if (controller != null) controller.placeBid(selectedProduct.getProductId(), amt);
        } catch (Exception e) { showNotification("Giá không hợp lệ", "error"); }
    }
}
