package auction.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String URL = "jdbc:mysql://mysql-39eb3d8a-auctionteamgemini.l.aivencloud.com:25460/team_geminidb";
    private static final  String USER = "avnadmin";
    private static final String PASSWORD = "AVNS_XKpOX5CIOktTk60S0pl";

    static {
        try {
            // Đăng ký MySQL driver thủ công
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("Không tìm thấy MySQL JDBC Driver: " + e.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
