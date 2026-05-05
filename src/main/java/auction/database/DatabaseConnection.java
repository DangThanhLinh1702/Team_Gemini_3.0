package auction.database;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConnection {
    // Tạo đối tượng Properties để chứa các thông số
    private static final Properties properties = new Properties();

    // Khối static này tự động chạy để đọc file database.properties
    static {
        try (InputStream input = DatabaseConnection.class.getClassLoader().getResourceAsStream("database.properties")) {
            if (input == null) {
                System.out.println("❌ Không tìm thấy file database.properties trong thư mục resources!");
            } else {
                properties.load(input); // Tải dữ liệu từ file vào biến
            }
        } catch (Exception ex) {
            System.out.println("❌ Lỗi khi đọc file cấu hình: " + ex.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        // Lấy các thông số từ biến properties (đã đọc từ file) để kết nối
        String url = properties.getProperty("db.url");
        String user = properties.getProperty("db.user");
        String password = properties.getProperty("db.password");
        return DriverManager.getConnection(url, user, password);
    }

    public static void main(String[] args) {
        try {
            Connection conn = getConnection();
            if (conn != null) {
                // Mình đổi lại chữ cho đúng với database thực tế là Aiven / defaultdb nhé
                System.out.println("🎉 Kết nối MySQL (Aiven Cloud - defaultdb) thành công rực rỡ!");
                conn.close(); // Test xong thì nhớ đóng kết nối
            }
        } catch (SQLException e) {
            System.out.println("❌ Kết nối thất bại. Lỗi chi tiết: ");
            e.printStackTrace();
        }
    }
}