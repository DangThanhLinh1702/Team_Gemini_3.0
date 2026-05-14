package auction.database;

import auction.shared.model.AuctionItem;
import auction.client.ui.ProductItem;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

// Service xử lý logic nghiệp vụ và đồng bộ dữ liệu từ database lên UI
public class AuctionService {
    private final AuctionRepository repository;
    
    public AuctionService() {
        this.repository = new AuctionRepository();
    }
    
    // Lấy tất cả sản phẩm đang đấu giá và chuyển thành ProductItem cho UI
    public List<ProductItem> getActiveAuctionProducts() {
        List<AuctionItem> items = repository.getActiveAuctions();
        List<ProductItem> products = new ArrayList<>();
        
        for (AuctionItem item : items) {
            String leader = item.getCurrentBidder() != null ? item.getCurrentBidder() : "---";
            String status = "Đang đấu";
            long endTime = item.getEndTime() != null ? 
                java.sql.Timestamp.valueOf(item.getEndTime()).getTime() : 
                System.currentTimeMillis() + (120 * 1000L);
            
            ProductItem product = new ProductItem(
                item.getId(),
                item.getName(),
                item.getCurrentPrice(),
                leader,
                status,
                item.getSellerUsername(),
                endTime
            );
            products.add(product);
        }
        
        return products;
    }
    
    // Đồng bộ dữ liệu từ database lên UI
    public void syncDataToUI(auction.client.ui.AuctionUI ui) {
        try {
            List<ProductItem> products = getActiveAuctionProducts();
            
            // Xóa dữ liệu cũ và thêm dữ liệu mới
            ui.clearTable();
            for (ProductItem product : products) {
                ui.addProduct(product);
            }
            
            ui.appendLog("Đồng bộ dữ liệu từ database thành công. Tìm thấy " + products.size() + " sản phẩm.");
            
        } catch (Exception e) {
            ui.appendLog("Lỗi khi đồng bộ dữ liệu: " + e.getMessage());
            ui.showNotification("Lỗi đồng bộ dữ liệu", "error");
        }
    }
    
    // Cập nhật giá bid mới
    public boolean updateBid(String itemId, long newPrice, String bidder) {
        return repository.updateBid(itemId, newPrice, bidder);
    }
    
    // Kết thúc đấu giá
    public boolean endAuction(String itemId) {
        return repository.endAuction(itemId);
    }
    
    // Thêm sản phẩm đấu giá mới
    public boolean addAuctionItem(AuctionItem item) {
        if (item.getId() == null || item.getId().isEmpty()) {
            item.setId(UUID.randomUUID().toString());
        }
        
        if (item.getStartTime() == null) {
            item.setStartTime(java.time.LocalDateTime.now());
        }
        
        return repository.addAuctionItem(item);
    }
    
    // Lấy thông tin sản phẩm theo ID
    public AuctionItem getAuctionItemById(String id) {
        return repository.getAuctionItemById(id);
    }
    
    // Kiểm tra và cập nhật các phiên đấu giá đã hết hạn
    public void checkAndUpdateExpiredAuctions() {
        List<AuctionItem> activeItems = repository.getActiveAuctions();
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        
        for (AuctionItem item : activeItems) {
            if (item.getEndTime() != null && item.getEndTime().isBefore(now)) {
                repository.endAuction(item.getId());
            }
        }
    }
}
