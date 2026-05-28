package auction.server.repository;

import auction.server.model.AuctionSession;
import auction.database.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository xử lý tất cả thao tác DB liên quan đến bảng auctions.
 */
public class AuctionRepository {

    public void saveAuction(AuctionSession session) {
        String sql = "INSERT INTO auctions (item_id, seller_id, current_price, highest_bidder, is_finished, start_time, end_time) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, session.getItemId());
            ps.setInt(2, session.getSellerId());
            ps.setDouble(3, session.getCurrentPrice());
            ps.setString(4, session.getHighestBidder());
            ps.setBoolean(5, false);
            ps.setTimestamp(6, session.getStartTime());
            ps.setTimestamp(7, session.getEndTime());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    session.setAuctionId(keys.getInt(1));
                }
            }
            System.out.println("✅ [DB] Lưu phiên | auction_id=" + session.getAuctionId()
                    + " | item_id=" + session.getItemId());

        } catch (SQLException e) {
            System.err.println("❌ [DB] Lỗi lưu phiên: " + e.getMessage());
        }
    }

    public void updateBid(int auctionId, double currentPrice, String highestBidder) {
        String sql = "UPDATE auctions SET current_price = ?, highest_bidder = ? WHERE auction_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDouble(1, currentPrice);
            ps.setString(2, highestBidder);
            ps.setInt(3, auctionId);
            ps.executeUpdate();

        } catch (SQLException e) {
            System.err.println("❌ [DB] Lỗi cập nhật giá: " + e.getMessage());
        }
    }

    public void finishAuction(int auctionId, double finalPrice, String winner) {
        String sql = "UPDATE auctions SET is_finished = true, current_price = ?, highest_bidder = ? "
                + "WHERE auction_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDouble(1, finalPrice);
            ps.setString(2, winner);
            ps.setInt(3, auctionId);
            ps.executeUpdate();
            System.out.println("🏁 [DB] Phiên kết thúc | auction_id=" + auctionId
                    + " | người thắng=" + winner);

        } catch (SQLException e) {
            System.err.println("❌ [DB] Lỗi đóng phiên: " + e.getMessage());
        }
    }

    public List<AuctionSession> getAllAuctions() {
        List<AuctionSession> list = new ArrayList<>();
        String sql = "SELECT * FROM auctions";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                AuctionSession s = new AuctionSession(
                        rs.getInt("item_id"),
                        rs.getInt("seller_id"),
                        rs.getDouble("current_price"),
                        rs.getTimestamp("start_time"),
                        rs.getTimestamp("end_time")
                );
                s.setAuctionId(rs.getInt("auction_id"));
                s.setHighestBidder(rs.getString("highest_bidder") != null
                        ? rs.getString("highest_bidder") : "Chưa có ai");
                s.setFinished(rs.getBoolean("is_finished"));
                list.add(s);
            }

        } catch (SQLException e) {
            System.err.println("❌ [DB] Lỗi lấy danh sách auctions: " + e.getMessage());
        }
        return list;
    }

    public AuctionSession findById(int auctionId) {
        String sql = "SELECT * FROM auctions WHERE auction_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    AuctionSession s = new AuctionSession(
                            rs.getInt("item_id"),
                            rs.getInt("seller_id"),
                            rs.getDouble("current_price"),
                            rs.getTimestamp("start_time"),
                            rs.getTimestamp("end_time")
                    );
                    s.setAuctionId(rs.getInt("auction_id"));
                    s.setHighestBidder(rs.getString("highest_bidder") != null
                            ? rs.getString("highest_bidder") : "Chưa có ai");
                    s.setFinished(rs.getBoolean("is_finished"));
                    return s;
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ [DB] Lỗi tìm auction: " + e.getMessage());
        }
        return null;
    }

    /**
     * Tìm phiên đấu giá MỚI NHẤT của một sản phẩm theo item_id.
     * Dùng khi session không có trong RAM (server restart / phiên đã kết thúc).
     */
    public AuctionSession findLatestByItemId(int itemId) {
        String sql = "SELECT * FROM auctions WHERE item_id = ? ORDER BY auction_id DESC LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    AuctionSession s = new AuctionSession(
                            rs.getInt("item_id"),
                            rs.getInt("seller_id"),
                            rs.getDouble("current_price"),
                            rs.getTimestamp("start_time"),
                            rs.getTimestamp("end_time")
                    );
                    s.setAuctionId(rs.getInt("auction_id"));
                    s.setHighestBidder(rs.getString("highest_bidder") != null
                            ? rs.getString("highest_bidder") : "Chưa có ai");
                    s.setFinished(rs.getBoolean("is_finished"));
                    return s;
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ [DB] Lỗi tìm auction theo item_id: " + e.getMessage());
        }
        return null;
    }
}