module auction.client { // Hoặc tên module bạn đặt
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;
    requires jdk.httpserver;

    // Dòng này cực kỳ quan trọng:
    // Cho phép FXMLLoader truy cập vào các class UI để nạp file FXML
    opens auction.client.ui to javafx.fxml;

    // Nếu bạn có controller ở package khác, cũng phải opens package đó
    // opens auction.client.controller to javafx.fxml;

    exports auction.client;
    exports auction.client.ui;
}