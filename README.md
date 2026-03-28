# Auction App
# Bài tập lớn môn Lập Trình Nâng Cao của nhóm 3

Tổ chức các thư mục và chức năng của từng package, class chính (thêm các class để tối ưu code thì quá tốt) (ae đọc đi nghen xong tạm thời code theo form này mai sau mở rộng nó dễ)
# 1. Thư mục client (Giao Diện Người Dùng)

* Vai trò: Ứng dụng hiển thị trên máy của người dùng. Nhiệm vụ chính là lấy thao tác của người dùng, gửi lên Server và hiển thị kết quả trả về. 

Package ui (Giao diện hiển thị - View): // ae áp dụng JavaFX làm test luôn phần này trước //

- LoginUI: Màn hình vẽ các ô nhập tài khoản, mật khẩu và nút Đăng nhập.

- AuctionUI: Màn hình phòng đấu giá chi tiết, hiện ảnh sản phẩm, ô nhập giá tiền và hiển thị lịch sử người khác trả giá.

- MainUI: Sảnh chờ chung, hiển thị danh sách các phòng đấu giá đang diễn ra để người dùng chọn.

Package controller (Người điều phối):

- LoginController: Lấy chữ người dùng vừa gõ ở LoginUI, đóng gói lại, nhờ SocketClient gửi lên Server, đợi Server báo về (Thành công/Thất bại) và cập nhật lại lên LoginUI.

- AuctionController: Quản lý nút "Đặt giá", đồng thời liên tục lắng nghe sự kiện từ Server (như có người khác vừa trả giá cao hơn) để cập nhật giao diện AuctionUI ngay lập tức.

Package network (Giao tiếp mạng):

- SocketClient: Chuyên gia liên lạc. Nó chứa cấu hình IP, Port của Server và cung cấp các hàm sendData() và receiveData() để các Controller gọi đến.

**Class ClientMain: Nơi chứa hàm main() của máy khách, khởi tạo các Controller và hiển thị màn hình LoginUI đầu tiên.

# 2.Thư mục server (Bộ Não Của Hệ Thống)

- Nhiệm vụ: Chạy 24/7, lắng nghe kết nối, quản lý luồng dữ liệu, kiểm tra luật chơi (logic) và lưu trữ dữ liệu.

Package core (Lõi mạng):

- ServerMain: Hàm main() mở cổng (port) và khởi động Server.

- ClientHandler: Mỗi khi 1 Client kết nối, Server tạo ra 1 ClientHandler chạy độc lập (Thread) để chuyên phục vụ Client đó (nhận/gửi Packet).

- ClientManager: Quản lý danh sách các ClientHandler đang hoạt động. Rất quan trọng để tính năng Broadcast hoạt động (VD: thông báo giá mới cho toàn bộ người trong phòng).

Package handler (Bộ phận tiếp tân phân loại):

- AuthHandler: Nhận Packet có lệnh đăng nhập/đăng ký từ ClientHandler và chuyển cho UserService xử lý.

- BidHandler: Nhận Packet đặt giá và chuyển cho AuctionService xử lý.

Package model (Thực thể dữ liệu):

- Entity: Lớp gốc chứa thông tin chung như ID, ngày tạo.

- User: Lớp trừu tượng (Abstract) đại diện cho người dùng chung.

- Bidder, Seller, Admin: Kế thừa từ User, mỗi class có đặc điểm riêng.

- Item, Auction, BidTransaction: Lưu thông tin về Sản phẩm, Phiên đấu giá và Lịch sử trả giá.

Package repository (DAO - Tương tác cơ sở dữ liệu): *(tạm thời dùng mảng nào học database r làm tiếp)

- IRepository<T>: Giao diện chuẩn mực (Generics) quy định các hàm Thêm/Sửa/Xóa/Tìm kiếm chung.

- UserRepository, AuctionRepository: Thực thi IRepository, làm nhiệm vụ đọc/ghi dữ liệu thực tế (vào Database hoặc File/List).

Package service (Luật chơi / Logic nghiệp vụ):

- IUserService, IAuctionService: Giao diện quy định các chức năng (Interface).

- UserServiceImpl, AuctionServiceImpl: Nơi chứa "chất xám". Ví dụ: Kiểm tra xem người dùng có bị khóa không, giá đặt (bid) có hợp lệ không (phải cao hơn giá hiện tại và thời gian chưa kết thúc).

# 3.Thư mục shared (Phần Dùng Chung)

- Nhiệm vụ: Chứa các lớp dữ liệu và quy tắc mà cả Client và Server đều phải biết để có thể "hiểu" được nhau khi truyền dữ liệu qua mạng bằng Socket.

Package protocol (Quy tắc giao tiếp):

- Packet: Lớp "vỏ bọc" cho mọi gói tin gửi qua mạng. Gồm 3 phần: Mã lệnh (để làm gì), Dữ liệu (mang theo cái gì), và Trạng thái (thành công hay thất bại).

- RequestType: Chứa các mã lệnh (VD: LOGIN, BID, GET_AUCTIONS).

Package dto (Data Transfer Object - Thùng hàng dữ liệu):

- Chứa các đối tượng chỉ dùng để truyền đi qua mạng. (VD: UserDTO mang thông tin tài khoản, BidDTO mang thông tin số tiền đặt giá). Các lớp này phải implements Serializable.

Package constant (Hằng số):

- Lưu các giá trị cố định dùng chung như Role (SELLER, BIDDER, ADMIN) để tránh viết sai chính tả trong code.

Package util (Công cụ tiện ích):

- Các hàm dùng chung như kiểm tra định dạng email, mã hóa mật khẩu, định dạng tiền tệ (VNĐ).

# 4.Luồng hoạt động 
- (Client) Người dùng gõ giá 100k vào AuctionUI và bấm nút "Đặt giá".

- (Client) AuctionController lấy số 100k đó, tạo ra một BidDTO, bọc vào một Packet mang cờ lệnh PLACE_BID.

- (Client) SocketClient gửi Packet này qua mạng lên Server.

- (Server) ClientHandler của người dùng đó nhận được Packet, đọc thấy cờ PLACE_BID nên chuyển gói hàng cho BidHandler.

- (Server) BidHandler tháo gói hàng, nhờ AuctionServiceImpl kiểm tra xem 100k có hợp lệ không.

- (Server) AuctionServiceImpl gọi AuctionRepository để lấy thông tin phiên đấu giá hiện tại để đối chiếu. Nếu hợp lệ, lưu lịch sử mới vào Repository.

- (Server) AuctionServiceImpl báo lại kết quả thành công. Lúc này, Server dùng ClientManager để gửi một thông báo (Broadcast) đến tất cả các Client đang trong phòng đấu giá đó.

- (Client) Phía người dùng, AuctionUpdateListener nhận được thông báo mới, lập tức chỉ đạo AuctionUI đổi con số trên màn hình thành 100k.
