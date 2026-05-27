package auction.server.repository;

import auction.server.model.AuctionSession;
import auction.database.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository xử lý tất cả thao tác DB liên quan đến bảng auctions.
 * Bao gồm: lưu phiên mới, cập nhật giá khi bid, đóng phiên khi kết thúc.
 */
public class AuctionRepository {

    /**
     * Lưu phiên đấu giá mới vào DB khi vừa tạo phiên.
     * Sau khi lưu, tự động gán auction_id (AUTO_INCREMENT) ngược lại vào session.
     */
    public void saveAuction(AuctionSession session) {
        String sql = "INSERT INTO auctions (item_id, seller_id, current_price, highest_bidder, is_finished, start_time, end_time) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, session.getItemId());
            ps.setInt(2, session.getSellerId());
            ps.setDouble(3, session.getCurrentPrice());
            ps.setString(4, session.getHighestBidder()); // "Chưa có ai" lúc khởi tạo
            ps.setBoolean(5, false);
            ps.setTimestamp(6, session.getStartTime());
            ps.setTimestamp(7, session.getEndTime());

            ps.executeUpdate();

            // Lấy ID vừa được tạo và gán lại vào session
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    session.setAuctionId(keys.getInt(1));
                }
            }

            System.out.println("✅ [DB] Lưu phiên đấu giá mới | auction_id=" + session.getAuctionId()
                    + " | item_id=" + session.getItemId());

        } catch (SQLException e) {
            System.err.println("❌ [DB] Lỗi lưu phiên đấu giá: " + e.getMessage());
        }
    }

    /**
     * Cập nhật giá hiện tại và người dẫn đầu sau mỗi lần đặt giá thành công.
     * Gọi sau mỗi placeBid() thành công để đảm bảo DB luôn đồng bộ với RAM.
     */
    public void updateBid(int auctionId, double currentPrice, String highestBidder) {
        String sql = "UPDATE auctions SET current_price = ?, highest_bidder = ? WHERE auction_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDouble(1, currentPrice);
            ps.setString(2, highestBidder);
            ps.setInt(3, auctionId);

            ps.executeUpdate();
            System.out.println("✅ [DB] Cập nhật giá | auction_id=" + auctionId
                    + " | giá=" + String.format("%,.0f", currentPrice)
                    + " | người dẫn đầu=" + highestBidder);

        } catch (SQLException e) {
            System.err.println("❌ [DB] Lỗi cập nhật giá: " + e.getMessage());
        }
    }

    /**
     * Đánh dấu phiên kết thúc trong DB (is_finished = true, lưu kết quả cuối).
     * Gọi khi timer hết giờ hoặc admin kết thúc sớm.
     */
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
                    + " | người thắng=" + winner
                    + " | giá cuối=" + String.format("%,.0f", finalPrice));

        } catch (SQLException e) {
            System.err.println("❌ [DB] Lỗi đóng phiên đấu giá: " + e.getMessage());
        }
    }

    /**
     * Lấy toàn bộ lịch sử đấu giá từ DB (dùng cho báo cáo / admin).
     */
    public List<AuctionSession> getAllAuctions() {
        List<AuctionSession> list = new ArrayList<>();
        String sql = "SELECT * FROM auctions";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                // Tạo session từ DB với đầy đủ thông tin
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

    /**
     * Tìm phiên đấu giá theo auction_id.
     */
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
            System.err.println("❌ [DB] Lỗi tìm auction theo id: " + e.getMessage());
        }
        return null;
    }
}
