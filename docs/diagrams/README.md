# Sơ đồ UML - Hệ thống Đấu giá Trực tuyến

## 📋 Mô tả tổng quát

Bộ sơ đồ này mô tả kiến trúc và luồng hoạt động của hệ thống đấu giá trực tuyến (Online Auction System) với WebSocket real-time.

---

## 1. 📐 Class Diagram (Sơ đồ lớp)

### Tệp: `ClassDiagram.puml`

#### Các thành phần chính:

**Phân cấp người dùng:**
- `Entity` (lớp cha abstract)
  - `User` (lớp abstract)
    - `Bidder` - Người tham gia đấu giá
    - `Seller` - Người bán hàng
    - `Admin` - Quản trị viên

**Mô hình dữ liệu:**
- `Item` - Sản phẩm cần đấu giá
  - id, name, description, startingPrice, sellerUserName
- `AuctionSession` - Phiên đấu giá
  - Quản lý giá hiện tại, người chiến thắng, trạng thái

**Thành phần máy chủ:**
- `AuctionManager` (Singleton)
  - Quản lý tất cả các phiên đấu giá hoạt động
  - Lập lịch tự động kết thúc phiên đấu giá
  - Gọi callback khi phiên kết thúc
- `AuctionWebSocketServer`
  - Xử lý kết nối WebSocket từ các client
  - Nhận yêu cầu tham gia phòng (JOIN) và đặt giá (BID)
  - Broadcast cập nhật giá đến tất cả client trong phòng

**DTO (Data Transfer Object):**
- `WebSocketRequestDTO` - Dữ liệu yêu cầu từ client

---

## 2. 🔄 Sequence Diagram (Sơ đồ tuần tự)

### Tệp: `SequenceDiagram.puml`

Mô tả quy trình " **Bidding Process** " gồm 4 giai đoạn:

### **Giai đoạn 1: Đăng nhập (Login Phase)**
```
Bidder → Client UI → WebSocket Server
  ↓ (Xác minh + Tạo JWT)
Server → Client UI → Bidder (Trả về token)
```

### **Giai đoạn 2: Duyệt sản phẩm (Browse Items Phase)**
```
Client UI → AuctionManager → AuctionSession
  ↓ (Lấy danh sách phiên đấu giá hoạt động)
Server → Client UI → Bidder (Hiển thị danh sách)
```

### **Giai đoạn 3: Tham gia phòng (Join Auction Room)**
```
Bidder → Client UI → WebSocket Server
  ↓ (Gửi JOIN + itemId + JWT)
Server → AuctionManager → Verify & Get Session
  ↓ (Trở về thông tin phiên)
Server → Client UI → Bidder (Hiển thị giá hiện tại)
```

### **Giai đoạn 4: Đặt giá (Placing Bid)**
```
Bidder → Client UI → WebSocket Server
  ↓ (Gửi BID + price + JWT)
Server → AuctionSession.placeBid()
  ↓ Hai khả năng:
  
  ✅ Thành công: 
    - Cập nhật currentPrice & highestBidder
    - Broadcast đến tất cả client trong phòng
    - Hiển thị giá mới
    
  ❌ Thất bại:
    - Giá < giá hiện tại
    - Hoặc phiên đã kết thúc
    - Gửi thông báo lỗi
```

### **Giai đoạn 5: Kết thúc đấu giá (Auto-triggered)**
```
AuctionManager (sau timeout)
  ↓ finishAuction()
Broadcast AUCTION_ENDED → Tất cả client
  ↓
Hiển thị người thắng & giá cuối cùng
```

---

## 3. 👥 Use Case Diagram (Sơ đồ trường hợp sử dụng)

### Tệp: `UseCaseDiagram.puml`

#### **Các diễn viên (Actors):**

1. **Bidder** - Người tham gia đấu giá
2. **Seller** - Người bán hàng
3. **Admin** - Quản trị viên hệ thống
4. **System** - Quy trình tự động

#### **Use Cases chi tiết:**

**Cho Bidder:**
- `Register Account` - Đăng ký tài khoản
- `Login` - Đăng nhập
- `Browse Available Items` - Duyệt sản phẩm có sẵn
- `View Item Details` - Xem chi tiết sản phẩm
- `Join Auction Room` - Tham gia phòng đấu giá
- `Place Bid` - Đặt giá
- `View Bid History` - Xem lịch sử đấu giá
- `Receive Bid Notification` - Nhận thông báo cập nhật giá
- `Get Auction Results` - Xem kết quả phiên đấu giá

**Cho Seller:**
- `Register Account` - Đăng ký tài khoản
- `Login` - Đăng nhập
- `Create Item` - Tạo sản phẩm cần bán
- `Set Starting Price` - Đặt giá khởi điểm
- `Monitor Item Auction` - Theo dõi phiên đấu giá
- `View Winner` - Xem người thắng

**Cho Admin:**
- `Login` - Đăng nhập
- `Manage Users` - Quản lý người dùng
- `View All Auctions` - Xem tất cả phiên đấu giá
- `Manage System Settings` - Quản lý cài đặt hệ thống

**Cho System (tự động):**
- `Start Auction` - Khởi động phiên đấu giá
- `Schedule End Time` - Lập lịch kết thúc
- `End Auction` - Kết thúc phiên
- `Calculate Winner` - Tính toán người thắng
- `Broadcast Result` - Phát sóng kết quả

#### **Mối quan hệ:**
- `<<include>>` - Use case này luôn được bao gồm
- `<<extend>>` - Use case này có thể mở rộng cái khác
- Ví dụ: `Place Bid` luôn bao gồm `Join Auction Room`

---

## 4. 🔧 Công nghệ được sử dụng

- **Backend:** Java, WebSocket (Java-WebSocket)
- **Authentication:** JWT (JSON Web Token)
- **Concurrency:** ConcurrentHashMap, ScheduledExecutorService
- **Serialization:** GSON
- **Real-time Communication:** WebSocket

---

## 5. 📝 Hướng dẫn sử dụng sơ đồ

Bạn có thể render các tệp `.puml` này bằng:

### **Cách 1: PlantUML Online**
- Truy cập: https://www.plantuml.com/plantuml/uml/
- Copy paste nội dung file vào

### **Cách 2: IDE Extensions**
- **IntelliJ IDEA**: Cài PlantUML plugin
- **VS Code**: Cài PlantUML extension

### **Cách 3: Command Line**
```bash
# Cần cài PlantUML
plantuml ClassDiagram.puml
plantuml SequenceDiagram.puml
plantuml UseCaseDiagram.puml
```

Các tệp sẽ được export thành `.png` hoặc `.svg`

---

## 6. 📊 Các mối quan hệ chính

```
┌─────────────────────────────────────────┐
│        Bidder / Seller (Clients)        │
└────────────────────┬────────────────────┘
                     │
              WebSocket Connection
                     │
        ┌────────────▼─────────────┐
        │  AuctionWebSocketServer  │
        └────────────┬─────────────┘
                     │
         ┌───────────┴──────────┐
         │                      │
    ┌────▼──────────┐   ┌──────▼──────┐
    │ AuctionManager │   │ AuctionRoom │
    │   (Singleton)  │   │  (per item) │
    └────┬──────────┘   └──────────────┘
         │
    ┌────▼──────────────┐
    │  AuctionSession   │
    │  - placeBid()     │
    │  - isFinished     │
    └───────────────────┘
```

---

## 7. ⚡ Quy trình đặt giá đồng thời (Concurrent Bidding)

Hệ thống sử dụng:
- `synchronized` method trong `AuctionSession.placeBid()`
- `ConcurrentHashMap` cho auction rooms
- `ScheduledExecutorService` cho timeout

Điều này đảm bảo:
- ✅ Không race condition khi nhiều người đặt giá cùng lúc
- ✅ Chỉ giá cao nhất mới được lưu
- ✅ Phiên tự động kết thúc đúng giờ

---

Generated: 2026-05-01

