package auction.server.model;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class AuctionSession {
    private int auctionId;
    private int itemId;
    private int sellerId;
    private double currentPrice;
    private String highestBidder;
    private boolean isFinished;
    private Timestamp startTime;
    private Timestamp endTime;

    // Lưu lịch sử đặt giá trong phiên
    private final List<String> bidHistory = new ArrayList<>();

    private static final SimpleDateFormat SDF = new SimpleDateFormat("HH:mm:ss");

    public AuctionSession(int itemId, int sellerId, double startingPrice, Timestamp startTime, Timestamp endTime) {
        this.itemId = itemId;
        this.sellerId = sellerId;
        this.currentPrice = startingPrice;
        this.highestBidder = "Chưa có ai";
        this.isFinished = false;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    /**
     * Đặt giá mới — bất kỳ người dùng nào (không phải seller) đều được đặt.
     * Giá mới phải STRICTLY lớn hơn giá hiện tại.
     */
    public synchronized boolean placeBid(String username, double newBidPrice) {
        if (!isAuctionRunning()) return false;
        if (newBidPrice > this.currentPrice) {
            this.currentPrice = newBidPrice;
            this.highestBidder = username;
            String timeStr = SDF.format(new Timestamp(System.currentTimeMillis()));
            bidHistory.add(String.format("[%s] %s → %,.0f VNĐ", timeStr, username, newBidPrice));
            return true;
        }
        return false;
    }

    public boolean isAuctionRunning() {
        if (isFinished) return false;
        Timestamp now = new Timestamp(System.currentTimeMillis());
        return now.before(endTime);
    }

    public synchronized void finishAuction() {
        this.isFinished = true;
        String timeStr = SDF.format(new Timestamp(System.currentTimeMillis()));
        bidHistory.add(String.format("[%s] ⏰ Phiên kết thúc — Người thắng: %s với giá %,.0f VNĐ",
                timeStr, highestBidder, currentPrice));
    }

    // Getters & Setters
    public int getAuctionId()  { return auctionId; }
    public void setAuctionId(int auctionId) { this.auctionId = auctionId; }

    public int getItemId()     { return itemId; }
    public void setItemId(int itemId) { this.itemId = itemId; }

    public int getSellerId()   { return sellerId; }
    public void setSellerId(int sellerId) { this.sellerId = sellerId; }

    public double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }

    public String getHighestBidder() { return highestBidder; }
    public void setHighestBidder(String highestBidder) { this.highestBidder = highestBidder; }

    public boolean isFinished()  { return isFinished; }
    public void setFinished(boolean finished) { isFinished = finished; }

    public Timestamp getStartTime() { return startTime; }
    public void setStartTime(Timestamp startTime) { this.startTime = startTime; }

    public Timestamp getEndTime() { return endTime; }
    public void setEndTime(Timestamp endTime) { this.endTime = endTime; }

    public List<String> getBidHistory() { return new ArrayList<>(bidHistory); }
}