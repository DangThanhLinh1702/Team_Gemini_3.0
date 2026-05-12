package auction.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String URL = "jdbc:mysql://mysql-39eb3d8a-auctionteamgemini.l.aivencloud.com:25460/team_geminidb";
    private static final String URL_NO_DB = "jdbc:mysql://mysql-39eb3d8a-auctionteamgemini.l.aivencloud.com:25460";
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
    public static void main(String[] args) {
        try {
            // Thử kết nối với database, nếu không tồn tại thì tạo
            Connection conn;
            try {
                conn = getConnection();
                System.out.println("🎉 Kết nối MySQL (team_geminidb) thành công rực rỡ!");
            } catch (SQLException e) {
                if (e.getMessage().contains("Unknown database")) {
                    System.out.println("📝 Database chưa tồn tại, đang tạo mới...");
                    createDatabase();
                    conn = getConnection();
                    System.out.println("✅ Database đã được tạo và kết nối thành công!");
                } else {
                    throw e;
                }
            }
            
            // Kiểm tra và tạo bảng auction_items nếu chưa tồn tại
            createTableIfNotExists(conn);
            
            // Test thêm sản phẩm mẫu
            testInsertSample(conn);
            
            // Test Repository
            testRepository();
            
            // Test Server ItemService
            testServerItemService();
            
            conn.close(); // Test xong thì nhớ đóng kết nối
        } catch (SQLException e) {
            System.out.println("❌ Kết nối thất bại. Lỗi chi tiết: ");
            e.printStackTrace();
        }
    }
    
    private static void createDatabase() throws SQLException {
        try (Connection conn = DriverManager.getConnection(URL_NO_DB, USER, PASSWORD);
             var stmt = conn.createStatement()) {
            stmt.execute("CREATE DATABASE team_geminidb");
        }
    }
    
    private static void createTableIfNotExists(Connection conn) throws SQLException {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS auction_items (" +
            "id VARCHAR(36) PRIMARY KEY," +
            "name VARCHAR(255) NOT NULL," +
            "description TEXT," +
            "starting_price BIGINT NOT NULL," +
            "current_price BIGINT NOT NULL," +
            "seller_username VARCHAR(100) NOT NULL," +
            "current_bidder VARCHAR(100)," +
            "start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
            "end_time TIMESTAMP NOT NULL," +
            "status VARCHAR(20) DEFAULT 'ACTIVE'" +
            ")";
            
        try (var stmt = conn.createStatement()) {
            stmt.execute(createTableSQL);
            System.out.println("✅ Bảng auction_items đã sẵn sàng!");
        }
    }
    
    private static void testInsertSample(Connection conn) throws SQLException {
        String insertSQL = "INSERT INTO auction_items (id, name, description, starting_price, current_price, seller_username, end_time, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (var stmt = conn.prepareStatement(insertSQL)) {
            String sampleId = java.util.UUID.randomUUID().toString();
            stmt.setString(1, sampleId);
            stmt.setString(2, "iPhone 15 Pro Max");
            stmt.setString(3, "Điện thoại flagship mới nhất");
            stmt.setLong(4, 25000000);
            stmt.setLong(5, 25000000);
            stmt.setString(6, "testuser");
            stmt.setTimestamp(7, new java.sql.Timestamp(System.currentTimeMillis() + 7200000)); // 2 giờ
            stmt.setString(8, "ACTIVE");
            
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                System.out.println("✅ Test insert sản phẩm mẫu thành công! ID: " + sampleId);
            }
        }
    }
    
    private static void testRepository() {
        System.out.println("🧪 Testing AuctionRepository...");
        AuctionRepository repo = new AuctionRepository();
        
        try {
            // Test get active auctions
            var items = repo.getActiveAuctions();
            System.out.println("✅ Get active auctions: " + items.size() + " items");
            
            if (!items.isEmpty()) {
                var firstItem = items.get(0);
                System.out.println("   - First item: " + firstItem.getName() + " (ID: " + firstItem.getId() + ")");
            }
            
            // Test add new item
            auction.shared.model.AuctionItem newItem = new auction.shared.model.AuctionItem();
            newItem.setId(java.util.UUID.randomUUID().toString());
            newItem.setName("Test Product from Repository");
            newItem.setDescription("Test description");
            newItem.setStartingPrice(1000000L);
            newItem.setCurrentPrice(1000000L);
            newItem.setSellerUsername("testuser");
            newItem.setStartTime(java.time.LocalDateTime.now());
            newItem.setEndTime(java.time.LocalDateTime.now().plusHours(2));
            newItem.setStatus("ACTIVE");
            
            boolean added = repo.addAuctionItem(newItem);
            System.out.println("✅ Add new item: " + (added ? "SUCCESS" : "FAILED"));
            
        } catch (Exception e) {
            System.out.println("❌ Repository test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testServerItemService() {
        System.out.println("🧪 Testing Server ItemService...");
        auction.server.service.ItemService itemService = new auction.server.service.ItemService();
        
        try {
            // Test add item via ItemService
            String result = itemService.addItem("MacBook Pro M3", "Laptop mới nhất", 45000000.0, "selleruser");
            System.out.println("✅ ItemService.addItem: " + result);
            
            // Test get all items
            var items = itemService.getAllItem();
            System.out.println("✅ ItemService.getAllItem: " + items.size() + " items");
            
            if (!items.isEmpty()) {
                var lastItem = itemService.getLastInsertedItem();
                if (lastItem != null) {
                    System.out.println("   - Last item: " + lastItem.getName() + " (ID: " + lastItem.getId() + ")");
                }
            }
            
        } catch (Exception e) {
            System.out.println("❌ ItemService test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
