package auction.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String URL = "jdbc:mysql://127.0.0.1:3306/team_geminidb";
    private static final  String USER = "root";
    private static final String PASSWORD = "1702@2007";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
    public static void main(String[] args) {
        try {
            Connection conn = getConnection();
            if (conn != null) {
                System.out.println("🎉 Kết nối MySQL (team_geminidb) thành công rực rỡ!");
                conn.close(); // Test xong thì nhớ đóng kết nối
            }
        } catch (SQLException e) {
            System.out.println("❌ Kết nối thất bại. Lỗi chi tiết: ");
            e.printStackTrace();
        }
    }
}
