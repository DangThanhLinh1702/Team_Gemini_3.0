package auction.client.ui;

import auction.client.controller.AuctionController;
import auction.client.network.AdminItemClient;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.ButtonBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class AuctionUI implements Initializable {

    // ── Header ──────────────────────────────────────────────────────────────
    @FXML private Label lblCurrentUser;

    // ── Sidebar: thông tin đấu giá ──────────────────────────────────────────
    @FXML private Label lblSelectedProduct;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblLeader;
    @FXML private Label lblCountdown;
    @FXML private Label lblNotification;
    @FXML private TextField txtBidAmount;
    @FXML private Button btnBid;

    // ── Header buttons ───────────────────────────────────────────────────────
    @FXML private Button btnPostItem;
    @FXML private Button btnRefresh;
    @FXML private Button btnChangeRole;
    @FXML private Button btnManageUsers;

    // ── Log ──────────────────────────────────────────────────────────────────
    @FXML private TextArea txtLog;

    // ── Grid / Detail panels ─────────────────────────────────────────────────
    @FXML private ScrollPane gridScrollPane;
    @FXML private TilePane productGrid;
    @FXML private VBox detailPanel;
    @FXML private Label lblDetailName;
    @FXML private Label lblDetailSeller;
    @FXML private ListView<String> listBidHistory;   // ← lịch sử đấu giá
    @FXML private ImageView imgDetailPreview;

    // ── State ────────────────────────────────────────────────────────────────
    private AuctionController controller;
    private ObservableList<ProductItem> productList;
    private ProductItem selectedProduct;
    private String currentUsername;
    private String currentRole;
    private String jwtToken;
    private Runnable onLogout;

    public void setJwtToken(String token) { this.jwtToken = token; }
    public void setOnLogout(Runnable onLogout) { this.onLogout = onLogout; }

    // ────────────────────────────────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        productList = FXCollections.observableArrayList();
        startCountdownTimer();
        appendLog("Ứng dụng khởi động thành công.");
        btnBid.setDisable(true);
        // Placeholder cho listBidHistory khi chưa có dữ liệu
        if (listBidHistory != null) {
            listBidHistory.setPlaceholder(new Label("(Chưa có lịch sử đặt giá)"));
        }
    }

    public void initializeWithUser(String username, String role) {
        this.currentUsername = username;
        this.currentRole = role;
        controller = new AuctionController(this, username);
        if (jwtToken != null) controller.setJwtToken(jwtToken);
        updateRoleUI();
        Platform.runLater(this::handleRefresh);
    }

    // ── Countdown timer ──────────────────────────────────────────────────────
    private void startCountdownTimer() {
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            if (selectedProduct != null) {
                long timeRemaining = selectedProduct.getEndTime() - System.currentTimeMillis();
                if (timeRemaining > 0) {
                    long seconds = timeRemaining / 1000;
                    lblCountdown.setText(String.format("%02d:%02d",
                            (seconds % 3600) / 60, seconds % 60));
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

    // ── Nhận danh sách ban đầu ───────────────────────────────────────────────
    public void onInitialItemsReceived(List<Map<String, Object>> items) {
        clearTable();
        for (Map<String, Object> item : items) {
            try {
                String idRaw = String.valueOf(item.get("id"));
                String id = idRaw.endsWith(".0") ? idRaw.substring(0, idRaw.length() - 2) : idRaw;
                String name = (String) item.get("name");
                long price = (long) Double.parseDouble(String.valueOf(item.get("startingPrice")));
                String seller = (String) item.get("seller");
                long endTime = (long) Double.parseDouble(String.valueOf(item.get("endTime")));
                String imageBase64 = item.containsKey("image") && item.get("image") != null
                        ? (String) item.get("image") : "";
                String leader = item.containsKey("highestBidder") && item.get("highestBidder") != null
                        ? (String) item.get("highestBidder") : "---";

                ProductItem productItem = new ProductItem(id, name, price, leader, "Đang đấu", seller, endTime);
                productItem.setImageBase64(imageBase64);
                addProduct(productItem);
            } catch (Exception e) {
                System.err.println("Lỗi parse sản phẩm: " + e.getMessage());
            }
        }
    }

    // ── Sản phẩm mới được đăng ──────────────────────────────────────────────
    public void onNewItemAdded(String itemId, String name, double price,
                               String seller, long endTime, String imageBase64) {
        ProductItem newItem = new ProductItem(itemId, name, (long) price,
                "---", "Đang đấu", seller, endTime);
        newItem.setImageBase64(imageBase64);
        addProduct(newItem);
        showNotification("✨ Sản phẩm mới: " + name, "success");
        appendLog("Sản phẩm mới: " + name + " (ID=" + itemId + ")");
    }

    // ── Cập nhật giá trên card grid + sidebar ────────────────────────────────
    public void updatePrice(String productId, double newPrice, String leader) {
        for (ProductItem item : productList) {
            if (item.getProductId().equals(productId)) {
                // setRawPrice() tự update currentPriceProperty → card tự refresh qua binding
                item.setRawPrice((long) newPrice);
                item.setLeader(leader != null ? leader : "---");

                // Nếu đang xem detail của sản phẩm này → force update sidebar trực tiếp
                if (selectedProduct != null && selectedProduct.getProductId().equals(productId)) {
                    // Unbind → setText trực tiếp → rebind
                    // (đảm bảo label hiển thị giá trị mới ngay lập tức, tránh binding stale)
                    lblCurrentPrice.textProperty().unbind();
                    lblCurrentPrice.setText(ProductItem.formatPrice((long) newPrice));
                    lblCurrentPrice.textProperty().bind(item.currentPriceProperty());

                    lblLeader.textProperty().unbind();
                    lblLeader.setText(leader != null ? leader : "---");
                    lblLeader.textProperty().bind(item.leaderProperty());

                    // Cập nhật selectedProduct reference để trỏ đúng object trong list
                    selectedProduct = item;
                }
                break;
            }
        }
    }

    /**
     * Cập nhật listBidHistory trong detail panel.
     * Chỉ hiển thị nếu đang xem sản phẩm có itemId khớp.
     */
    public void updateBidHistory(String itemId, List<String> history) {
        if (listBidHistory == null) return;
        if (selectedProduct == null || !selectedProduct.getProductId().equals(itemId)) return;
        listBidHistory.getItems().setAll(history);
        // Cuộn xuống mục mới nhất
        if (!history.isEmpty()) {
            listBidHistory.scrollTo(history.size() - 1);
        }
    }

    /** Disable nút đấu giá khi phiên kết thúc */
    public void markAuctionFinished(String itemId) {
        if (selectedProduct != null && selectedProduct.getProductId().equals(itemId)) {
            btnBid.setDisable(true);
            lblCountdown.setText("00:00 (Kết thúc)");
            appendLog("Phiên đấu giá sản phẩm ID=" + itemId + " đã kết thúc.");
        }
    }

    public void showAuctionEnded(String productId, String winner, double price) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("🏆 Kết thúc phiên đấu giá");
        alert.setHeaderText("Phiên đấu giá đã kết thúc!");
        alert.setContentText(String.format(
                "🥇 Người thắng: %s\n💰 Giá cuối: %,.0f VNĐ", winner, price));
        alert.showAndWait();
    }

    // ── Thêm sản phẩm vào grid ───────────────────────────────────────────────
    public void addProduct(ProductItem item) {
        productList.add(item);
        Platform.runLater(() -> productGrid.getChildren().add(createProductCard(item)));
    }

    public void clearTable() {
        Platform.runLater(() -> {
            productList.clear();
            productGrid.getChildren().clear();
        });
    }

    // ── Detail panel ─────────────────────────────────────────────────────────
    private void showDetailPanel(ProductItem item) {
        // Tìm item trong productList để đảm bảo bind đúng object được cập nhật
        ProductItem liveItem = productList.stream()
                .filter(p -> p.getProductId().equals(item.getProductId()))
                .findFirst().orElse(item);
        selectedProduct = liveItem;

        // Reset listBidHistory khi mở panel mới
        if (listBidHistory != null) {
            listBidHistory.getItems().clear();
        }

        updateProductDetail(liveItem);
        if (lblDetailName   != null) lblDetailName.setText("Tên: " + liveItem.getProductName());
        if (lblDetailSeller != null) lblDetailSeller.setText("Người bán: " + liveItem.getSeller());

        if (gridScrollPane != null) gridScrollPane.setVisible(false);
        if (detailPanel    != null) detailPanel.setVisible(true);

        // JOIN phòng → server sẽ gửi SESSION_STATE với lịch sử đầy đủ
        if (controller != null) controller.joinAuction(liveItem.getProductId());

        // Enable bid nếu thời gian còn và không phải SELLER/ADMIN
        long timeLeft = liveItem.getEndTime() - System.currentTimeMillis();
        if (timeLeft > 0 && !"SELLER".equalsIgnoreCase(currentRole)
                && !"ADMIN".equalsIgnoreCase(currentRole)) {
            btnBid.setDisable(false);
        }
    }

    public void updateProductDetail(ProductItem product) {
        lblSelectedProduct.setText(product.getProductName());

        // Unbind → set trực tiếp → rebind để hiển thị ngay giá trị mới nhất
        lblCurrentPrice.textProperty().unbind();
        lblCurrentPrice.setText(product.getCurrentPrice());
        lblCurrentPrice.textProperty().bind(product.currentPriceProperty());

        lblLeader.textProperty().unbind();
        lblLeader.setText(product.getLeader());
        lblLeader.textProperty().bind(product.leaderProperty());

        // Ảnh
        if (imgDetailPreview != null) {
            if (product.getImageBase64() != null && !product.getImageBase64().isEmpty()) {
                try {
                    byte[] imgBytes = Base64.getDecoder().decode(product.getImageBase64());
                    imgDetailPreview.setImage(new Image(new ByteArrayInputStream(imgBytes)));
                } catch (Exception e) {
                    imgDetailPreview.setImage(null);
                }
            } else {
                imgDetailPreview.setImage(null);
            }
        }
    }

    @FXML
    private void handleBackToGrid() {
        selectedProduct = null;
        btnBid.setDisable(true);
        if (listBidHistory != null) listBidHistory.getItems().clear();
        lblSelectedProduct.setText("(Chưa chọn sản phẩm)");
        lblCurrentPrice.textProperty().unbind();
        lblCurrentPrice.setText("0 VNĐ");
        lblLeader.textProperty().unbind();
        lblLeader.setText("---");
        lblCountdown.setText("00:00");
        if (detailPanel    != null) detailPanel.setVisible(false);
        if (gridScrollPane != null) gridScrollPane.setVisible(true);
    }

    // ── Đặt giá ──────────────────────────────────────────────────────────────
    @FXML
    private void handleBid() {
        if (selectedProduct == null) {
            showNotification("Vui lòng chọn sản phẩm trước!", "error");
            return;
        }
        if (btnBid.isDisabled()) return;

        String inputText = txtBidAmount.getText().trim();
        if (inputText.isEmpty()) {
            showNotification("Vui lòng nhập số tiền muốn đặt!", "error");
            return;
        }
        try {
            // Chấp nhận số có dấu phẩy/chấm phân cách hàng nghìn
            long amt = Long.parseLong(inputText.replace(",", "").replace(".", ""));
            if (amt <= selectedProduct.getRawPrice()) {
                showNotification(String.format(
                                "❌ Giá phải lớn hơn %,.0f VNĐ!", (double) selectedProduct.getRawPrice()),
                        "error");
                return;
            }
            if (controller != null) {
                controller.placeBid(selectedProduct.getProductId(), amt);
                txtBidAmount.clear();
                appendLog(String.format("Bạn vừa đặt giá %,.0f VNĐ cho \"%s\"",
                        (double) amt, selectedProduct.getProductName()));
            }
        } catch (NumberFormatException e) {
            showNotification("❌ Giá không hợp lệ! Vui lòng nhập số.", "error");
        }
    }

    // ── Role ──────────────────────────────────────────────────────────────────
    @FXML
    private void handleToggleRole() {
        if (currentRole == null) return;
        if ("ADMIN".equalsIgnoreCase(currentRole)) {
            showNotification("Tài khoản ADMIN không thể chuyển vai trò.", "error");
            return;
        }
        currentRole = "SELLER".equalsIgnoreCase(currentRole) ? "BIDDER" : "SELLER";
        updateRoleUI();
        appendLog("Đã đổi vai trò sang: " + currentRole);
        showNotification("Đã chuyển sang quyền: " + currentRole, "info");
    }

    private void updateRoleUI() {
        Platform.runLater(() -> {
            lblCurrentUser.setText(currentUsername + " (" + currentRole + ")");
            if ("ADMIN".equalsIgnoreCase(currentRole)) {
                if (btnPostItem    != null) btnPostItem.setVisible(false);
                if (btnChangeRole  != null) btnChangeRole.setVisible(false);
                if (btnManageUsers != null) btnManageUsers.setVisible(true);
                btnBid.setDisable(true);
            } else {
                if (btnPostItem    != null) btnPostItem.setVisible("SELLER".equalsIgnoreCase(currentRole));
                if (btnChangeRole  != null) btnChangeRole.setVisible(true);
                if (btnManageUsers != null) btnManageUsers.setVisible(false);
                // Nếu đang ở detail panel và là BIDDER → enable bid
                if ("BIDDER".equalsIgnoreCase(currentRole) && selectedProduct != null) {
                    long timeLeft = selectedProduct.getEndTime() - System.currentTimeMillis();
                    btnBid.setDisable(timeLeft <= 0);
                } else {
                    btnBid.setDisable(true);
                }
            }
        });
    }

    // ── Refresh ───────────────────────────────────────────────────────────────
    @FXML
    private void handleRefresh() {
        selectedProduct = null;
        btnBid.setDisable(true);
        lblSelectedProduct.setText("(Chưa chọn sản phẩm)");
        lblCurrentPrice.textProperty().unbind();
        lblCurrentPrice.setText("0 VNĐ");
        lblLeader.textProperty().unbind();
        lblLeader.setText("---");
        lblCountdown.setText("00:00");
        if (detailPanel    != null) detailPanel.setVisible(false);
        if (gridScrollPane != null) gridScrollPane.setVisible(true);
        if (controller != null) controller.fetchInitialProducts();
        appendLog("Đã làm mới danh sách.");
    }

    // ── Post item ─────────────────────────────────────────────────────────────
    @FXML
    private void handlePostItem() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/auction/client/ui/PostItemDialog.fxml"));
            Parent root = loader.load();
            PostItemController postController = loader.getController();
            if (controller != null) {
                postController.setAuctionController(controller);
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

    // ── Product card ──────────────────────────────────────────────────────────
    private javafx.scene.Node createProductCard(ProductItem item) {
        VBox card = new VBox(5);
        card.setPrefSize(180, 240);
        card.setStyle("-fx-background-color: white; -fx-border-color: #bdc3c7; " +
                "-fx-border-radius: 8; -fx-padding: 10; -fx-cursor: hand;");

        // Ảnh sản phẩm
        StackPane imgBox = new StackPane();
        imgBox.setPrefSize(160, 120);
        imgBox.setStyle("-fx-background-color: #ecf0f1; -fx-background-radius: 5;");
        if (item.getImageBase64() != null && !item.getImageBase64().isEmpty()) {
            try {
                byte[] imgBytes = Base64.getDecoder().decode(item.getImageBase64());
                ImageView imgView = new ImageView(new Image(new ByteArrayInputStream(imgBytes)));
                imgView.setFitWidth(150);
                imgView.setFitHeight(110);
                imgView.setPreserveRatio(true);
                imgBox.getChildren().add(imgView);
            } catch (Exception e) {
                imgBox.getChildren().add(new Label("Lỗi ảnh"));
            }
        } else {
            Label noImg = new Label("📦");
            noImg.setStyle("-fx-font-size: 32px;");
            imgBox.getChildren().add(noImg);
        }

        Label nameLbl = new Label(item.getProductName());
        nameLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        nameLbl.setWrapText(true);

        Label priceLbl = new Label();
        priceLbl.textProperty().bind(item.currentPriceProperty());
        priceLbl.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");

        Label leaderLbl = new Label();
        leaderLbl.textProperty().bind(item.leaderProperty());
        leaderLbl.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 11px;");
        leaderLbl.setWrapText(true);

        card.getChildren().addAll(imgBox, nameLbl, priceLbl, leaderLbl);

        // Nút ADMIN
        if ("ADMIN".equalsIgnoreCase(currentRole)) {
            HBox adminBar = new HBox(5);
            adminBar.setAlignment(Pos.CENTER);
            Button btnEdit = new Button("✏ Sửa");
            btnEdit.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; " +
                    "-fx-font-size: 11px; -fx-background-radius: 4; -fx-cursor: hand;");
            btnEdit.setMaxWidth(Double.MAX_VALUE);
            Button btnDelete = new Button("🗑 Xóa");
            btnDelete.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; " +
                    "-fx-font-size: 11px; -fx-background-radius: 4; -fx-cursor: hand;");
            btnDelete.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(btnEdit, Priority.ALWAYS);
            HBox.setHgrow(btnDelete, Priority.ALWAYS);
            btnEdit.setOnAction(e -> { e.consume(); showEditDialog(item, card); });
            btnDelete.setOnAction(e -> { e.consume(); showDeleteConfirm(item, card); });
            adminBar.getChildren().addAll(btnEdit, btnDelete);
            card.getChildren().add(adminBar);
        }

        // Click vào card → mở detail
        card.setOnMouseClicked(e -> showDetailPanel(item));

        return card;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    public void enableBidButton() {
        if (!"SELLER".equalsIgnoreCase(currentRole) && !"ADMIN".equalsIgnoreCase(currentRole)) {
            btnBid.setDisable(false);
        }
    }

    public void appendLog(String msg) {
        Platform.runLater(() -> {
            if (txtLog != null) txtLog.appendText("[LOG] " + msg + "\n");
        });
    }

    public void showNotification(String msg, String type) {
        Platform.runLater(() -> {
            if (lblNotification != null) {
                String color = "error".equals(type) ? "#e74c3c"
                        : "success".equals(type) ? "#27ae60" : "#2980b9";
                lblNotification.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px;");
                lblNotification.setText(msg);
                // Auto-clear sau 4s
                new Timeline(new KeyFrame(Duration.seconds(4),
                        ev -> lblNotification.setText(""))).play();
            }
        });
    }

    public void setCurrentUser(String user) {
        Platform.runLater(() -> lblCurrentUser.setText(user));
    }

    // ── ADMIN dialogs ─────────────────────────────────────────────────────────
    private void showDeleteConfirm(ProductItem item, VBox card) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận xóa");
        confirm.setHeaderText("Bạn có thực sự muốn xóa sản phẩm này?");
        confirm.setContentText("\"" + item.getProductName() + "\"\n\nHành động này không thể hoàn tác.");
        ButtonType btnYes = new ButtonType("Xóa", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnNo  = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(btnYes, btnNo);
        confirm.getDialogPane().lookupButton(btnYes)
                .setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");
        confirm.showAndWait().ifPresent(result -> {
            if (result == btnYes) {
                if (controller == null) { showNotification("Lỗi: controller chưa sẵn sàng.", "error"); return; }
                new Thread(() -> {
                    AdminItemClient.Result res = controller.deleteItem(item.getProductId());
                    Platform.runLater(() -> {
                        if (res.success) {
                            productGrid.getChildren().remove(card);
                            productList.remove(item);
                            showNotification("✅ Đã xóa sản phẩm: " + item.getProductName(), "success");
                            appendLog("[ADMIN] Đã xóa sản phẩm ID=" + item.getProductId());
                        } else {
                            showAlert(Alert.AlertType.ERROR, "Xóa thất bại", res.message);
                        }
                    });
                }, "admin-delete-thread").start();
            }
        });
    }

    private void showEditDialog(ProductItem item, VBox card) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Chỉnh sửa sản phẩm");
        dialog.setResizable(false);

        Label lbName = new Label("Tên sản phẩm *");
        TextField tfName = new TextField(item.getProductName());
        Label lbDesc = new Label("Mô tả");
        TextArea taDesc = new TextArea();
        taDesc.setPromptText("Mô tả sản phẩm");
        taDesc.setPrefRowCount(3);
        taDesc.setWrapText(true);
        Label lbPrice = new Label("Giá khởi điểm (VNĐ) *");
        TextField tfPrice = new TextField(String.valueOf((long) item.getRawPrice()));
        Label lbError = new Label("");
        lbError.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px;");

        Button btnSave = new Button("💾 Lưu thay đổi");
        Button btnCancel = new Button("Hủy");
        btnSave.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-background-radius: 5; -fx-pref-width: 140;");
        btnCancel.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; " +
                "-fx-background-radius: 5; -fx-pref-width: 80;");

        HBox btnBar = new HBox(10, btnSave, btnCancel);
        btnBar.setAlignment(Pos.CENTER_RIGHT);

        VBox layout = new VBox(8,
                new Label("ID: " + item.getProductId()),
                lbName, tfName, lbDesc, taDesc, lbPrice, tfPrice, lbError, btnBar);
        layout.setPadding(new Insets(20));
        layout.setPrefWidth(380);

        btnSave.setOnAction(e -> {
            lbError.setText("");
            String newName = tfName.getText().trim();
            if (newName.isEmpty()) { lbError.setText("⚠ Tên không được để trống."); return; }
            double newPrice;
            try {
                newPrice = Double.parseDouble(tfPrice.getText().trim().replace(",","").replace(".",""));
                if (newPrice <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) { lbError.setText("⚠ Giá phải là số dương."); return; }
            String newDesc = taDesc.getText().trim();
            btnSave.setDisable(true);
            btnSave.setText("Đang lưu...");
            double fp = newPrice;
            new Thread(() -> {
                AdminItemClient.Result res = controller.updateItem(item.getProductId(), newName, newDesc, fp);
                Platform.runLater(() -> {
                    if (res.success) {
                        item.setRawPrice((long) fp);
                        card.getChildren().stream()
                                .filter(n -> n instanceof Label && ((Label) n).getStyle().contains("bold"))
                                .findFirst().ifPresent(n -> ((Label) n).setText(newName));
                        dialog.close();
                        showNotification("✅ Đã cập nhật: " + newName, "success");
                    } else {
                        lbError.setText("⚠ " + res.message);
                        btnSave.setDisable(false);
                        btnSave.setText("💾 Lưu thay đổi");
                    }
                });
            }, "admin-update-thread").start();
        });
        btnCancel.setOnAction(e -> dialog.close());
        dialog.setScene(new Scene(layout));
        dialog.showAndWait();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Platform.runLater(() -> {
            Alert a = new Alert(type);
            a.setTitle(title);
            a.setHeaderText(null);
            a.setContentText(content);
            a.showAndWait();
        });
    }

    // ── Logout ────────────────────────────────────────────────────────────────
    @FXML
    private void handleLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Đăng xuất");
        confirm.setHeaderText(null);
        confirm.setContentText("Bạn có chắc muốn đăng xuất không?");
        ButtonType btnYes = new ButtonType("Đăng xuất", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnNo  = new ButtonType("Hủy",       ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(btnYes, btnNo);
        confirm.showAndWait().ifPresent(result -> {
            if (result == btnYes) {
                auction.client.ClientMain.setJwtToken(null);
                if (onLogout != null) Platform.runLater(onLogout);
            }
        });
    }

    // ── Manage Users (ADMIN) ──────────────────────────────────────────────────
    @FXML
    private void handleManageUsers() {
        if (controller == null) return;
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Quản lý tài khoản người dùng");
        dialog.setMinWidth(620);
        dialog.setMinHeight(420);
        Label loading = new Label("⏳ Đang tải danh sách...");
        loading.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;");
        VBox root = new VBox(12, loading);
        root.setPadding(new Insets(20));
        dialog.setScene(new Scene(root));
        dialog.show();

        new Thread(() -> {
            AdminItemClient.Result res = controller.fetchUsers();
            Platform.runLater(() -> {
                root.getChildren().clear();
                if (!res.success) { root.getChildren().add(new Label("❌ Lỗi: " + res.message)); return; }
                try {
                    JsonObject json = new Gson().fromJson(res.message, JsonObject.class);
                    JsonArray users = json.has("data") ? json.getAsJsonArray("data") : new JsonArray();
                    Label title = new Label("👥 Danh sách người dùng (" + users.size() + ")");
                    title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
                    ScrollPane scroll = new ScrollPane();
                    scroll.setFitToWidth(true);
                    VBox userList = new VBox(8);
                    userList.setPadding(new Insets(5));
                    for (JsonElement el : users) {
                        JsonObject u = el.getAsJsonObject();
                        String uname = u.has("username") ? u.get("username").getAsString() : "?";
                        String urole  = u.has("role")     ? u.get("role").getAsString()     : "?";
                        HBox row = new HBox(10);
                        row.setAlignment(Pos.CENTER_LEFT);
                        row.setStyle("-fx-background-color: white; -fx-border-color: #dce1e7; " +
                                "-fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 8 12;");
                        Label lblUser = new Label(uname);
                        lblUser.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-min-width: 160;");
                        Label lblRole = new Label("[" + urole + "]");
                        lblRole.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px; -fx-min-width: 80;");
                        Region spacer = new Region();
                        HBox.setHgrow(spacer, Priority.ALWAYS);
                        if (uname.equalsIgnoreCase(currentUsername) || "ADMIN".equalsIgnoreCase(urole)) {
                            Label lblSelf = new Label("(không thể chỉnh sửa)");
                            lblSelf.setStyle("-fx-text-fill: #bdc3c7; -fx-font-size: 11px;");
                            row.getChildren().addAll(lblUser, lblRole, spacer, lblSelf);
                        } else {
                            Button btnBlock = new Button("🔒 Block");
                            Button btnDelU = new Button("🗑 Xóa");
                            btnBlock.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; " +
                                    "-fx-background-radius: 4; -fx-font-size: 11px; -fx-cursor: hand;");
                            btnDelU.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; " +
                                    "-fx-background-radius: 4; -fx-font-size: 11px; -fx-cursor: hand;");
                            btnBlock.setOnAction(e -> {
                                boolean isBlocked = btnBlock.getText().contains("Block");
                                btnBlock.setDisable(true);
                                new Thread(() -> {
                                    AdminItemClient.Result r = isBlocked
                                            ? controller.blockUser(uname) : controller.unblockUser(uname);
                                    Platform.runLater(() -> {
                                        btnBlock.setDisable(false);
                                        if (r.success) {
                                            btnBlock.setText(isBlocked ? "🔓 Unblock" : "🔒 Block");
                                            showNotification((isBlocked ? "🔒 Block: " : "🔓 Unblock: ") + uname, "info");
                                        } else { showAlert(Alert.AlertType.ERROR, "Thất bại", r.message); }
                                    });
                                }, "admin-block-thread").start();
                            });
                            btnDelU.setOnAction(e -> {
                                Alert conf = new Alert(Alert.AlertType.CONFIRMATION);
                                conf.setContentText("Xóa tài khoản \"" + uname + "\"?");
                                ButtonType yes = new ButtonType("Xóa", ButtonBar.ButtonData.OK_DONE);
                                ButtonType no  = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
                                conf.getButtonTypes().setAll(yes, no);
                                conf.showAndWait().ifPresent(r -> {
                                    if (r == yes) {
                                        btnDelU.setDisable(true);
                                        new Thread(() -> {
                                            AdminItemClient.Result dr = controller.deleteUser(uname);
                                            Platform.runLater(() -> {
                                                if (dr.success) { userList.getChildren().remove(row); }
                                                else { btnDelU.setDisable(false); showAlert(Alert.AlertType.ERROR, "Xóa thất bại", dr.message); }
                                            });
                                        }, "admin-del-user").start();
                                    }
                                });
                            });
                            row.getChildren().addAll(lblUser, lblRole, spacer, btnBlock, btnDelU);
                        }
                        userList.getChildren().add(row);
                    }
                    scroll.setContent(userList);
                    Button btnClose = new Button("✖ Đóng");
                    btnClose.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; " +
                            "-fx-background-radius: 5; -fx-cursor: hand; -fx-padding: 6 20;");
                    btnClose.setOnAction(e -> dialog.close());
                    HBox footer = new HBox(btnClose);
                    footer.setAlignment(Pos.CENTER_RIGHT);
                    root.getChildren().addAll(title, scroll, footer);
                } catch (Exception ex) {
                    root.getChildren().add(new Label("❌ Lỗi đọc dữ liệu: " + ex.getMessage()));
                }
            });
        }, "fetch-users-thread").start();
    }
}
