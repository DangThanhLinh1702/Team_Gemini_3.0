package auction.database;

import auction.shared.model.AuctionItem;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Repository xử lý truy cập database cho auction items
public class AuctionRepository {
    
    // Lấy tất cả sản phẩm đang đấu giá
    public List<AuctionItem> getActiveAuctions() {
        List<AuctionItem> items = new ArrayList<>();
        String sql = "SELECT * FROM auction_items WHERE status = 'ACTIVE' AND end_time > NOW()";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                AuctionItem item = mapResultSetToAuctionItem(rs);
                items.add(item);
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi lấy danh sách đấu giá: " + e.getMessage());
        }
        return items;
    }
    
    // Lấy sản phẩm theo ID
    public AuctionItem getAuctionItemById(String id) {
        String sql = "SELECT * FROM auction_items WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, id);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToAuctionItem(rs);
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi lấy sản phẩm theo ID: " + e.getMessage());
        }
        return null;
    }
    
    // Cập nhật giá và người dẫn đầu
    public boolean updateBid(String itemId, long newPrice, String bidder) {
        String sql = "UPDATE auction_items SET current_price = ?, current_bidder = ? WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, newPrice);
            stmt.setString(2, bidder);
            stmt.setString(3, itemId);
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi khi cập nhật bid: " + e.getMessage());
            return false;
        }
    }
    
    // Kết thúc đấu giá
    public boolean endAuction(String itemId) {
        String sql = "UPDATE auction_items SET status = 'ENDED' WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, itemId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi khi kết thúc đấu giá: " + e.getMessage());
            return false;
        }
    }
    
    // Thêm sản phẩm đấu giá mới
    public boolean addAuctionItem(AuctionItem item) {
        String sql = "INSERT INTO auction_items (id, name, description, starting_price, current_price, " +
                    "seller_username, start_time, end_time, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, item.getId());
            stmt.setString(2, item.getName());
            stmt.setString(3, item.getDescription());
            stmt.setLong(4, item.getStartingPrice());
            stmt.setLong(5, item.getCurrentPrice());
            stmt.setString(6, item.getSellerUsername());
            stmt.setTimestamp(7, Timestamp.valueOf(item.getStartTime()));
            stmt.setTimestamp(8, Timestamp.valueOf(item.getEndTime()));
            stmt.setString(9, item.getStatus());
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi khi thêm sản phẩm: " + e.getMessage());
            return false;
        }
    }
    
    // Map ResultSet sang AuctionItem
    private AuctionItem mapResultSetToAuctionItem(ResultSet rs) throws SQLException {
        AuctionItem item = new AuctionItem();
        item.setId(rs.getString("id"));
        item.setName(rs.getString("name"));
        item.setDescription(rs.getString("description"));
        item.setStartingPrice(rs.getLong("starting_price"));
        item.setCurrentPrice(rs.getLong("current_price"));
        item.setSellerUsername(rs.getString("seller_username"));
        item.setCurrentBidder(rs.getString("current_bidder"));
        
        Timestamp startTime = rs.getTimestamp("start_time");
        if (startTime != null) {
            item.setStartTime(startTime.toLocalDateTime());
        }
        
        Timestamp endTime = rs.getTimestamp("end_time");
        if (endTime != null) {
            item.setEndTime(endTime.toLocalDateTime());
        }
        
        item.setStatus(rs.getString("status"));
        return item;
    }
}
