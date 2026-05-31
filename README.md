# 🏷️ HỆ THỐNG ĐẤU GIÁ TRỰC TUYẾN (AUCTION APP) - BÀI TẬP LỚN LTNC

## 1. Mô tả ngắn gọn bài toán và phạm vi hệ thống
- **Bài toán:** Xây dựng một ứng dụng đấu giá trực tuyến hoạt động theo mô hình Client-Server. Ứng dụng cho phép nhiều người dùng kết nối đồng thời để đăng bán tài sản và tham gia trả giá (bidding) cạnh tranh theo thời gian thực.
- **Phạm vi hệ thống:** - Hệ thống cung cấp hai vai trò chính: Người bán (Seller) và Người mua (Bidder).
    - Hỗ trợ xử lý đa luồng (Multi-threading) và giao tiếp mạng theo thời gian thực.
    - Quản lý các phiên đấu giá chặt chẽ với đồng hồ đếm ngược tự động và lưu trữ toàn bộ lịch sử trả giá.

## 2. Công nghệ sử dụng, môi trường chạy và yêu cầu cài đặt
- **Ngôn ngữ lập trình:** Java
- **Giao diện người dùng (GUI):** JavaFX 21
- **Giao tiếp mạng & Real-time:** WebSocket (`org.java-websocket:1.5.3`)
- **Bảo mật & Xác thực:** JSON Web Token - JWT (`com.auth0:java-jwt:4.5.1`)
- **Xử lý dữ liệu:** Gson (`com.google.code.gson:2.10.1`)
- **Cơ sở dữ liệu:** MySQL (Được triển khai trực tuyến trên Aiven Cloud)
- **Công cụ Build:** Gradle (Kotlin DSL)
- **Yêu cầu cài đặt:**
    - Máy tính cần cài đặt sẵn **JDK 17** (hoặc JDK 21).
    - **Không cần cài đặt Database local**: Ứng dụng đã được cấu hình tự động kết nối với MySQL Cloud. Chỉ cần đảm bảo máy tính có kết nối Internet ổn định khi khởi chạy ứng dụng.

## 3. Cấu trúc thư mục và các module chính
Dự án được chia thành 3 module logic chính: `client`, `server`, và `shared`.

```
Team_Gemini_3.0/
├── src/main/java/auction/
│   ├── client/               # Module Client (Giao diện và Logic người dùng)
│   │   ├── controller/       # Xử lý sự kiện giao diện (AuctionController, PostItemController...)
│   │   ├── network/          # Giao tiếp mạng (WebSocketClient, AuthClient)
│   │   ├── ui/               # Giao diện hiển thị (View)
│   │   └── ClientMain.java   # Lớp khởi chạy ứng dụng Client (Entry point)
│   ├── server/               # Module Server (Core logic và truy xuất DB)
│   │   ├── core/             # Quản lý Server và các phiên đấu giá
│   │   ├── handler/          # Xử lý các yêu cầu (Request) từ Client
│   │   ├── model/            # Cấu trúc dữ liệu và thực thể
│   │   ├── repository/       # Giao tiếp trực tiếp với Cơ sở dữ liệu
│   │   └── ServerMain.java   # Lớp khởi chạy Server (Entry point)
│   └── shared/               # Module dùng chung
│       ├── dto/              # Các đối tượng truyền tải dữ liệu giữa Client-Server
│       └── util/             # Các lớp tiện ích hỗ trợ (Json, Jwt)
├── src/main/resources/
│   ├── auction/client/ui/    # Chứa thiết kế giao diện tĩnh (.fxml)
│   └── database.properties   # Cấu hình Database Cloud
└── build.gradle.kts          # Cấu hình dependencies và quản lý project
```

## 4. Vị trí các file jar
Đường dẫn: Team_Gemini_3.0/build/libs/

File Server: AuctionApp-1.0-server.jar

File Client: AuctionApp-1.0-client.jar

## 5. Hướng dẫn chạy Server / Client
**Lưu ý:** Bắt buộc khởi chạy Server trước Client.

- **Bước 1: Khởi chạy Server**
  Mở Terminal/CMD và gõ lần lượt các lệnh sau:
  ```
  cd Team_Gemini_3.0/build/libs
  java -jar AuctionApp-1.0-server.jar
  ```

- **Bước 2: Khởi chạy Client**
  ```
  cd Team_Gemini_3.0/build/libs
  java -jar AuctionApp-1.0-client.jar
  ```


## 6. Danh sách chức năng đã hoàn thành
* Quản lý tài khoản: Đăng ký, Đăng nhập, Phân quyền người dùng (Cho phép chuyển đổi linh hoạt vai trò Seller/Bidder).

* Tính năng của Seller: Đăng tải sản phẩm mới kèm giá khởi điểm, thời lượng đấu giá và upload hình ảnh (Hình ảnh được mã hóa an toàn dưới dạng Base64).

* Tính năng của Bidder: Xem danh sách sản phẩm đang đấu giá, nhập số tiền và tham gia trả giá (Bid).

* Xử lý thời gian thực (Real-time):

- Cập nhật tự động trạng thái giá cao nhất hiện tại cho toàn bộ các Client.

- Hiển thị danh sách người đang dẫn đầu phiên đấu giá.

- Đồng hồ đếm ngược thời gian kết thúc phiên đấu giá chuẩn xác.

- Lưu trữ dữ liệu: Lưu trữ an toàn thông tin người dùng, chi tiết sản phẩm và toàn bộ lịch sử các lần trả giá vào Database.

## 7. Link PDF và video demo
