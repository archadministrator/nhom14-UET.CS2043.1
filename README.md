
Auction System
Dự án xây dựng một hệ thống đấu giá trực tuyến thời gian thực (Real-time Online Auction System), cho phép người bán đăng sản phẩm đấu giá và người mua tham gia trả giá thông qua giao diện web/desktop. Hệ thống được phát triển theo mô hình client–server, hỗ trợ nhiều vai trò người dùng, đảm bảo tính công bằng trong đấu giá và cập nhật dữ liệu theo thời gian thực.

Phạm vi hệ thống:

- Quản lý người dùng và phân quyền (Admin/Seller/Bidder).
- Quản lý sản phẩm đấu giá và vòng đời phiên đấu giá.
- Xử lý nghiệp vụ đấu giá thời gian thực.
- Hỗ trợ xác thực, bảo mật và quản trị hệ thống.
- Giao tiếp client–server qua REST API và WebSocket.

I. Các chức năng chính đã hoàn thành:

1. Quản lý tài khoản và xác thực
   - Đăng ký, đăng nhập.
   - Xác thực bằng JWT.
   - Quản lý hồ sơ người dùng và số dư tài khoản.

2. Quản lý đấu giá
   - Tạo, sửa, xoá sản phẩm đấu giá.
   - Xem danh sách đấu giá và chi tiết sản phẩm.
   - Quản lý trạng thái phiên đấu giá (mở, đóng, kích hoạt tự động).

3. Chức năng đặt giá
   - Đặt giá trực tiếp.
   - Kiểm tra tính hợp lệ của bid.
   - Lưu lịch sử đấu giá và truy vấn bid của người dùng.

4. Đấu giá tự động (Auto Bid / Proxy Bid)
   - Thiết lập mức đấu giá tự động.
   - Tự động nâng giá khi có cạnh tranh.
   - Huỷ cấu hình auto bid.

5. Cập nhật thời gian thực
   - Đồng bộ thông tin đấu giá bằng WebSocket.
   - Thông báo khi phiên đấu giá bắt đầu/kết thúc.

6. Quản trị hệ thống
   - Quản lý người dùng.
   - Khoá/mở tài khoản.
   - Quản lý phiên đấu giá và xác nhận thanh toán.

7. Kiểm thử
   - Unit test cho các service chính: User, Auction, Bid và AutoBid.


II. Công nghệ sử dụng, môi trường chạy và yêu cầu cài đặt
    1. Công nghệ sử dụng
        - Backend: Java
        - Framework backend: Springboot
        - Database: SQLite
        - ORM: Spring Data JPA / Hibernate
        - Authentication: JWT
        - Real-time Communication: WebSocket
        - Build tool: Maven
        - Testing: JUnit
        - API Testing: Postman
        - Version Control: Git
    2. Môi trường chạy
        - OS: Window, macOS, Linux 
        - Java Dev Kit: JDK 21+
        - IDE: IntelliJ IDEA, VSCode hoặc Eclipse 
    3. Yêu cầu cài đặt
        - JDK 21 trở lên
        - Có cấu hình Apache Maven
        
