package auction.shared.model;

import java.time.LocalDateTime;

// Entity model cho sản phẩm đấu giá trong database
public class AuctionItem {
    private String id;
    private String name;
    private String description;
    private long startingPrice;
    private long currentPrice;
    private String sellerUsername;
    private String currentBidder;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status; // ACTIVE, ENDED, CANCELLED
    
    public AuctionItem() {}
    
    public AuctionItem(String id, String name, String description, long startingPrice, 
                       String sellerUsername, LocalDateTime endTime) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.currentPrice = startingPrice;
        this.sellerUsername = sellerUsername;
        this.endTime = endTime;
        this.status = "ACTIVE";
    }
    
    // Getters và Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public long getStartingPrice() { return startingPrice; }
    public void setStartingPrice(long startingPrice) { this.startingPrice = startingPrice; }
    
    public long getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(long currentPrice) { this.currentPrice = currentPrice; }
    
    public String getSellerUsername() { return sellerUsername; }
    public void setSellerUsername(String sellerUsername) { this.sellerUsername = sellerUsername; }
    
    public String getCurrentBidder() { return currentBidder; }
    public void setCurrentBidder(String currentBidder) { this.currentBidder = currentBidder; }
    
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
