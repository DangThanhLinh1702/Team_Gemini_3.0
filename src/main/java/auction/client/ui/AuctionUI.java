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
    @FXML private Button btnJoin;
    @FXML private Button btnBid;
    @FXML private Button btnPostItem;
    @FXML private Button btnRefresh; // Nút làm mới
    @FXML private Button btnChangeRole; // ĐÃ SỬA: Trùng khớp hoàn toàn với fx:id trong FXML
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
            if (controller != null) {
                controller.postNewItem(itemDTO);
            }
        });
    }

    @FXML
    public void handleRefresh() {
        if (controller != null) {
            controller.refreshData();
            showNotification("Đang làm mới danh sách...", "info");
        }
    }

    public void addProduct(ProductItem product) { Platform.runLater(() -> productList.add(product)); }

    public void updatePrice(String productId, double newPrice, String leader) {
        Platform.runLater(() -> {
            for (ProductItem item : productList) {
                if (item.getProductId().equals(productId)) {
                    item.setRawPrice((long) newPrice);
                    item.setCurrentPrice(String.format("%,.0f VNĐ", newPrice));
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
            if (selectedProduct.getSeller().equals(currentUsername) || "SELLER".equalsIgnoreCase(currentRole)) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Cảnh báo");
                alert.setHeaderText(null);
                alert.setContentText("Bạn không thể tham gia đấu giá khi đang ở vai trò Seller của sản phẩm này!");
                alert.showAndWait();
                return;
            }
            if (controller != null) controller.joinAuction(selectedProduct.getProductId());
        }
    }

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
}