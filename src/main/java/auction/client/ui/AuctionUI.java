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
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.ButtonBar;
import javafx.geometry.Insets;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.TilePane;
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

    @FXML
    private Label lblCurrentUser;
    @FXML
    private Label lblSelectedProduct;
    @FXML
    private Label lblCurrentPrice;
    @FXML
    private Label lblLeader;
    @FXML
    private Label lblCountdown;
    @FXML
    private Label lblNotification;
    @FXML
    private TextField txtBidAmount;
    @FXML
    private Button btnBid;
    @FXML
    private Button btnPostItem;
    @FXML
    private Button btnRefresh;
    @FXML
    private Button btnChangeRole;
    @FXML
    private Button btnManageUsers;
    @FXML
    private TextArea txtLog;

    @FXML
    private ScrollPane gridScrollPane;
    @FXML
    private TilePane productGrid;
    @FXML
    private VBox detailPanel;
    @FXML
    private Label lblDetailName;
    @FXML
    private Label lblDetailSeller;
    @FXML
    private ListView<String> listBidHistory;

    // --- THÊM Ô CHỨA ẢNH CHI TIẾT (fx:id="imgDetailPreview") ---
    @FXML
    private ImageView imgDetailPreview;

    private AuctionController controller;
    private ObservableList<ProductItem> productList;
    private ProductItem selectedProduct;
    private String currentUsername;
    private String currentRole;
    private String jwtToken; // Lưu token để truyền cho các lệnh ADMIN
    private Runnable onLogout; // Callback để quay về màn hình Login

    public void setJwtToken(String token) {
        this.jwtToken = token;
    }

    /** Đặt callback đăng xuất — ClientMain truyền vào */
    public void setOnLogout(Runnable onLogout) {
        this.onLogout = onLogout;
    }

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
        if (jwtToken != null) controller.setJwtToken(jwtToken);
        updateRoleUI();
        Platform.runLater(this::handleRefresh);
    }

    @FXML
    private void handleToggleRole() {
        if (currentRole == null) return;
        // ADMIN không được phép đổi role
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
                // ADMIN: ẩn hết chức năng của Bidder và Seller, hiện quản lý user
                if (btnPostItem    != null) btnPostItem.setVisible(false);
                if (btnChangeRole  != null) btnChangeRole.setVisible(false);
                if (btnManageUsers != null) btnManageUsers.setVisible(true);
                btnBid.setDisable(true);
            } else {
                if (btnPostItem    != null) btnPostItem.setVisible("SELLER".equalsIgnoreCase(currentRole));
                if (btnChangeRole  != null) btnChangeRole.setVisible(true);
                if (btnManageUsers != null) btnManageUsers.setVisible(false);
                btnBid.setDisable("SELLER".equalsIgnoreCase(currentRole));
            }
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
                    item.setRawPrice((long) newPrice); // setRawPrice tự cập nhật currentPriceProperty
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
        card.setPrefSize(180, 240);
        card.setStyle("-fx-background-color: white; -fx-border-color: #bdc3c7; -fx-border-radius: 8; -fx-padding: 10; -fx-cursor: hand;");

        // ── Ảnh sản phẩm ────────────────────────────────────────────────────
        StackPane imgBox = new StackPane();
        imgBox.setPrefSize(160, 120);
        imgBox.setStyle("-fx-background-color: #ecf0f1; -fx-background-radius: 5;");

        if (item.getImageBase64() != null && !item.getImageBase64().isEmpty()) {
            try {
                byte[] imgBytes = Base64.getDecoder().decode(item.getImageBase64());
                Image img = new Image(new ByteArrayInputStream(imgBytes));
                ImageView imgView = new ImageView(img);
                imgView.setFitWidth(150);
                imgView.setFitHeight(110);
                imgView.setPreserveRatio(true);
                imgBox.getChildren().add(imgView);
            } catch (Exception e) {
                imgBox.getChildren().add(new Label("Lỗi ảnh"));
            }
        } else {
            imgBox.getChildren().add(new Label("Không có ảnh"));
        }

        // ── Thông tin sản phẩm ──────────────────────────────────────────────
        Label nameLbl = new Label(item.getProductName());
        nameLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        nameLbl.setWrapText(true);

        Label priceLbl = new Label();
        priceLbl.textProperty().bind(item.currentPriceProperty());
        priceLbl.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");

        card.getChildren().addAll(imgBox, nameLbl, priceLbl);

        // ── Nút ADMIN: Sửa + Xóa (chỉ hiện khi role là ADMIN) ─────────────
        if ("ADMIN".equalsIgnoreCase(currentRole)) {
            javafx.scene.layout.HBox adminBar = new javafx.scene.layout.HBox(5);
            adminBar.setAlignment(javafx.geometry.Pos.CENTER);

            Button btnEdit = new Button("✏ Sửa");
            btnEdit.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; "
                    + "-fx-font-size: 11px; -fx-background-radius: 4; -fx-cursor: hand;");
            btnEdit.setMaxWidth(Double.MAX_VALUE);

            Button btnDelete = new Button("🗑 Xóa");
            btnDelete.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; "
                    + "-fx-font-size: 11px; -fx-background-radius: 4; -fx-cursor: hand;");
            btnDelete.setMaxWidth(Double.MAX_VALUE);

            javafx.scene.layout.HBox.setHgrow(btnEdit, javafx.scene.layout.Priority.ALWAYS);
            javafx.scene.layout.HBox.setHgrow(btnDelete, javafx.scene.layout.Priority.ALWAYS);

            btnEdit.setOnAction(e -> {
                e.consume(); // không trigger click card
                showEditDialog(item, card);
            });
            btnDelete.setOnAction(e -> {
                e.consume();
                showDeleteConfirm(item, card);
            });

            adminBar.getChildren().addAll(btnEdit, btnDelete);
            card.getChildren().add(adminBar);
        }

        // ── Click vào card → vào chi tiết ───────────────────────────────────
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

    // ── Dialog xác nhận XÓA sản phẩm ─────────────────────────────────────────
    private void showDeleteConfirm(ProductItem item, VBox card) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận xóa");
        confirm.setHeaderText("Bạn có thực sự muốn xóa sản phẩm này?");
        confirm.setContentText("\"" + item.getProductName() + "\"\n\nHành động này sẽ xóa luôn phiên đấu giá và không thể hoàn tác.");

        ButtonType btnYes = new ButtonType("Xóa", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnNo = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(btnYes, btnNo);

        // Style nút Xóa màu đỏ
        confirm.getDialogPane().lookupButton(btnYes)
                .setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");

        confirm.showAndWait().ifPresent(result -> {
            if (result == btnYes) {
                if (controller == null) {
                    showNotification("Lỗi: controller chưa sẵn sàng.", "error");
                    return;
                }

                // Gọi API trong background thread để không block UI
                new Thread(() -> {
                    AdminItemClient.Result res = controller.deleteItem(item.getProductId());
                    Platform.runLater(() -> {
                        if (res.success) {
                            // Xóa card khỏi giao diện ngay lập tức
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

    // ── Dialog SỬA sản phẩm ──────────────────────────────────────────────────
    private void showEditDialog(ProductItem item, VBox card) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Chỉnh sửa sản phẩm");
        dialog.setResizable(false);

        // ── Form fields ──────────────────────────────────────────────────────
        Label lbName = new Label("Tên sản phẩm *");
        TextField tfName = new TextField(item.getProductName());
        tfName.setPromptText("Tên sản phẩm (bắt buộc)");

        Label lbDesc = new Label("Mô tả");
        TextArea taDesc = new TextArea();
        taDesc.setPromptText("Mô tả sản phẩm");
        taDesc.setPrefRowCount(3);
        taDesc.setWrapText(true);

        Label lbPrice = new Label("Giá khởi điểm (VNĐ) *");
        TextField tfPrice = new TextField(String.valueOf((long) item.getRawPrice()));
        tfPrice.setPromptText("Ví dụ: 500000");

        Label lbError = new Label("");
        lbError.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px;");

        // ── Nút ─────────────────────────────────────────────────────────────
        Button btnSave = new Button("💾 Lưu thay đổi");
        Button btnCancel = new Button("Hủy");

        btnSave.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; "
                + "-fx-font-weight: bold; -fx-background-radius: 5; -fx-pref-width: 140;");
        btnCancel.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; "
                + "-fx-background-radius: 5; -fx-pref-width: 80;");

        javafx.scene.layout.HBox btnBar = new javafx.scene.layout.HBox(10, btnSave, btnCancel);
        btnBar.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        // ── Layout ───────────────────────────────────────────────────────────
        VBox layout = new VBox(8,
                new Label("ID: " + item.getProductId()),
                lbName, tfName,
                lbDesc, taDesc,
                lbPrice, tfPrice,
                lbError,
                btnBar);
        layout.setPadding(new javafx.geometry.Insets(20));
        layout.setPrefWidth(380);
        layout.setStyle("-fx-background-color: #f9f9f9;");

        // ── Sự kiện Lưu ─────────────────────────────────────────────────────
        btnSave.setOnAction(e -> {
            lbError.setText(""); // reset lỗi cũ

            // Validate tên
            String newName = tfName.getText().trim();
            if (newName.isEmpty()) {
                lbError.setText("⚠ Tên sản phẩm không được để trống.");
                tfName.requestFocus();
                return;
            }

            // Validate giá
            double newPrice;
            try {
                String priceStr = tfPrice.getText().trim().replace(",", "").replace(".", "");
                newPrice = Double.parseDouble(priceStr);
                if (newPrice <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                lbError.setText("⚠ Giá khởi điểm phải là số dương.");
                tfPrice.requestFocus();
                return;
            }

            String newDesc = taDesc.getText().trim();

            // Disable nút để tránh double-click
            btnSave.setDisable(true);
            btnSave.setText("Đang lưu...");

            double finalPrice = newPrice;
            new Thread(() -> {
                AdminItemClient.Result res = controller.updateItem(
                        item.getProductId(), newName, newDesc, finalPrice);

                Platform.runLater(() -> {
                    if (res.success) {
                        // Cập nhật card ngay trên UI không cần refresh
                        item.setRawPrice((long) finalPrice); // tự cập nhật currentPriceProperty

                        // Cập nhật label tên trên card
                        card.getChildren().stream()
                                .filter(n -> n instanceof Label && ((Label) n).getStyle().contains("bold"))
                                .findFirst()
                                .ifPresent(n -> ((Label) n).setText(newName));

                        dialog.close();
                        showNotification("✅ Đã cập nhật: " + newName, "success");
                        appendLog("[ADMIN] Đã sửa sản phẩm ID=" + item.getProductId()
                                + " → tên=\"" + newName + "\", giá=" + (long) finalPrice);
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

    // ── Helper: hiện Alert đơn giản ──────────────────────────────────────────
    private void showAlert(Alert.AlertType type, String title, String content) {
        Platform.runLater(() -> {
            Alert a = new Alert(type);
            a.setTitle(title);
            a.setHeaderText(null);
            a.setContentText(content);
            a.showAndWait();
        });
    }

    // ── ĐĂNG XUẤT ────────────────────────────────────────────────────────────
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
                // Xóa token toàn cục
                auction.client.ClientMain.setJwtToken(null);
                if (onLogout != null) {
                    Platform.runLater(onLogout);
                }
            }
        });
    }

    // ── ADMIN: Mở cửa sổ quản lý User ───────────────────────────────────────
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
        root.setStyle("-fx-background-color: #f9f9f9;");
        dialog.setScene(new Scene(root));
        dialog.show();

        new Thread(() -> {
            AdminItemClient.Result res = controller.fetchUsers();
            Platform.runLater(() -> {
                root.getChildren().clear();
                if (!res.success) {
                    root.getChildren().add(new Label("❌ Lỗi: " + res.message));
                    return;
                }
                try {
                    // Parse JSON trả về: { status, message, data: [ {username, role, ...}, ... ] }
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

                        javafx.scene.layout.HBox row = new javafx.scene.layout.HBox(10);
                        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                        row.setStyle("-fx-background-color: white; -fx-border-color: #dce1e7; "
                                + "-fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 8 12;");

                        Label lblUser = new Label(uname);
                        lblUser.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-min-width: 160;");
                        Label lblRole = new Label("[" + urole + "]");
                        lblRole.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px; -fx-min-width: 80;");

                        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
                        javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

                        // Không cho ADMIN tự xóa/block chính mình
                        if (uname.equalsIgnoreCase(currentUsername) || "ADMIN".equalsIgnoreCase(urole)) {
                            Label lblSelf = new Label("(tài khoản này không thể chỉnh sửa)");
                            lblSelf.setStyle("-fx-text-fill: #bdc3c7; -fx-font-size: 11px;");
                            row.getChildren().addAll(lblUser, lblRole, spacer, lblSelf);
                        } else {
                            Button btnBlock = new Button("🔒 Block");
                            Button btnDelete = new Button("🗑 Xóa");
                            btnBlock.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; "
                                    + "-fx-background-radius: 4; -fx-font-size: 11px; -fx-cursor: hand;");
                            btnDelete.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; "
                                    + "-fx-background-radius: 4; -fx-font-size: 11px; -fx-cursor: hand;");

                            btnBlock.setOnAction(e -> {
                                boolean isBlocked = btnBlock.getText().contains("Block");
                                String action = isBlocked ? "block" : "unblock";
                                btnBlock.setDisable(true);
                                new Thread(() -> {
                                    AdminItemClient.Result r = isBlocked
                                            ? controller.blockUser(uname)
                                            : controller.unblockUser(uname);
                                    Platform.runLater(() -> {
                                        btnBlock.setDisable(false);
                                        if (r.success) {
                                            btnBlock.setText(isBlocked ? "🔓 Unblock" : "🔒 Block");
                                            btnBlock.setStyle(isBlocked
                                                    ? "-fx-background-color: #27ae60; -fx-text-fill: white; -fx-background-radius: 4; -fx-font-size: 11px; -fx-cursor: hand;"
                                                    : "-fx-background-color: #e67e22; -fx-text-fill: white; -fx-background-radius: 4; -fx-font-size: 11px; -fx-cursor: hand;");
                                            showNotification((isBlocked ? "🔒 Đã block: " : "🔓 Đã unblock: ") + uname, "info");
                                            appendLog("[ADMIN] " + (isBlocked ? "Block" : "Unblock") + " tài khoản: " + uname);
                                        } else {
                                            showAlert(Alert.AlertType.ERROR, "Thất bại", r.message);
                                        }
                                    });
                                }, "admin-block-thread").start();
                            });

                            btnDelete.setOnAction(e -> {
                                Alert conf = new Alert(Alert.AlertType.CONFIRMATION);
                                conf.setTitle("Xác nhận xóa tài khoản");
                                conf.setContentText("Xóa tài khoản \"" + uname + "\"? Hành động này không thể hoàn tác.");
                                ButtonType yes = new ButtonType("Xóa", ButtonBar.ButtonData.OK_DONE);
                                ButtonType no  = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
                                conf.getButtonTypes().setAll(yes, no);
                                conf.getDialogPane().lookupButton(yes)
                                        .setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
                                conf.showAndWait().ifPresent(r -> {
                                    if (r == yes) {
                                        btnDelete.setDisable(true);
                                        new Thread(() -> {
                                            AdminItemClient.Result dr = controller.deleteUser(uname);
                                            Platform.runLater(() -> {
                                                if (dr.success) {
                                                    userList.getChildren().remove(row);
                                                    showNotification("✅ Đã xóa tài khoản: " + uname, "success");
                                                    appendLog("[ADMIN] Đã xóa tài khoản: " + uname);
                                                } else {
                                                    btnDelete.setDisable(false);
                                                    showAlert(Alert.AlertType.ERROR, "Xóa thất bại", dr.message);
                                                }
                                            });
                                        }, "admin-delete-user-thread").start();
                                    }
                                });
                            });

                            row.getChildren().addAll(lblUser, lblRole, spacer, btnBlock, btnDelete);
                        }
                        userList.getChildren().add(row);
                    }

                    scroll.setContent(userList);
                    Button btnClose = new Button("✖ Đóng");
                    btnClose.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; "
                            + "-fx-background-radius: 5; -fx-cursor: hand; -fx-padding: 6 20;");
                    btnClose.setOnAction(e -> dialog.close());
                    javafx.scene.layout.HBox footer = new javafx.scene.layout.HBox(btnClose);
                    footer.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
                    root.getChildren().addAll(title, scroll, footer);
                } catch (Exception ex) {
                    root.getChildren().add(new Label("❌ Lỗi đọc dữ liệu: " + ex.getMessage()));
                }
            });
        }, "fetch-users-thread").start();
    }
}