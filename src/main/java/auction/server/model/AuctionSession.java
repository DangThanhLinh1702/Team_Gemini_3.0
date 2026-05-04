package auction.server.model;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AuctionSession {
    // Định nghĩa các vai trò có thể chọn trong phòng
    public enum Role {
        SELLER, BIDDER
    }

    // Các trường dữ liệu gốc của bạn
    private int auctionId;        // ID phiên đấu giá
    private int itemId;           // ID sản phẩm
    private int sellerId;         // ID người bán (Dùng để lưu/map với Database)
    private double currentPrice;  // Giá hiện tại
    private String highestBidder; // Username người đặt giá cao nhất
    private boolean isFinished;   // Trạng thái kết thúc
    private Timestamp startTime;  // Thời gian bắt đầu
    private Timestamp endTime;    // Thời gian kết thúc

    // --- CÁC TRƯỜNG DỮ LIỆU BỔ SUNG CHO TÍNH NĂNG CHỌN VAI TRÒ ---

    // Lưu username của người đang giữ vai trò Seller hiện tại (null nếu chưa ai chọn)
    private String currentSeller;

    // Danh sách những người đang chọn vai trò Bidder
    private final Set<String> bidders = new HashSet<>();

    // Lưu lịch sử đặt giá ngay trong phiên
    private final List<String> bidHistory = new ArrayList<>();

    public AuctionSession(int itemId, int sellerId, double startingPrice, Timestamp startTime, Timestamp endTime) {
        this.itemId = itemId;
        this.sellerId = sellerId;
        this.currentPrice = startingPrice;
        this.highestBidder = "Chưa có ai";
        this.isFinished = false;
        this.startTime = startTime;
        this.endTime = endTime;
        this.currentSeller = null; // Sẽ được cập nhật khi có user chọn vai trò SELLER
    }

    // =====================================================================
    // LÔ-GIC QUẢN LÝ VAI TRÒ VÀ TRẠNG THÁI PHÒNG
    // =====================================================================

    /**
     * Chức năng: Người dùng tự chọn/đổi vai trò của mình trong phòng (trên App)
     */
    public synchronized boolean changeUserRole(String username, Role desiredRole) {
        // 1. Tuyệt đối không cho đổi vai trò nếu trận đấu đang diễn ra
        if (isAuctionRunning()) {
            return false;
        }

        if (desiredRole == Role.SELLER) {
            // Kiểm tra xem phòng đã có người khác làm Seller chưa
            if (currentSeller != null && !currentSeller.equals(username)) {
                return false; // Đã có người khác làm Seller, không thể tranh giành
            }

            // Hợp lệ: Xóa khỏi danh sách Bidder (nếu đang ở đó) và gán làm Seller
            bidders.remove(username);
            currentSeller = username;

            // Ghi nhận lịch sử hệ thống
            bidHistory.add("Hệ thống: " + username + " đã chọn vai trò Seller.");
            return true;

        } else if (desiredRole == Role.BIDDER) {
            // Nếu người này đang làm Seller mà muốn xuống làm Bidder -> Nhả ghế Seller (thành null)
            if (currentSeller != null && currentSeller.equals(username)) {
                currentSeller = null;
            }

            // Thêm vào danh sách Bidder
            bidders.add(username);

            // Ghi nhận lịch sử hệ thống
            bidHistory.add("Hệ thống: " + username + " đã chọn vai trò Bidder.");
            return true;
        }

        return false;
    }

    /**
     * Kiểm tra xem phiên đấu giá CÓ ĐANG THỰC SỰ DIỄN RA hay không
     * Trạng thái này kẹp giữa startTime và endTime, và chưa bị gọi finishAuction()
     */
    public boolean isAuctionRunning() {
        if (isFinished) return false;
        Timestamp now = new Timestamp(System.currentTimeMillis());
        return now.after(startTime) && now.before(endTime);
    }

    /**
     * Ràng buộc bắt đầu: Cần ít nhất 1 Seller và 2 Bidder
     */
    public boolean canStartAuction() {
        return currentSeller != null && bidders.size() >= 2;
    }

    // =====================================================================
    // LÔ-GIC ĐẤU GIÁ CHÍNH
    // =====================================================================

    // Đặt giá mới
    public synchronized boolean placeBid(String username, double newBidPrice) {
        // Kiểm tra xem cuộc đấu giá có đang chạy không
        if (!isAuctionRunning()) return false;

        // Seller không được tự đặt giá cho sản phẩm của mình
        if (username.equals(currentSeller)) return false;

        // Chỉ những ai đã chọn vai trò Bidder mới được đặt giá
        if (!bidders.contains(username)) return false;

        // Logic đặt giá gốc của bạn
        if (newBidPrice > this.currentPrice) {
            this.currentPrice = newBidPrice;
            this.highestBidder = username;
            bidHistory.add(username + " đặt giá " + newBidPrice);
            return true;
        }
        return false;
    }

    // Kết thúc phiên đấu giá
    public synchronized void finishAuction() {
        this.isFinished = true;
        bidHistory.add("Phiên kết thúc. Người thắng: " + highestBidder + " với giá " + currentPrice);
    }

    // =====================================================================
    // GETTER & SETTER ĐẦY ĐỦ
    // =====================================================================

    public int getAuctionId() { return auctionId; }
    public void setAuctionId(int auctionId) { this.auctionId = auctionId; }

    public int getItemId() { return itemId; }
    public void setItemId(int itemId) { this.itemId = itemId; }

    public int getSellerId() { return sellerId; }
    public void setSellerId(int sellerId) { this.sellerId = sellerId; }

    public double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }

    public String getHighestBidder() { return highestBidder; }
    public void setHighestBidder(String highestBidder) { this.highestBidder = highestBidder; }

    public boolean isFinished() { return isFinished; }
    public void setFinished(boolean finished) { isFinished = finished; }

    public Timestamp getStartTime() { return startTime; }
    public void setStartTime(Timestamp startTime) { this.startTime = startTime; }

    public Timestamp getEndTime() { return endTime; }
    public void setEndTime(Timestamp endTime) { this.endTime = endTime; }

    public String getCurrentSeller() { return currentSeller; }
    public void setCurrentSeller(String currentSeller) { this.currentSeller = currentSeller; }

    public Set<String> getBidders() { return bidders; }

    public List<String> getBidHistory() { return bidHistory; }
}