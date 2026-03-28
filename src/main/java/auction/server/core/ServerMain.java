package auction.server.core;

import auction.server.handler.AuthHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class ServerMain {
    public static void main(String[] args) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
            AuthHandler authHandler = new AuthHandler();

            server.createContext("/login", authHandler);
            server.createContext("/register", authHandler);
            server.createContext("/users", authHandler);

            server.start();
            System.out.println("SERVER ĐÃ KHỞI ĐỘNG");

        } catch (IOException e) {
            System.out.println("Lỗi khi khởi động Server: " + e.getMessage());
        }
    }
}