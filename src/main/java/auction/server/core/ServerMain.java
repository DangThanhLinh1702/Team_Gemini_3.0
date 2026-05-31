package auction.server.core;

import auction.server.handler.AuthHandler;
import auction.server.handler.ItemHandler;
import auction.server.handler.ItemListHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class ServerMain {
    public static void main(String... args) {
        try {
            // ========== HTTP SERVER — Port 8080 ==========
            HttpServer httpServer = HttpServer.create(new InetSocketAddress(8080), 0);
            httpServer.createContext("/login",    new AuthHandler());
            httpServer.createContext("/register", new AuthHandler());
            httpServer.createContext("/users",    new AuthHandler());
            httpServer.createContext("/me",       new AuthHandler());
            httpServer.createContext("/items",    new ItemHandler());
            httpServer.createContext("/auctions", new ItemListHandler());
            httpServer.setExecutor(Executors.newCachedThreadPool());
            httpServer.start();
            System.out.println("✓ HTTP Server đang chạy tại cổng 8080");

            // ========== WEBSOCKET SERVER — Port 8081 ==========
            AuctionWebSocketServer wsServer = new AuctionWebSocketServer(8081);
            wsServer.start();
            System.out.println("✓ WebSocket Server đang chạy tại cổng 8081");

            System.out.println("\n========================================");
            System.out.println("  SERVER KHỞI ĐỘNG THÀNH CÔNG!");
            System.out.println("  HTTP  : http://localhost:8080");
            System.out.println("  WS    : ws://localhost:8081");
            System.out.println("========================================");

        } catch (IOException e) {
            System.err.println("❌ Lỗi khởi động Server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
