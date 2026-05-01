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

    public boolean updateUserRole(String username, String newRole) {
        String query = "UPDATE users SET role = ? WHERE username = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, newRole);
            preparedStatement.setString(2, username);
            return preparedStatement.executeUpdate() > 0;
        } catch (SQLException exception) {
            exception.printStackTrace();
            return false;
        }
    }
}
