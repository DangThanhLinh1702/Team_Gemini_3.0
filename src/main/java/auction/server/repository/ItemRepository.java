package auction.server.repository;

import auction.server.model.Item;
import auction.database.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class ItemRepository {
    // Danh sách tạm trong bộ nhớ
    private static final ArrayList<Item> allItems = new ArrayList<>();

    // Lưu item vào cả ArrayList và Database
    public void saveItem(Item item) {
        // Lưu vào ArrayList
        allItems.add(item);
        System.out.println("Đã thêm sản phẩm vào bộ nhớ: " + item.getName());

        // Lưu vào Database
        String query = "INSERT INTO items (name, description, starting_price) VALUES (?, ?, ?)";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setString(1, item.getName());
            preparedStatement.setString(2, item.getDescription());
            preparedStatement.setDouble(3, item.getStartingPrice());

            preparedStatement.executeUpdate();
            System.out.println("Đã thêm sản phẩm vào database: " + item.getName());

        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }
    public Item findById(int id) {
        String query = "SELECT * FROM items WHERE item_id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setInt(1, id);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                Item itemObject = new Item(
                        resultSet.getString("name"),
                        resultSet.getString("description"),
                        resultSet.getDouble("starting_price"),
                        resultSet.getString("seller_username")
                );
                itemObject.setId(resultSet.getInt("item_id"));
                return itemObject;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Lấy tất cả item từ ArrayList
    public ArrayList<Item> getAllItems() {
        return allItems;
    }
    public Item findLastInserted() {
        String query = "SELECT * FROM items ORDER BY item_id DESC LIMIT 1";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(query)) {

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Item item = new Item(
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getDouble("starting_price"),
                        rs.getString("seller_username")
                );
                item.setId(rs.getInt("item_id"));
                // nếu có seller_id trong bảng thì thêm:
                // item.setSellerId(rs.getInt("seller_id"));
                return item;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }



    // Lấy tất cả item từ Database
    public ArrayList<Item> getAllItemsFromDatabase() {
        ArrayList<Item> items = new ArrayList<>();
        String query = "SELECT * FROM items";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            while (resultSet.next()) {
                Item itemObject = new Item(
                        resultSet.getString("name"),
                        resultSet.getString("description"),
                        resultSet.getDouble("starting_price"),
                        resultSet.getString("seller_username") // thêm cột seller_username
                );
                itemObject.setId(resultSet.getInt("item_id")); // nếu Item có setId
                items.add(itemObject);
            }


        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return items;
    }
}
