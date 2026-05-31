# Hệ thống Đấu giá Trực tuyến

Dự án môn **Lập trình nâng cao** — Nhóm 14, Trường Đại học Công nghệ (VNU-UET).

Hệ thống cho phép nhiều người dùng cùng tham gia đấu giá sản phẩm theo thời gian thực, với các vai trò Bidder, Seller và Admin. Tham khảo mô hình eBay Auctions.

---

## Công nghệ sử dụng

| Thành phần | Công nghệ |
|---|---|
| Server | Java 21, Spring Boot 3.2, Spring Security (JWT), Spring WebSocket (STOMP), Spring Data JPA |
| Client | Java 17, JavaFX 21 (FXML), Java-WebSocket 1.5 |
| Cơ sở dữ liệu | SQLite 3 |
| Build tool | Maven 3.8+ |
| Test | JUnit 5, Mockito 5 |
| CI/CD | GitHub Actions |

---

## Yêu cầu môi trường

- **JDK 21** (server) và **JDK 17+** (client) — hoặc dùng JDK 21 cho cả hai
- **Maven 3.8+**
- Không cần cài JavaFX riêng — đã đóng gói trong fat JAR

---

## Cấu trúc thư mục

```
auction-system/
├── server/                         # Spring Boot backend
│   ├── src/main/java/com/auction/
│   │   ├── controller/             # REST API endpoints
│   │   ├── service/                # Business logic (BidService, AutoBidService, ...)
│   │   ├── model/                  # JPA entities (User, AuctionItem, Bid, AutoBidConfig)
│   │   ├── dao/                    # Spring Data JPA repositories
│   │   ├── scheduler/              # AuctionBackgroundWorker (@Scheduled 10s)
│   │   ├── config/                 # WebSocket, Security, Application config
│   │   └── exception/              # Custom exceptions + GlobalExceptionHandler
│   ├── src/main/resources/
│   │   ├── application.properties  # Cấu hình port, DB, JWT
│   │   └── schema.sql              # DDL khởi tạo DB
│   ├── data/
│   │   └── auction.db              # File SQLite (tự tạo khi chạy lần đầu)
│   └── pom.xml
│
├── client/                         # JavaFX frontend
│   ├── src/main/java/com/auction/client/
│   │   ├── controller/             # FXML Controllers
│   │   ├── service/                # ApiService (REST), WebSocketService (STOMP)
│   │   ├── realtime/               # AuctionEventBus, AuctionObserver, AuctionEvent
│   │   ├── model/                  # ClientDto (AuctionDto, BidDto, ...)
│   │   └── util/                   # FxUtil, SessionManager
│   ├── src/main/resources/fxml/    # Các file .fxml giao diện
│   └── pom.xml
│
└── UML diagrams/                   # Sơ đồ PlantUML
```

---

## Vị trí file JAR

Sau khi build bằng `mvn package`:

| Module | Đường dẫn JAR | Mô tả |
|---|---|---|
| Server | `server/target/auction-server-1.0.0.jar` | Spring Boot fat JAR (chạy trực tiếp) |
| Client | `client/target/auction-client-1.0.0.jar` | JavaFX fat JAR (Shade, kèm `Launcher`) |

---

## Hướng dẫn build & chạy

### Bước 1 — Build

```bash
# Build server
cd auction-system/server
mvn clean package 

# Build client
cd ../client
mvn clean package 
```

### Bước 2 — Chạy Server (chạy TRƯỚC)

```bash
cd auction-system/server
java -jar target/auction-server-1.0.0.jar
```

Server khởi động tại `http://localhost:8080`.  
File DB SQLite được tạo tự động tại `server/data/auction.db` nếu chưa có.

> **Lưu ý:** Phải đảm bảo port 8080 chưa bị chiếm trước khi chạy.

### Bước 3 — Chạy Client (chạy SAU khi server đã sẵn sàng)

```bash
cd auction-system/client
java -jar target/auction-client-1.0.0.jar
```

Cửa sổ JavaFX sẽ mở ra. Mặc định client kết nối tới `http://localhost:8080`.

### Tài khoản mặc định (seed data)

| Username | Password | Vai trò |
|---|---|---|
| `admin` | `admin123` | Admin |
| `seller1` | `123456` | Seller |
| `bidder1` | `123456` | Bidder |
| `bidder2` | `123456` | Bidder |

---

## Danh sách chức năng đã hoàn thành

### Chức năng bắt buộc

- [x] **Quản lý người dùng** — Đăng ký, đăng nhập JWT, 3 vai trò (Bidder / Seller / Admin), nạp tiền
- [x] **Quản lý sản phẩm** — Seller: thêm / sửa / xóa phiên đấu giá với đầy đủ thông tin
- [x] **Tham gia đấu giá** — Đặt giá, kiểm tra hợp lệ, cập nhật người dẫn đầu
- [x] **Kết thúc phiên** — Tự động đóng phiên theo lịch (3s), xác định winner, vòng đời `OPEN → RUNNING → FINISHED → PAID / CANCELED`
- [x] **Xử lý lỗi & ngoại lệ** — GlobalExceptionHandler, custom exceptions, xử lý lỗi kết nối
- [x] **Giao diện JavaFX** — 5 màn hình: danh sách phiên, chi tiết, đặt giá realtime, quản lý seller, admin

### Kỹ thuật & Kiến trúc

- [x] **Xử lý đồng thời an toàn** — Per-auction `ReentrantLock(fair=true)` + `@Transactional`, tránh lost update / race condition
- [x] **Realtime update** — WebSocket STOMP, `AuctionEventBus` (Observer Pattern), dispatch trên FX thread
- [x] **Subscribe-first pattern** — Không bỏ lỡ event khi navigate vào chi tiết phiên
- [x] **JavaFX Property binding** — `SimpleObjectProperty` cho giá/status, TableView tự re-render không cần `refresh()`
- [x] **Kiến trúc Client-Server** — REST API (JWT) + WebSocket STOMP, tách biệt hoàn toàn
- [x] **MVC** — FXML + Controller phía client; Controller → Service → DAO phía server
- [x] **Maven + Shade Plugin** — Fat JAR chạy trực tiếp `java -jar`
- [x] **Unit Test** — JUnit 5 + Mockito, coverage BidService (concurrency), AutoBidService, WebSocketService
- [x] **CI/CD** — GitHub Actions chạy `mvn verify` tự động

### Chức năng nâng cao

- [x] **Auto-Bidding** — `PriorityQueue` theo `maxAmount DESC, createdAt ASC`; proxy bid = `loser.maxAmount + best.increment`
- [x] **Anti-sniping** — Bid trong 5 phút cuối → gia hạn 5 phút, tối đa 3 lần; sau đó hard-close 30 giây
- [x] **Bid History Visualization** — `LineChart<Number,Number>` JavaFX, cập nhật realtime mỗi bid

---

## Nhóm thực hiện

| Thành viên | Vai trò chính |
|---|---|
| Dương Huy | Frontend: toàn bộ các file fxml, controller; SessionManager và MainApp |
| Đặng Gia Hưng | Backend: AuctionService, Scheduler, REST API |
| Lê Công Nhật | Backend: AuctionDetailController, realtime client |
| Nguyễn Văn Mạnh | Backend: AuctionListController, WebSocketService, UI |

# Link báo cáo PDF và video demo

https://drive.google.com/drive/folders/1iy9qvs_ie9olkUQdgbe_zyQym1W93QLA?usp=drive_link
