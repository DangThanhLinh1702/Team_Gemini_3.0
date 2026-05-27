package auction.server.repository;

import auction.server.model.User;
import auction.server.model.Admin;
import auction.server.model.Bidder;
import auction.server.model.Seller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import auction.database.DatabaseConnection;

public class UserRepository {

    // Kiểm tra username đã tồn tại chưa
    public boolean isUsernameExist(String username) {
        String query = "SELECT 1 FROM users WHERE username = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setString(1, username);
            ResultSet resultSet = preparedStatement.executeQuery();
            return resultSet.next();

        } catch (SQLException exception) {
            exception.printStackTrace();
            return false;
        }
    }

    // Lưu user mới vào DB
    public void saveUser(User newUser) {
        String query = "INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?)";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setString(1, newUser.getUsername());
            preparedStatement.setString(2, newUser.getPassword()); // giả sử password đã được hash
            preparedStatement.setString(3, newUser.getRole());

            preparedStatement.executeUpdate();
            System.out.println("Đã thêm: " + newUser.getUsername() + " - Quyền: " + newUser.getRole());

        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    // Kiểm tra đăng nhập
    public boolean checkLogin(String username, String password) {
        String query = "SELECT * FROM users WHERE username = ? AND password_hash = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setString(1, username);
            preparedStatement.setString(2, password);

            ResultSet resultSet = preparedStatement.executeQuery();
            return resultSet.next();

        } catch (SQLException exception) {
            exception.printStackTrace();
            return false;
        }
    }

    // Trả về object User nếu đăng nhập đúng
    public User authenticate(String username, String password) {
        String query = "SELECT * FROM users WHERE username = ? AND password_hash = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setString(1, username);
            preparedStatement.setString(2, password);

            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                String role = resultSet.getString("role");
                User userObject;
                switch (role) {
                    case "ADMIN":
                        userObject = new Admin(resultSet.getString("username"), resultSet.getString("password_hash"));
                        break;
                    case "SELLER":
                        userObject = new Seller(resultSet.getString("username"), resultSet.getString("password_hash"));
                        break;
                    case "BIDDER":
                        userObject = new Bidder(resultSet.getString("username"), resultSet.getString("password_hash"));
                        break;
                    default:
                        userObject = null;
                }
                if (userObject != null) {
                    userObject.setId(resultSet.getInt("user_id"));
                    return userObject;
                }
            }

        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return null;
    }

    // Lấy tất cả user từ DB
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String query = "SELECT * FROM users";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            while (resultSet.next()) {
                String role = resultSet.getString("role");
                User userObject;
                switch (role) {
                    case "ADMIN":
                        userObject = new Admin(resultSet.getString("username"), resultSet.getString("password_hash"));
                        break;
                    case "SELLER":
                        userObject = new Seller(resultSet.getString("username"), resultSet.getString("password_hash"));
                        break;
                    case "BIDDER":
                        userObject = new Bidder(resultSet.getString("username"), resultSet.getString("password_hash"));
                        break;
                    default:
                        userObject = null;
                }
                if (userObject != null) {
                    userObject.setId(resultSet.getInt("user_id"));
                    users.add(userObject);
                }
            }

        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return users;
    }

    // Xóa user theo username (ADMIN dùng)
    public boolean deleteUser(String username) {
        String query = "DELETE FROM users WHERE username = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, username);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Block / Unblock user (thêm cột is_blocked nếu chưa có, rồi cập nhật)
    public boolean setUserBlocked(String username, boolean blocked) {
        // Đảm bảo cột tồn tại (idempotent - chạy nhiều lần không sao)
        ensureBlockedColumn();
        String query = "UPDATE users SET is_blocked = ? WHERE username = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setBoolean(1, blocked);
            ps.setString(2, username);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean isUserBlocked(String username) {
        ensureBlockedColumn();
        String query = "SELECT is_blocked FROM users WHERE username = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getBoolean("is_blocked");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Tự động thêm cột is_blocked nếu chưa có trong DB
    private void ensureBlockedColumn() {
        String alterQuery = "ALTER TABLE users ADD COLUMN IF NOT EXISTS is_blocked BOOLEAN NOT NULL DEFAULT FALSE";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(alterQuery)) {
            ps.executeUpdate();
        } catch (SQLException e) {
            // MySQL không hỗ trợ IF NOT EXISTS cho ALTER TABLE trong mọi phiên bản
            // Thử cách khác: kiểm tra trước
            try {
                String checkQuery = "SELECT is_blocked FROM users LIMIT 1";
                try (Connection connection = DatabaseConnection.getConnection();
                     PreparedStatement ps2 = connection.prepareStatement(checkQuery)) {
                    ps2.executeQuery(); // Nếu không lỗi => cột đã tồn tại
                }
            } catch (SQLException e2) {
                // Cột chưa tồn tại, thêm vào
                String fallback = "ALTER TABLE users ADD COLUMN is_blocked BOOLEAN NOT NULL DEFAULT FALSE";
                try (Connection connection = DatabaseConnection.getConnection();
                     PreparedStatement ps3 = connection.prepareStatement(fallback)) {
                    ps3.executeUpdate();
                    System.out.println("✅ Đã thêm cột is_blocked vào bảng users");
                } catch (SQLException e3) {
                    // Bỏ qua nếu cột đã tồn tại
                }
            }
        }
    }

    // Tìm user theo ID
    public User findById(int id) {
        String query = "SELECT * FROM users WHERE user_id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setInt(1, id);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                String role = resultSet.getString("role");
                User userObject;
                switch (role) {
                    case "ADMIN":
                        userObject = new Admin(resultSet.getString("username"), resultSet.getString("password_hash"));
                        break;
                    case "SELLER":
                        userObject = new Seller(resultSet.getString("username"), resultSet.getString("password_hash"));
                        break;
                    case "BIDDER":
                        userObject = new Bidder(resultSet.getString("username"), resultSet.getString("password_hash"));
                        break;
                    default:
                        userObject = null;
                }
                if (userObject != null) {
                    userObject.setId(resultSet.getInt("user_id"));
                    return userObject;
                }
            }

        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return null;
    }
}
