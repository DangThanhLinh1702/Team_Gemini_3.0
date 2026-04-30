package auction.server.model;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class AuctionSession {
    private int auctionId;        // ID phiên đấu giá
    private int itemId;           // ID sản phẩm
    private int sellerId;         // ID người bán
    private double currentPrice;  // Giá hiện tại
    private String highestBidder; // Username người đặt giá cao nhất
    private boolean isFinished;   // Trạng thái kết thúc
    private Timestamp startTime;  // Thời gian bắt đầu
    private Timestamp endTime;    // Thời gian kết thúc

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
        if (isFinished) return false;
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
