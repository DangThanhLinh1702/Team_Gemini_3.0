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
        String query = "INSERT INTO auctions (item_id, seller_id, start_time, end_time) VALUES (?, ?, ?, ?)";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setInt(1, newAuction.getItemId());
            preparedStatement.setInt(2, newAuction.getSellerId());
            preparedStatement.setTimestamp(3, newAuction.getStartTime());
            preparedStatement.setTimestamp(4, newAuction.getEndTime());

            preparedStatement.executeUpdate();
            System.out.println("Đã thêm auction cho item_id: " + newAuction.getItemId());

        } catch (SQLException exception) {
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
