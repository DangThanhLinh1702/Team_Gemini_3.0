package auction.server.repository;

import auction.server.model.AuctionSession;
import auction.database.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AuctionRepository {

    public void saveAuction(AuctionSession newAuction) {
        // Câu lệnh SQL đã được cập nhật để bao gồm đầy đủ tất cả các cột của bảng auctions
        String query = "INSERT INTO auctions (item_id, seller_id, current_price, highest_bidder, is_finished, start_time, end_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query, java.sql.Statement.RETURN_GENERATED_KEYS)) {

            // 1. Map các thông tin cơ bản
            preparedStatement.setInt(1, newAuction.getItemId());
            preparedStatement.setInt(2, newAuction.getSellerId());
            preparedStatement.setDouble(3, newAuction.getCurrentPrice());

            // 2. Map thông tin khởi tạo (Khi mới tạo phiên: chưa có người thắng, chưa kết thúc)
            // Nếu trong Model AuctionSession của bạn có trường này, hãy dùng newAuction.getHighestBidder()
            preparedStatement.setString(4, null);
            preparedStatement.setBoolean(5, false); // Mặc định là FALSE giống cấu trúc DB của bạn

            // 3. Map thời gian
            preparedStatement.setTimestamp(6, newAuction.getStartTime());
            preparedStatement.setTimestamp(7, newAuction.getEndTime());

            // Thực thi lệnh lưu vào database
            preparedStatement.executeUpdate();

            // Lấy auction_id (AUTO_INCREMENT) vừa được tạo từ database và gán ngược lại vào đối tượng session
            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    newAuction.setAuctionId(generatedKeys.getInt(1));
                }
            }

            System.out.println("🚀 [DATABASE LOG] Đã lưu thành công phiên đấu giá vào bảng 'auctions' | ID: " + newAuction.getAuctionId());

        } catch (SQLException exception) {
            System.err.println("❌ Lỗi khi ghi log vào bảng auctions: " + exception.getMessage());
            exception.printStackTrace();
        }
    }

    public List<AuctionSession> getAllAuctions() {
        List<AuctionSession> auctions = new ArrayList<>();
        String query = "SELECT * FROM auctions";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            while (resultSet.next()) {
                AuctionSession auctionObject = new AuctionSession(
                        resultSet.getInt("auction_id"),
                        resultSet.getInt("item_id"),
                        resultSet.getInt("seller_id"),
                        resultSet.getTimestamp("start_time"),
                        resultSet.getTimestamp("end_time")
                );
                auctions.add(auctionObject);
            }

        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return auctions;
    }

    public AuctionSession findById(int id) {
        String query = "SELECT * FROM auctions WHERE auction_id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setInt(1, id);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                return new AuctionSession(
                        resultSet.getInt("auction_id"),
                        resultSet.getInt("item_id"),
                        resultSet.getInt("seller_id"),
                        resultSet.getTimestamp("start_time"),
                        resultSet.getTimestamp("end_time")
                );
            }

        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return null;
    }
}
