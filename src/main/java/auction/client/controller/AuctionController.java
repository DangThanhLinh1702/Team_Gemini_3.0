
package auction.client.controller;

import auction.client.ui.AuctionUI;
import auction.client.ui.ProductItem;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

/**
 * AuctionController - Xử lý toàn bộ logic nghiệp vụ của trang đấu giá
 *
 * Nhiệm vụ:
 *  - Kiểm tra giá đặt có hợp lệ không
 *  - Cập nhật UI khi có giá mới
 *  - Quản lý đồng hồ đếm ngược
 *  - Xử lý khi phiên đấu giá kết thúc
 *
 * Lưu ý: Class này KHÔNG xử lý giao diện trực tiếp,
 *        mà thông qua AuctionUI để cập nhật màn hình.
 */
public class AuctionController {

    // ==================== Hằng số ====================

    private static final int AUCTION_DURATION_SECONDS = 60;   // Thời gian mỗi phiên đấu giá (giây)
    private static final long MIN_BID_INCREMENT = 10_000;       // Tăng tối thiểu mỗi lần đặt giá (10,000 VNĐ)

    // ==================== Biến ====================

    private final AuctionUI ui;            // Tham chiếu đến giao diện để cập nhật
    private String currentProductId;       // ID sản phẩm đang được đấu giá
    private long currentHighestPrice;      // Giá cao nhất hiện tại
    private String currentLeader;          // Người đang dẫn đầu
    private String currentUsername;        // Tên người dùng đang đăng nhập
    private boolean isJoined;              // Người dùng đã tham gia phiên chưa
    private boolean isAuctionActive;       // Phiên đấu giá có đang diễn ra không

    private Timeline countdownTimer;       // Đồng hồ đếm ngược
    private int timeRemaining;             // Số giây còn lại

    // ==================== Khởi tạo ====================

    /**
     * @param ui Tham chiếu đến AuctionUI để gọi cập nhật giao diện
     */
    public AuctionController(AuctionUI ui) {
        this.ui = ui;
        this.isJoined = false;
        this.isAuctionActive = false;
        this.currentLeader = "";
        this.currentHighestPrice = 0;

        // TODO: Lấy tên người dùng từ session / login sau khi tích hợp Login
        this.currentUsername = "Người dùng";
        ui.setCurrentUser(currentUsername);

        // Tải dữ liệu sản phẩm mẫu (sau này thay bằng gọi network)
        loadSampleProducts();
    }

    // ==================== Xử lý Tham gia ====================

    /**
     * Xử lý khi người dùng nhấn "Tham gia đấu giá"
     *
     * @param productId ID sản phẩm muốn tham gia
     */
    public void joinAuction(String productId) {
        if (isJoined) {
            ui.showNotification("Bạn đã tham gia rồi!", "warning");
            return;
        }

        if (!isAuctionActive) {
            ui.showNotification("Phiên đấu giá chưa bắt đầu hoặc đã kết thúc.", "error");
            return;
        }

        // Lưu thông tin phiên đang tham gia
        this.currentProductId = productId;
        this.isJoined = true;

        // Kích hoạt nút "Đặt giá"
        ui.enableBidButton();
        ui.showNotification("✅ Tham gia thành công!", "success");

        // TODO: Gửi thông tin tham gia lên server qua Network layer
        // networkClient.sendJoinRequest(productId, currentUsername);
    }

    // ==================== Xử lý Đặt giá ====================

    /**
     * Xử lý khi người dùng nhấn "Đặt giá"
     *
     * Các bước:
     *  1. Kiểm tra phiên còn hoạt động không
     *  2. Kiểm tra giá có hợp lệ không (lớn hơn giá hiện tại + tối thiểu)
     *  3. Cập nhật giá và thông báo lên UI
     *  4. (TODO) Gửi lên server
     *
     * @param productId ID sản phẩm
     * @param bidAmount Số tiền người dùng muốn đặt
     */
    public void placeBid(String productId, long bidAmount) {

        // Kiểm tra 1: Phiên đấu giá còn hoạt động không
        if (!isAuctionActive) {
            ui.showNotification("❌ Phiên đấu giá đã kết thúc!", "error");
            return;
        }

        // Kiểm tra 2: Người dùng đã tham gia chưa
        if (!isJoined) {
            ui.showNotification("⚠️ Bạn cần tham gia trước khi đặt giá!", "warning");
            return;
        }

        // Kiểm tra 3: Giá phải lớn hơn 0
        if (bidAmount <= 0) {
            ui.showNotification("❌ Giá đặt phải lớn hơn 0!", "error");
            return;
        }

        // Kiểm tra 4: Giá mới phải cao hơn giá hiện tại ít nhất MIN_BID_INCREMENT
        long minimumBid = currentHighestPrice + MIN_BID_INCREMENT;
        if (bidAmount < minimumBid) {
            ui.showNotification(
                    String.format("❌ Giá đặt phải ít nhất %,d VNĐ (tăng tối thiểu %,d VNĐ)",
                            minimumBid, MIN_BID_INCREMENT),
                    "error"
            );
            return;
        }

        // ✅ Giá hợp lệ → cập nhật dữ liệu
        currentHighestPrice = bidAmount;
        currentLeader = currentUsername;

        // Cập nhật UI
        ui.updatePrice(productId, bidAmount, currentUsername);
        ui.showNotification("✅ Đặt giá thành công!", "success");

        // TODO: Gửi giá mới lên server
        // networkClient.sendBid(productId, bidAmount, currentUsername);
    }

    // ==================== Nhận cập nhật từ Server ====================

    /**
     * Gọi khi nhận được giá mới từ server (người khác đặt giá)
     * Thường được gọi từ Network layer sau khi nhận dữ liệu socket
     *
     * @param productId ID sản phẩm
     * @param newPrice  Giá mới
     * @param leader    Người vừa đặt giá
     */
    public void onNewBidReceived(String productId, long newPrice, String leader) {
        // Cập nhật dữ liệu nội bộ
        if (productId.equals(currentProductId)) {
            currentHighestPrice = newPrice;
            currentLeader = leader;
        }

        // Cập nhật UI
        ui.updatePrice(productId, newPrice, leader);
    }

    // ==================== Quản lý Đồng hồ đếm ngược ====================

    /**
     * Bắt đầu đồng hồ đếm ngược cho phiên đấu giá
     */
    public void startCountdown() {
        isAuctionActive = true;
        timeRemaining = AUCTION_DURATION_SECONDS;

        countdownTimer = new Timeline(
                new KeyFrame(Duration.seconds(1), event -> {
                    timeRemaining--;
                    ui.updateCountdown(timeRemaining);   // Cập nhật hiển thị mỗi giây

                    if (timeRemaining <= 0) {
                        countdownTimer.stop();
                        handleAuctionEnd();              // Hết giờ → kết thúc
                    }
                })
        );
        countdownTimer.setCycleCount(Timeline.INDEFINITE);
        countdownTimer.play();

        ui.appendLog("Phiên đấu giá bắt đầu! Thời gian: " + AUCTION_DURATION_SECONDS + " giây.");
    }

    /**
     * Dừng đồng hồ (dùng khi cần huỷ hoặc đặt lại)
     */
    public void stopCountdown() {
        if (countdownTimer != null) {
            countdownTimer.stop();
        }
        isAuctionActive = false;
    }

    // ==================== Kết thúc phiên ====================

    /**
     * Xử lý khi phiên đấu giá kết thúc (hết giờ hoặc server báo)
     */
    private void handleAuctionEnd() {
        isAuctionActive = false;
        isJoined = false;

        String winner = currentLeader.isEmpty() ? "Không có ai" : currentLeader;
        ui.showAuctionEnded(winner, currentHighestPrice);

        // TODO: Gửi thông báo kết thúc lên server để xác nhận
        // networkClient.sendAuctionEndConfirmation(currentProductId, winner, currentHighestPrice);
    }

    // ==================== Dữ liệu mẫu ====================

    /**
     * Load dữ liệu sản phẩm mẫu để test UI
     * TODO: Sau này thay bằng gọi từ Network layer (nhận từ server)
     */
    private void loadSampleProducts() {
        isAuctionActive = true; // Giả lập phiên đang hoạt động

        ui.addProduct(new ProductItem("P001", "iPhone 15 Pro Max 256GB", 25_000_000L, "Nguyễn Văn A", "Đang đấu"));
        ui.addProduct(new ProductItem("P002", "MacBook Air M2 2024",    28_000_000L, "Trần Thị B",   "Đang đấu"));
        ui.addProduct(new ProductItem("P003", "Samsung Galaxy S24 Ultra", 22_000_000L, "",            "Chưa có bid"));

        // Bắt đầu đếm ngược
        startCountdown();
    }

    // ==================== Getter ====================

    public boolean isAuctionActive() { return isAuctionActive; }
    public long getCurrentHighestPrice() { return currentHighestPrice; }
    public String getCurrentLeader() { return currentLeader; }
}
