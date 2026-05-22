package auction.server.model;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AuctionSession {
    private int auctionId;        // ID phiên đấu giá
    private int itemId;           // ID sản phẩm
    private int sellerId;         // ID người bán
    private double currentPrice;  // Giá hiện tại
    private String highestBidder; // Username người đặt giá cao nhất
    private boolean isFinished;   // Trạng thái kết thúc
    private Timestamp startTime;  // Thời gian bắt đầu
    private Timestamp endTime;    // Thời gian kết thúc

    public enum Role {
        SELLER, BIDDER
    }
    private String currentSeller;
    private final Set<String> bidders = new HashSet<>();

    // Lưu lịch sử đặt giá ngay trong phiên (thay cho bảng bids)
    private final List<String> bidHistory = new ArrayList<>();

    public AuctionSession(int itemId, int sellerId, double startingPrice, Timestamp startTime, Timestamp endTime) {
        this.itemId = itemId;
        this.sellerId = sellerId;
        this.currentPrice = startingPrice;
        this.highestBidder = "Chưa có ai";
        this.isFinished = false;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    // Đặt giá mới
    public synchronized boolean placeBid(String username, double newBidPrice) {
        if (!isAuctionRunning()) return false;

        // Tránh việc Seller tự buff giá của chính mình
        if (username.equals(currentSeller)) return false;

        // Phải là người đã chọn vai trò Bidder mới được đặt giá
        if (!bidders.contains(username)) return false;

        if (newBidPrice > this.currentPrice) {
            this.currentPrice = newBidPrice;
            this.highestBidder = username;
            bidHistory.add(username + " đặt giá " + newBidPrice);
            return true;
        }
        return false;
    }
    public synchronized boolean changeUserRole(String username, Role desiredRole) {
        // 1. Tuyệt đối không cho đổi vai trò nếu trận đấu đang diễn ra
        if (isAuctionRunning()) {
            return false;
        }

        if (desiredRole == Role.SELLER) {
            // Nếu người này muốn làm Seller
            // Kiểm tra xem phòng đã có Seller khác chưa
            if (currentSeller != null && !currentSeller.equals(username)) {
                return false; // Báo lỗi: "Đã có người làm Seller cho phòng này"
            }

            // Nếu hợp lệ: Rút người này khỏi danh sách Bidder (nếu có) và đưa lên làm Seller
            bidders.remove(username);
            currentSeller = username;
            return true;

        } else if (desiredRole == Role.BIDDER) {
            // Nếu người này muốn làm Bidder
            // Nếu họ đang là Seller mà muốn làm Bidder -> Trả lại ghế Seller thành null
            if (currentSeller != null && currentSeller.equals(username)) {
                currentSeller = null;
            }

            // Thêm người này vào danh sách Bidder
            bidders.add(username);
            return true;
        }

        return false;
    }

    public boolean isAuctionRunning() {
        if (isFinished) return false;
        Timestamp now = new Timestamp(System.currentTimeMillis());
        return now.after(startTime) && now.before(endTime);
    }
    public boolean canStartAuction() {
        return currentSeller != null && bidders.size() >= 2;
    }

    // Kết thúc phiên đấu giá
    public synchronized void finishAuction() {
        this.isFinished = true;
        bidHistory.add("Phiên kết thúc. Người thắng: " + highestBidder + " với giá " + currentPrice);
    }

    // Getter & Setter
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

    public List<String> getBidHistory() { return bidHistory; }
}
