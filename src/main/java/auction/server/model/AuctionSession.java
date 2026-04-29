package auction.server.model;

public class AuctionSession {
    private String idItem;
    private String nameItem;
    private double currentPrice;
    private String highestBidder;
    private boolean isFinished;
    private long endTime; // THÊM: Lưu lại mốc thời gian kết thúc (millisecond)

    public AuctionSession(String idItem, String nameItem, double startingPrice, long durationSeconds) {
        this.idItem = idItem;
        this.nameItem = nameItem;
        this.currentPrice = startingPrice;
        this.highestBidder = "Chưa có ai";
        this.isFinished = false;
        // Tính toán thời gian kết thúc ngay khi tạo phiên
        this.endTime = System.currentTimeMillis() + (durationSeconds * 1000);
    }

    public synchronized boolean placeBid(String username , double newBidPrice){
        if(isFinished){
            return false;
        }
        if(newBidPrice > this.currentPrice){
            this.currentPrice = newBidPrice;
            this.highestBidder = username;
            return true;
        }
        return false;
    }

    public synchronized void finishAuction() {
        this.isFinished = true;
    }

    public String getIdItem() { return idItem; }
    public String getNameItem() { return nameItem; }
    public double getCurrentPrice() { return currentPrice; }
    public String getHighestBidder() { return highestBidder; }
    public boolean isFinished() { return isFinished; }
    public long getEndTime() { return endTime; } // THÊM: Getter cho endTime
}