package auction.server.core;

import auction.server.handler.AuthHandler;
import auction.server.handler.ItemHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class ServerMain {
    public static void main(String[] args) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(8088), 0);
            AuthHandler authHandler = new AuthHandler();
            ItemHandler itemHandler = new ItemHandler();

            server.createContext("/login", authHandler);
            server.createContext("/register", authHandler);
            server.createContext("/users", authHandler);
            server.createContext("/items", itemHandler);

            server.start();
            System.out.println("SERVER STARTING");

        } catch (IOException e) {
            System.out.println("Lỗi khi khởi động Server: " + e.getMessage());
        }
    }
}