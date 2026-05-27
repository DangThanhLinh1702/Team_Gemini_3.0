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
                // 1. Khởi tạo đối tượng rỗng
                Item itemObject = new Item();

                // 2. Dùng các hàm SET để gán chính xác từng giá trị, không bao giờ lo nhầm!
                itemObject.setId(resultSet.getInt("item_id"));
                itemObject.setName(resultSet.getString("name"));
                itemObject.setDescription(resultSet.getString("description"));

                // Gán giá tiền (Client sẽ tự đọc trường này để hiện lên UI)
                itemObject.setStartingPrice(resultSet.getDouble("starting_price"));

                itemObject.setSellerUserName(resultSet.getString("seller_username"));

                // Gán ảnh (Đảm bảo tên cột trong get.. là "image_data" đúng như DB của bạn)
                itemObject.setImage(resultSet.getString("image_data"));

                itemObject.setEndTime(resultSet.getLong("end_time"));

                // 3. Thêm vào danh sách
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

    // ➕ ĐÃ THÊM: Hàm XÓA sản phẩm theo ID cho Admin
    public boolean deleteItemById(int itemId) {
        String query = "DELETE FROM items WHERE item_id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(query)) {

            ps.setInt(1, itemId);
            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException exception) {
            System.err.println("❌ LỖI KHI XÓA ITEM THEO ID: " + exception.getMessage());
            return false;
        }
    }

    // ➕ ĐÃ THÊM: Hàm CẬP NHẬT sản phẩm theo ID cho Admin (Đã chỉnh theo cột image_data)
    public boolean updateItem(int itemId, String name, String description, double startingPrice, String imageBase64) {
        String query = "UPDATE items SET name = ?, description = ?, starting_price = ?, image_data = ? WHERE item_id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(query)) {

            ps.setString(1, name);
            ps.setString(2, description);
            ps.setDouble(3, startingPrice);
            ps.setString(4, imageBase64 != null ? imageBase64 : "");
            ps.setInt(5, itemId);

            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException exception) {
            System.err.println("❌ LỖI KHI CẬP NHẬT ITEM: " + exception.getMessage());
            return false;
        }
    }
}