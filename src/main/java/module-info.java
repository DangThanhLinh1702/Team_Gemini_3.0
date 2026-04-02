module auction.client {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;
    requires jdk.httpserver;

    opens auction.client.ui to javafx.fxml;

    exports auction.client;
    exports auction.client.ui;

    opens auction.shared.dto to com.google.gson;
    opens auction.server.model to com.google.gson;

}