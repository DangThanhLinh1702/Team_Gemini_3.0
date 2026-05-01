package auction.server.core;

import auction.server.handler.AuthHandler;
import auction.server.handler.ItemHandler;
import auction.server.handler.ItemListHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class ServerMain {
    private static AuctionWebSocketServer wsServerRef;

    public static AuctionWebSocketServer getWsServerRef() {
        return wsServerRef;
    }

    public static void main(String... args) {
        try {
            // ========== KHỞI ĐỘNG HTTP SERVER (Port 8080) ==========
            HttpServer httpServer = HttpServer.create(new InetSocketAddress(8080), 0);
            AuthHandler authHandler = new AuthHandler();
            ItemListHandler itemListHandler = new ItemListHandler();
            ItemHandler itemHandler = new ItemHandler();

            // Các endpoints
            httpServer.createContext("/login", authHandler);
            httpServer.createContext("/register", authHandler);
            httpServer.createContext("/users", authHandler);
            httpServer.createContext("/change-role", authHandler);
            httpServer.createContext("/items", itemHandler);         // GET + POST
            httpServer.createContext("/auctions", itemListHandler);  // GET

            httpServer.setExecutor(Executors.newCachedThreadPool());
            httpServer.start();

            System.out.println("✓ HTTP Server started on port 8080");
            System.out.println("  Endpoints:");
            System.out.println("    - POST /login");
            System.out.println("    - POST /register");
            System.out.println("    - GET  /users");
            System.out.println("    - GET  /items");
            System.out.println("    - POST /items (SELLER auth)");
            System.out.println("    - GET  /auctions");

            // ========== KHỞI ĐỘNG WEBSOCKET SERVER (Port 8081) ==========
            AuctionWebSocketServer wsServer = new AuctionWebSocketServer(8081);
            wsServerRef = wsServer;
            wsServer.start();

            System.out.println("\n✓ WebSocket Server started on port 8081");
            System.out.println("  Actions: JOIN, BID");

            // 🌟 TẠO SẢN PHẨM MẪU (DUMMY DATA) TẠI ĐÂY 🌟
            // Giả sử AuctionManager của bạn có hàm tạo phiên đấu giá tên là createSession
            // Nếu code báo đỏ, bạn hãy mở class AuctionManager ra kiểm tra xem tên hàm là gì (VD: addSession, createAuction...) rồi sửa lại nhé!

            System.out.println("✓ Đã khởi tạo sản phẩm mẫu trên RAM: Mã [SP01], Giá khởi điểm [10000]");

            System.out.println("SERVER RUNNING SUCCESSFULLY!");

        } catch (IOException e) {
            System.err.println("Lỗi khi khởi động Server: " + e.getMessage());
            System.err.println("Stack trace: " + e);
        }
    }
}
