package auction.server.repository;

import auction.server.model.Item;
import auction.database.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;

public class ItemRepository {
    private static final ArrayList<Item> allItems = new ArrayList<>();

    public void saveItem(Item item) {
        String query = "INSERT INTO items (name, description, starting_price, seller_username) VALUES (?, ?, ?, ?)";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {

            preparedStatement.setString(1, item.getName());
            preparedStatement.setString(2, item.getDescription());
            preparedStatement.setDouble(3, item.getStartingPrice());

            // Nếu seller_username bị null hoặc rỗng, gán tạm giá trị mặc định để không bị lỗi DB
            String seller = (item.getSellerUserName() == null || item.getSellerUserName().isEmpty()) ? "Unknown" : item.getSellerUserName();
            preparedStatement.setString(4, seller);

            preparedStatement.executeUpdate();

            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int generatedId = generatedKeys.getInt(1);
                    item.setId(generatedId);
                }
            }

            allItems.add(item);
            System.out.println("====== [DATABASE] Đã lưu thành công sản phẩm: " + item.getName() + " (ID: " + item.getId() + ") ======");

        } catch (SQLException exception) {
            System.err.println("❌ LỖI DATABASE KHI LƯU ITEM: " + exception.getMessage());
            exception.printStackTrace();
        }
    }

    public ArrayList<Item> getAllItemsFromDatabase() {
        ArrayList<Item> items = new ArrayList<>();
        String query = "SELECT * FROM items";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            while (resultSet.next()) {
                Item itemObject = new Item(
                        resultSet.getInt("item_id"),
                        resultSet.getString("name"),
                        resultSet.getString("description"),
                        resultSet.getDouble("starting_price"),
                        resultSet.getString("seller_username")
                );
                items.add(itemObject);
            }
        } catch (SQLException exception) {
            System.err.println("❌ LỖI KHI LẤY DANH SÁCH ITEMS: " + exception.getMessage());
        }
        return items;
    }

    public Item findLastInserted() {
        // ĐÃ SỬA: Sắp xếp theo item_id thay vì start_time không tồn tại
        String query = "SELECT * FROM items ORDER BY item_id DESC LIMIT 1";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(query)) {

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Item(
                        rs.getInt("item_id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getDouble("starting_price"),
                        rs.getString("seller_username")
                );
            }
        } catch (SQLException e) {
            System.err.println("❌ LỖI LẤY ITEM CUỐI CÙNG: " + e.getMessage());
        }
        return null;
    }

    public Item findById(int id) {
        String query = "SELECT * FROM items WHERE item_id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setInt(1, id);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                return new Item(
                        resultSet.getInt("item_id"),
                        resultSet.getString("name"),
                        resultSet.getString("description"),
                        resultSet.getDouble("starting_price"),
                        resultSet.getString("seller_username")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public ArrayList<Item> getAllItems() {
        return getAllItemsFromDatabase();
    }
}