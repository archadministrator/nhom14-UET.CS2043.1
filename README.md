OK, vậy chúng ta bắt đầu dự án Auction System, một hệ thống đấu giá bằng web app. Vì tất cả đều là newbie, chúng ta sẽ xem xét dự án dưới góc nhìn đơn giản nhất

1. Tổng quan
    Auction System là một hệ thống đấu giá trực tuyến
    Đi theo hướng Auction Real-time - Đấu giá thời gian thực

2. Mô hình 
    a. Đối tượng
        Đối tượng của web app này gồm 2 nhóm chính: Bidder (người đấu giá) và Seller (người bán sản phẩm đấu giá)
    b. Hành vi riêng của đối tượng
        - Bidder: 
            + Xem sản phẩm
            + Đặt giá 
            + Theo dõi kết quả
            ...
        - Seller:
            + Đăng sản phẩm
            + Đặt giá khởi điểm
            + Set thời gian kết thúc đấu giá 
            ...
    c. Hành vi chung của User
        Tất cả Bidder và Seller đều là User với các hành vi: 
            + Đăng ký, đăng nhập
            + Thao tác với GUI 
            + Tham gia phiên đấu giá
            ...


3. Những việc cần làm
    a. Xây dựng các chức năng cơ bản của một Auction Web App
        + Quản lý người dùng: User là Bidder, Seller hay Admin?
        + Quản lý sản phẩm được đấu giá: 
            - Thêm, sửa, xoá sản phẩm
            - Các thông tin của sản phẩm:
                - Tên, mô tả, giá khởi điểm
                - Giá hiện tại
                - Thời gian bắt đầu, kết thúc phiên đấu sản phẩm này
        + Tham gia đấu giá:
            - Mô hình đấu giá cổ điển: đặt giá, người khác đặt cao hơn, đặt tiếp ... 
            - Kiểm tra tính logic của các phép đặt (giá không được thấp hơn hoặc bằng giá hiện tại và không được cao hơn số dư tài khoản của mình ...)
            - Cập nhật người dẫn đầu, giá cao nhất, ..
        + Kết thúc đấu giá: 
            - Chốt giá cuối cùng
            - Xác định người thắng
        + Giao diện - GUI
            - Sử dụng JavaFX (tích hợp CSS là đẹp nhất)
            - Khung giao diện 
                - Trang chủ: gồm logo app, login, tìm kiếm sản phẩm, danh sách sản phẩm đấu giá ở mức preview ...
                - Danh sách sản phẩm: thông tin chi tiết về sản phẩm, phiên đấu giá, ...
                - Chi tiết sản phẩm - màn hình đấu giá (phiên đấu giá): mọi thông tin về sản phẩm cũng như trạng thái đấu giá
                - Một số frame riêng: trang "Quản lý sản phẩm" cho Seller, 
            - Dark Mode, tối ưu tốc độ load frontend ...
        + Các chức năng khác: sẽ phát triển và cập nhật thêm
    b. Cấu trúc
        Dưới đây là cấu trúc ví dụ của một auction system cơ bản
           auction-system/
           │
           ├── client/
           │   ├── pom.xml
           │   └── src/
           │       └── main/
           │           ├── java/com/example/auctionfx/
           │           │   ├── AuctionFXApplication.java
           │           │   │
           │           │   ├── config/
           │           │   │   └── AppConfig.java
           │           │   │
           │           │   ├── controller/
           │           │   │   ├── MainController.java
           │           │   │   ├── LoginController.java
           │           │   │   └── AuctionDetailController.java
           │           │   │
           │           │   ├── service/
           │           │   │   └── ApiService.java
           │           │   │
           │           │   ├── model/
           │           │   │   ├── User.java
           │           │   │   ├── Auction.java
           │           │   │   └── Bid.java
           │           │   │
           │           │   ├── dto/
           │           │   │   ├── LoginRequest.java
           │           │   │   ├── BidRequest.java
           │           │   │   └── AuctionResponse.java
           │           │   │
           │           │   └── util/
           │           │       ├── HttpClientUtil.java
           │           │       └── SessionManager.java
           │           │
           │           └── resources/
           │               └── com/example/auctionfx/view/
           │                   ├── login.fxml
           │                   ├── main.fxml
           │                   └── auction-detail.fxml
           │
           ├── server/
           │   ├── pom.xml
           │   └── src/
           │       ├── main/
           │       │   ├── java/com/example/auction/
           │       │   │   ├── AuctionApplication.java
           │       │   │   │
           │       │   │   ├── config/
           │       │   │   │   ├── WebConfig.java
           │       │   │   │   └── OpenApiConfig.java
           │       │   │   │
           │       │   │   ├── security/
           │       │   │   │   ├── SecurityConfig.java
           │       │   │   │   ├── JwtFilter.java
           │       │   │   │   └── JwtUtil.java
           │       │   │   │
           │       │   │   ├── controller/
           │       │   │   │   ├── AuthController.java
           │       │   │   │   ├── AuctionController.java
           │       │   │   │   └── BidController.java
           │       │   │   │
           │       │   │   ├── service/
           │       │   │   │   ├── UserService.java
           │       │   │   │   ├── AuctionService.java
           │       │   │   │   └── BidService.java
           │       │   │   │
           │       │   │   ├── repository/
           │       │   │   │   ├── UserRepository.java
           │       │   │   │   ├── AuctionRepository.java
           │       │   │   │   └── BidRepository.java
           │       │   │   │
           │       │   │   ├── model/
           │       │   │   │   ├── User.java
           │       │   │   │   ├── Auction.java
           │       │   │   │   └── Bid.java
           │       │   │   │
           │       │   │   ├── dto/
           │       │   │   │   ├── LoginRequest.java
           │       │   │   │   ├── BidRequest.java
           │       │   │   │   └── AuctionResponse.java
           │       │   │   │
           │       │   │   ├── enums/
           │       │   │   │   ├── Role.java
           │       │   │   │   └── AuctionStatus.java
           │       │   │   │
           │       │   │   ├── exception/
           │       │   │   │   ├── GlobalExceptionHandler.java
           │       │   │   │   └── CustomException.java
           │       │   │   │
           │       │   │   └── validation/
           │       │   │       └── CustomValidator.java
           │       │   │
           │       │   └── resources/
           │       │       ├── application.yml
           │       │       ├── application-dev.yml
           │       │       ├── application-prod.yml
           │       │       ├── logback-spring.xml
           │       │       └── db/migration/
           │       │           ├── V1__init.sql
           │       │           └── V2__add_bid.sql
           │       │
           │       └── test/
           │           └── java/com/example/auction/
           │               ├── controller/
           │               │   └── AuctionControllerTest.java
           │               └── service/
           │                   └── AuctionServiceTest.java
           │
           ├── docs/
           │   ├── architecture.md
           │   └── api.md
           │
           ├── docker/
           │   ├── Dockerfile
           │   └── docker-compose.yml
           │
           └── README.md
    
    c. Phân công công việc
        Có 4 mảng chính: 
            + Backend
            + Frontend  
            + Database
            + Realtime
        Để cả 4 người có phần công việc tương đương nhau, sẽ phân công công việc giàn trải theo cả 4 mảng cho mỗi người:
        A - Backend – Auction Core
        Quản lý phiên đấu giá (CRUD), lịch sử đấu giá, tìm kiếm và lọc

        B - Backend – Bidding Logic và Real-time
        Proxy bidding, anti-sniping, WebSocket

        C - Frontend – JavaFX
        Toàn bộ giao diện người dùng, kết nối API, hiển thị real-time

        D - Backend – User và dịch vụ cơ bản
        Đăng ký, đăng nhập, phân quyền, ví cơ bản, đánh giá sản phẩm


        ------------------------------ ---------------------------------------------------------

        a. Thành viên A – Backend Auction Core và Data API
            1. Thiết kế database schema cho Auction, Bid, Category (nếu có)
            2. API CRUD Auction
                Tạo auction mới (tiêu đề, mô tả, giá khởi điểm, thời gian kết thúc, ảnh)
                Sửa auction (chỉ khi chưa có bid)
                Xóa auction (chỉ khi chưa có bid)
            3. API lấy danh sách auction
                Phân trang (page, size)
                Lọc theo trạng thái
                Sắp xếp theo giá và thời gian
                Tìm kiếm theo từ khóa
            4. API chi tiết auction kèm danh sách bid gần nhất
            5. API lịch sử đấu giá của một auction
            6. Scheduled task tự động kết thúc auction
            7. Viết unit test cho service

            *Công nghệ (tham khảo):
                Spring Data JPA, PostgreSQL
                Spring MVC
                JUnit, Mockito

        b. Thành viên B – Backend Bidding Logic và Real-time
            1. Proxy bidding
                Cho phép đặt giá tối đa
                Tự động tăng giá theo bước
                Xử lý race condition bằng lock
            2. Anti-sniping
                Nếu có bid trong X giây cuối thì gia hạn Y phút
            3. WebSocket real-time
                Thiết lập WebSocket STOMP
                Broadcast giá hiện tại và người đấu cao nhất
                Gửi thông báo khi bị vượt giá
            4. Xác định người thắng
            5. Viết unit test cho logic đồng thời

            *Công nghệ (gợi ý)
                Spring WebSocket, STOMP
                Spring Scheduling
                Java concurrency hoặc Redis

        c. Thành viên C – Frontend JavaFX
            1. Màn hình đăng nhập và đăng ký
            2. Màn hình danh sách auction
                TableView
                Tìm kiếm, lọc, phân trang
                Refresh định kỳ hoặc WebSocket
            3. Màn hình chi tiết auction
                Hiển thị thông tin
                Đồng hồ đếm ngược
                Form đặt giá và proxy
                Lịch sử đấu giá
                Cập nhật real-time
            4. Màn hình cá nhân
                Profile
                Auction đã đăng
                Auction đã tham gia
            5. Xử lý lỗi hiển thị

            *Công nghệ
                JavaFX (FXML, CSS)
                HttpClient hoặc OkHttp
                WebSocket client
        
        d.Thành viên D – Backend User và dịch vụ hỗ trợ
            1. Xác thực và phân quyền
                Đăng ký
                Đăng nhập (JWT)
                Role user và seller
                Mã hóa mật khẩu
            2. Profile
                Cập nhật thông tin
                Đổi mật khẩu
            3. Ví cơ bản
                Số dư user
                Kiểm tra tiền khi đặt giá
                API nạp tiền (admin)
                Xem lịch sử giao dịch
            4. Đánh giá
                Người mua đánh giá người bán
                Tính điểm trung bình
            5. API admin
                Nạp tiền
                Khóa tài khoản
                Xem danh sách user

            #Công nghệ
                Spring Security, JWT
                Spring Data JPA
                BCrypt


        Bảng phân công: 
            | Tuần  | Sprint                                   | Thành viên                 | Nhiệm vụ chi tiết                                                                                                                                         | Công nghệ / Công cụ                         | Đầu ra (Deliverable)                                                                 |
            |-------|------------------------------------------|----------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------|--------------------------------------------------------------------------------------|
            | 1–2   | Sprint 1: Khởi tạo & Xác thực            | A (Backend Auction)        | - Thiết kế DB schema (Auction, Bid).<br>- Viết API GET /auctions.<br>- API GET /auctions/{id}.<br>- Tạo dữ liệu mẫu.                                     | Spring Boot, JPA, PostgreSQL, Postman       | - ERD.<br>- API chạy localhost.<br>- Postman collection.                             |
            |       |                                          | B (Backend Bidding)        | - Nghiên cứu WebSocket + STOMP.<br>- Setup WebSocket config.<br>- Endpoint /ws + broker.<br>- API POST /bids.                                            | Spring WebSocket, STOMP, Postman            | - WebSocket server chạy.<br>- API bid cơ bản.                                        |
            |       |                                          | C (Frontend JavaFX)        | - Setup JavaFX.<br>- Thiết kế Login/Register.<br>- Kết nối API auth.<br>- BaseController + HttpClient wrapper.                                           | JavaFX, FXML, CSS, HttpClient               | - UI login/register.<br>- Flow xác thực hoạt động.                                   |
            |       |                                          | D (Backend User)           | - Spring Security + JWT.<br>- API đăng ký/đăng nhập.<br>- API user hiện tại.<br>- Phân quyền USER, SELLER.                                               | Spring Security, JWT, BCrypt                | - Auth JWT hoạt động.<br>- API user profile.                                         |
            | 3–4   | Sprint 2: Core Auction Features          | A                          | - CRUD auction.<br>- Tìm kiếm, lọc.<br>- Phân trang.<br>- Scheduled kết thúc auction.                                                                     | Spring Data JPA, @Scheduled                 | - CRUD hoàn chỉnh.<br>- Filter + pagination.                                         |
            |       |                                          | B                          | - Proxy Bidding.<br>- Xử lý race condition.<br>- WebSocket push bid realtime.                                                                             | Java concurrency, Spring WebSocket          | - Proxy bidding chạy.<br>- Realtime update.                                          |
            |       |                                          | C                          | - UI danh sách auction.<br>- Kết nối API.<br>- WebSocket client realtime.                                                                                 | JavaFX, WebSocket Client                    | - UI chính hoàn chỉnh.<br>- Realtime giá.                                            |
            |       |                                          | D                          | - Nâng cấp SELLER.<br>- Quản lý profile.<br>- API nạp tiền.                                                                                                | Spring Security, JPA                        | - User lên SELLER.<br>- Admin nạp tiền.                                              |
            | 5–6   | Sprint 3: Nâng cao & Hoàn thiện nghiệp vụ| A                          | - Lịch sử đấu giá.<br>- Auction của user.<br>- Tối ưu DB.<br>- Unit test.                                                                                 | JUnit, Mockito, Spring Data JPA             | - API lịch sử.<br>- Dashboard user.                                                  |
            |       |                                          | B                          | - Anti-sniping.<br>- WebSocket gia hạn.<br>- Xác định winner.<br>- Unit test logic.                                                                      | Spring Scheduling, JUnit                    | - Anti-sniping chạy.<br>- Auto kết thúc auction.                                     |
            |       |                                          | C                          | - UI chi tiết auction.<br>- Form bid & proxy.<br>- Bảng lịch sử.<br>- Xử lý WebSocket.                                                                   | JavaFX, Timeline, WebSocket                 | - UI chi tiết hoàn chỉnh.<br>- Bid realtime.                                         |
            |       |                                          | D                          | - Hệ thống đánh giá.<br>- API rating.<br>- Admin UI cơ bản.                                                                                               | JPA, Spring MVC                            | - Rating hoạt động.<br>- Admin UI.                                                   |
            | 7–8   | Sprint 4: Tích hợp, Kiểm thử & Hoàn thiện| Cả nhóm                    | - A: Tối ưu API, Swagger.<br>- B: Test tải, fix concurrency.<br>- C: UI/UX.<br>- D: Hoàn thiện admin.<br>- E2E test.<br>- README.                       | Toàn bộ stack                              | - App hoàn chỉnh.<br>- Swagger UI.<br>- Báo cáo dự án.                               |


        ---------------------------------------------------------------------


            
    


    
        











