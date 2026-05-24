package auction.server.repository;

import auction.server.model.Item;
import auction.database.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;

public class ItemRepository {

    public ArrayList<Item> getAllItemsFromDatabase() {
        ArrayList<Item> items = new ArrayList<>();
        String query = "SELECT * FROM items";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(query);
             ResultSet resultSet = ps.executeQuery()) {

            while (resultSet.next()) {
                int id = resultSet.getInt("item_id");
                String name = resultSet.getString("name");
                String description = resultSet.getString("description");
                double startingPrice = resultSet.getDouble("starting_price");
                String sellerUsername = resultSet.getString("seller_username");

                // Cập nhật tên biến ở đây
                String image = resultSet.getString("image_data");
                long endTime = resultSet.getLong("end_time");

                Item itemObject = new Item(id, name, description, startingPrice, sellerUsername, image, endTime);
                items.add(itemObject);
            }
        } catch (SQLException exception) {
            System.err.println("❌ LỖI KHI LẤY DANH SÁCH ITEMS: " + exception.getMessage());
        }
        return items;
    }

    public void saveItem(Item item) {
        String query = "INSERT INTO items (name, description, starting_price, seller_username, image_data, end_time) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, item.getName());
            ps.setString(2, item.getDescription());
            ps.setDouble(3, item.getStartingPrice());
            ps.setString(4, item.getSellerUserName());

            // Đã đổi thành item.getImage()
            ps.setString(5, item.getImage());
            ps.setLong(6, item.getEndTime());

            ps.executeUpdate();

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    item.setId(generatedKeys.getInt(1));
                }
            }
        } catch (SQLException exception) {
            System.err.println("❌ LỖI KHI LƯU ITEM VÀO DB: " + exception.getMessage());
        }
    }

    public Item findLastInserted() {
        String query = "SELECT * FROM items ORDER BY item_id DESC LIMIT 1";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                int realId = rs.getInt("item_id");
                String name = rs.getString("name");
                String description = rs.getString("description");
                double startingPrice = rs.getDouble("starting_price");
                String sellerUsername = rs.getString("seller_username");
                String image = rs.getString("image_data");
                long endTime = rs.getLong("end_time");

                return new Item(realId, name, description, startingPrice, sellerUsername, image, endTime);
            }
        } catch (SQLException exception) {
            System.err.println("❌ LỖI KHI TÌM ITEM CUỐI: " + exception.getMessage());
        }
        return null;
    }

    public Item findById(int itemId) {
        String query = "SELECT * FROM items WHERE item_id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(query)) {

            ps.setInt(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String name = rs.getString("name");
                    String description = rs.getString("description");
                    double startingPrice = rs.getDouble("starting_price");
                    String sellerUsername = rs.getString("seller_username");
                    String image = rs.getString("image_data");
                    long endTime = rs.getLong("end_time");

                    return new Item(itemId, name, description, startingPrice, sellerUsername, image, endTime);
                }
            }
        } catch (SQLException exception) {
            System.err.println("❌ LỖI KHI TÌM ITEM THEO ID: " + exception.getMessage());
        }
        return null;
    }
}