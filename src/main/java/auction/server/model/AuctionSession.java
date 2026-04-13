package auction.server.model;

public class AuctionSession {
    private String idItem;
    private String nameItem;
    private double currentPrice;
    private String highestBidder;
    private boolean isFinished;

    public AuctionSession(String idItem, String nameItem, double startingPrice) {
        this.idItem = idItem;
        this.nameItem = nameItem;
        this.currentPrice = startingPrice;
        this.highestBidder = "Chưa có ai";
        this.isFinished = false;
    }
    // HÀM QUAN TRỌNG NHẤT: XỬ LÝ ĐỒNG THỜI
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
    // Hàm dùng để khóa phiên đấu giá khi hết giờ
    public synchronized void finishAuction() {
        this.isFinished = true;
    }

    public String getIdItem() {
        return idItem;
    }

    public String getNameItem() {
        return nameItem;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public String getHighestBidder() {
        return highestBidder;
    }

    public boolean isFinished() {
        return isFinished;
    }
}
