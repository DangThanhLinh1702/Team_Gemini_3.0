package auction.server.repository;
import auction.database.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BidHistoryRepository {

    /**
     * Đảm bảo bảng bid_history tồn tại. Gọi một lần khi server khởi động.
     */
    public void ensureTableExists() {
        String sql = "CREATE TABLE IF NOT EXISTS bid_history (" +
                "id           BIGINT       NOT NULL AUTO_INCREMENT, " +
                "auction_id   INT          NOT NULL, " +
                "item_id      INT          NOT NULL, " +
                "bidder       VARCHAR(100) NOT NULL, " +
                "bid_price    DOUBLE       NOT NULL, " +
                "bid_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "display_text VARCHAR(255) NOT NULL, " +
                "PRIMARY KEY (id), " +
                "INDEX idx_item (item_id), " +
                "INDEX idx_auction (auction_id)" +
                ")";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement()) {
            st.executeUpdate(sql);
            System.out.println("✅ [DB] Bảng bid_history đã sẵn sàng.");
        } catch (SQLException e) {
            System.err.println("❌ [DB] Lỗi tạo bảng bid_history: " + e.getMessage());
        }
    }

    /**
     * Lưu một lần đặt giá vào DB ngay lập tức.
     *
     * @param auctionId   ID phiên đấu giá
     * @param itemId      ID sản phẩm
     * @param bidder      Tên người đặt giá
     * @param bidPrice    Giá đặt
     * @param displayText Chuỗi hiển thị UI, ví dụ "[14:05:30] alice → 1.500.000 VNĐ"
     */
    public void saveBid(int auctionId, int itemId, String bidder,
                        double bidPrice, String displayText) {
        String sql = "INSERT INTO bid_history (auction_id, item_id, bidder, bid_price, bid_time, display_text) " +
                "VALUES (?, ?, ?, ?, NOW(), ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, auctionId);
            ps.setInt(2, itemId);
            ps.setString(3, bidder);
            ps.setDouble(4, bidPrice);
            ps.setString(5, displayText);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ [DB] Lỗi lưu bid_history: " + e.getMessage());
        }
    }

    /**
     * Lấy toàn bộ lịch sử đặt giá cho một sản phẩm (theo thứ tự thời gian).
     * Dùng khi client JOIN phòng hoặc server restart cần nạp lại vào RAM.
     *
     * @param itemId ID sản phẩm
     * @return Danh sách chuỗi display_text, sắp xếp cũ → mới
     */
    public List<String> getBidHistoryByItem(int itemId) {
        List<String> result = new ArrayList<>();
        String sql = "SELECT display_text FROM bid_history WHERE item_id = ? ORDER BY bid_time ASC, id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(rs.getString("display_text"));
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ [DB] Lỗi đọc bid_history item_id=" + itemId + ": " + e.getMessage());
        }
        return result;
    }

    /**
     * Lấy lịch sử theo auction_id (dùng cho báo cáo / admin).
     */
    public List<String> getBidHistoryByAuction(int auctionId) {
        List<String> result = new ArrayList<>();
        String sql = "SELECT display_text FROM bid_history WHERE auction_id = ? ORDER BY bid_time ASC, id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(rs.getString("display_text"));
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ [DB] Lỗi đọc bid_history auction_id=" + auctionId + ": " + e.getMessage());
        }
        return result;
    }
}