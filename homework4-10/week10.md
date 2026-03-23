Bài 1:  Quản lý Dependency với Maven
Đề bài: Cho dự án MathUtils với tệp pom.xml cũ. Nhiệm vụ là nâng cấp cấu hình cũ (legacy) này để đáp ứng các yêu cầu sau:
Thêm Logback Classic 1.4.11 để thay thế các lệnh in thủ công (System.out.println) bằng structured logging (ghi log có cấu trúc).
Thêm Hibernate Core 6.2.0.Final để hỗ trợ nhu cầu lưu trữ dữ liệu trong tương lai.
Chuyển đổi dự án sang sử dụng JUnit Jupiter 5.9.2 làm công cụ kiểm thử duy nhất.
Giải quyết tất cả các xung đột và lỗi phát sinh từ những bổ sung này để đảm bảo quá trình build (xây dựng) thành công.
Giải thích giải pháp.
Bài 2: Code Quality - Checkstyle 
Đề bài: Một dự án BankSystem cũ chứa nhiều lỗi thiết kế và phong cách code không chuẩn. Mục tiêu là áp dụng các tiêu chuẩn chất lượng code chuyên nghiệp và khả năng quan sát (observability) bằng cách thực hiện các bước sau:
Tích hợp Maven Checkstyle Plugin vào quy trình build của dự án.
Chọn và áp dụng một tiêu chuẩn code (có thể dùng Google Java Style hoặc Sun Code Conventions) để xác định tất cả các vi phạm về định dạng và đặt tên.
Tái cấu trúc (Refactor) mã nguồn để giải quyết tất cả các lỗi được tìm thấy và vượt qua goal checkstyle:check thành công.
Theo dõi trạng thái hoạt động của hệ thống thông qua logging (sử dụng SLF4J hoặc Logback). Giải thích lý do chọn các cấp độ log (logging levels) và các điểm dữ liệu bạn chọn để ghi lại.
Bài 3: CI/CD automation 
Đề bài: Chọn một hàm hoặc hệ thống đã triển khai trước đó và tổ chức lại thành một dự án Maven có tích hợp logging SLF4J/Logback và Unit Tests. Nhiệm vụ là định nghĩa một quy trình CI để tự động hóa việc xác minh build:
Viết một workflow GitHub Actions được kích hoạt (trigger) khi có sự kiện push và pull_request.
Cấu hình workflow để thực thi vòng đời build của Maven (Maven build lifecycle), đảm bảo các pha test và package chạy thành công.
Triển khai action upload-artifact để lưu giữ và xác minh tệp .jar được tạo ra sau khi build hoàn tất.
Kiểm thử sự tự động hóa bằng cách cố tình gây ra lỗi build và chứng minh khả năng debug vấn đề bằng cách sử dụng execution logs do GitHub Actions cung cấp.
Bài 4: Kiểm thử đa hệ điều hành với Matrix Strategy
Đề bài: Vấn đề “Nó chạy được trên máy tôi” (It works on my machine) thường xảy ra do sự khác biệt về hệ điều hành. GitHub Actions cung cấp chiến lược Matrix để giúp giải quyết vấn đề này. Yêu cầu:
Cập nhật tệp cấu hình workflow của bạn để áp dụng Matrix Strategy, cho phép các bài test chạy đồng thời trên nhiều hệ điều hành: ubuntu-latest, windows-latest, và macos-latest.
Cố tình tạo một Unit Test xử lý đường dẫn tệp tin sử dụng định dạng cứng của Windows hoặc Linux (ví dụ: dùng dấu gạch chéo cứng \ hoặc /). Chạy pipeline để quan sát lỗi xảy ra trên hệ điều hành không tương thích.
Tái cấu trúc (Refactor) mã nguồn sử dụng File.separator hoặc API java.nio.file.Path để đảm bảo bài test chạy thành công trên mọi môi trường trong matrix.
Bài 5: Test Coverage & Quality Enforcement (JaCoCo)
Đề bài: Để đảm bảo độ tin cậy của mã nguồn, chỉ viết unit test là chưa đủ; bạn phải đo lường xem bao nhiêu phần code của bạn thực sự được kiểm thử (Code Coverage).
Tích hợp jacoco-maven-plugin vào pom.xml để tạo báo cáo về độ bao phủ code.
Cấu hình một quy tắc nghiêm ngặt (strict rule) trong plugin để tự động làm fail bản build nếu độ bao phủ code giảm xuống dưới 80%.
Đảm bảo bước kiểm tra này được thực thi trong workflow GitHub Actions của bạn (ví dụ: trong pha mvn verify).
Sử dụng action upload-artifact để trích xuất và lưu trữ báo cáo bao phủ (tìm thấy tại target/site/jacoco/index.html) sau khi build xong để có thể xem lại sau.
Bài 6: CI/CD Pipeline Optimization & Caching
Đề bài: Khi dự án phát triển, việc tải xuống các Maven dependency từ đầu cho mỗi lần chạy CI tiêu tốn nhiều thời gian và làm chậm chu kỳ phát triển.
Cấu hình dependency caching trong workflow GitHub Actions bằng cách thêm tham số cache: 'maven' vào action setup-java.
Thực hiện 2 lần push code liên tiếp lên GitHub repository. Ghi lại và so sánh thời gian thực thi workflow trước và sau khi áp dụng caching.
Kiểm tra và phân tích log thực thi của GitHub Actions để chứng minh rằng các dependency đã được lấy thành công từ cache thay vì phải tải lại từ Maven Central.
Bài 7: Automated Code Review via Pull Request
Đề bài: Biến CI pipeline của bạn thành một “người review” tự động để cung cấp phản hồi ngay lập tức trong quá trình phát triển. Yêu cầu:
Trigger: Cập nhật workflow GitHub Actions để tự động trigger khi có sự kiện pull_request hướng vào nhánh main.
Inline Feedback (Phản hồi trực tiếp): Tích hợp một GitHub Action (ví dụ: dbelyaev/action-checkstyle) để tự động đăng “bình luận”. (comments) trực tiếp vào các dòng code vi phạm tiêu chuẩn dự án.
Branch Protection (Bảo vệ nhánh): Cấu hình Branch Protection Rules để vô hiệu hóa nút Merge nếu phát hiện bất kỳ lỗi định dạng (formatting errors) hoặc lỗi test nào.
Verification (Kiểm chứng): Tạo một Pull Request với một lỗi Checkstyle cố ý. Xác nhận rằng Bot tự động bình luận vào PR và nút Merge vẫn bị khóa cho đến khi lỗi được sửa.
Bài 8: Đóng gói sản phẩm thực thi
Nhiệm vụ: Trong môi trường chuyên nghiệp, phần mềm phải được đóng gói để có thể chạy độc lập mà không cần đến IDE. Bạn được yêu cầu cấu hình vòng đời Maven để tạo ra một tệp thực thi hoàn chỉnh.
Yêu cầu:
Cấu hình plugin maven-jar-plugin trong tệp pom.xml bằng cách chỉ định lớp chính (Main-Class) trong phần manifest.
Thực thi lệnh mvn clean package để tạo tệp .jar trong thư mục target.
Xác minh: Chứng minh ứng dụng có thể khởi chạy thông qua terminal bằng lệnh java -jar target/*.jar. Giải thích ý nghĩa của thư mục target và pha package trong Maven.
Bài 9: Triển khai Logging chuyên nghiệp 
Nhiệm vụ: Thay thế tất cả các thói quen kiểm thử lỗi "nghiệp dư" bằng một khung làm việc logging có cấu trúc để đảm bảo khả năng truy vết hệ thống, như đã thảo luận trong bài giảng.
Yêu cầu:
Tối ưu hóa mã nguồn bằng cách loại bỏ tất cả các câu lệnh System.out.println().
Triển khai SLF4J cùng với Logback bằng cách sử dụng các mức độ log phù hợp: INFO để theo dõi các mốc quan trọng và ERROR để xử lý ngoại lệ.
Sử dụng Parameterized Logging (dấu giữ chỗ {}) thay vì cộng chuỗi để tối ưu hóa hiệu suất.
Cấu hình một FileAppender trong tệp logback.xml để lưu trữ log vào một tệp vật lý.
Xác minh: Chạy lệnh mvn test và xác nhận rằng các dòng log được định dạng chính xác (bao gồm thời gian và mức độ log) trong cả console và tệp log được tạo ra.
Bài 10: The broken pipeline
Một dự án Maven đã được thiết lập sẵn nhưng pipeline CI liên tục báo đỏ. Nhiệm vụ của bạn là tìm và sửa tất cả lỗi mà không được sửa mò - phải đọc log để hiểu nguyên nhân trước khi sửa.

Dự án được cung cấp như sau:

src/main/java/com/lab/ShippingCalculator.java
package com.lab;

public class ShippingCalculator {

    public double calculate(double weight, String type) {
        if (weight <= 0) {
            throw new IllegalArgumentException("Weight must be positive");
        }
        if (type.equals("EXPRESS")) return weight * 5000 + 20000;
        if (type.equals("STANDARD")) return weight * 3000;
        throw new IllegalArgumentException("Unknown type: " + type);
    }
}


src/test/java/com/lab/ShippingCalculatorTest.java
package com.lab;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ShippingCalculatorTest {

    ShippingCalculator calc = new ShippingCalculator();

    @Test
    void testStandard() {
        assertEquals(15000.0, calc.calculate(5, "STANDARD"));
    }

    @Test
    void testExpress() {
        assertEquals(45000.0, calc.calculate(5, "EXPRESS"));
    }

    @Test
    void testInvalidWeight() {
        assertThrows(IllegalArgumentException.class,
            () -> calc.calculate(-1, "STANDARD"));
    }
}


pom.xml
<project>
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.lab</groupId>
    <artifactId>shipping-app</artifactId>
    <version>1.0-SNAPSHOT</version>

    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
    </properties>

    <dependencies>
        <dependency>
            <groupId>ch.qos.logback</groupId>
            <artifactId>logback-classic</artifactId>
            <version>9.9.9</version>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>5.9.2</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>2.12.4</version>
            </plugin>
        </plugins>
    </build>
</project>


.github/workflows/ci.yml
name: Java CI

on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Build with Maven
        run: mvn package


Yêu cầu:
Tạo repository trên GitHub, đẩy toàn bộ dự án lên và quan sát pipeline chạy.
Với mỗi lỗi tìm được:
Chỉ ra lỗi nằm ở file nào, dòng nào.
Sao chép đoạn log liên quan từ GitHub Actions làm bằng chứng.
Giải thích nguyên nhân kỹ thuật của lỗi.
Sửa và push lại - pipeline phải xanh hoàn toàn trước khi chuyển sang lỗi tiếp theo.
Sau khi pipeline xanh, cố tình tạo thêm một lỗi thứ 4 tự nghĩ ra, push lên, chụp ảnh pipeline đỏ, sửa lại và giải thích. Lỗi tự nghĩ không được trùng với 3 lỗi đã có.










Networking Exercises
Bài 1: TCP + Client - Server (Mô hình Echo) 
Xây dựng một hệ thống chat bao gồm một Client và một Server dựa trên mô hình Echo sử dụng giao thức TCP. Hệ thống cần đáp ứng các yêu cầu sau:
Hệ thống phải sử dụng Sockets để giao tiếp và hỗ trợ ít nhất một kết nối hoạt động.
Server phải nhận được tin nhắn và ngay lập tức trả về chính xác tin nhắn đó cho Client.
Client phải hiển thị phản hồi nhận được từ Server lên màn hình.
Bài 2: Multi-Thread Client Server 
Mở rộng hệ thống chat ở Exe 5 với tính năng đa luồng (multi-threading) để hỗ trợ giao tiếp theo thời gian thực cho nhiều người dùng cùng lúc. Hệ thống cần đáp ứng các yêu cầu sau:
Server phải xử lý nhiều kết nối Client đồng thời mà không làm chặn (block) luồng thực thi chính khi một Client cụ thể phản hồi chậm.
Khi một Client gửi tin nhắn, Server phải xử lý và trả về phản hồi tương ứng cho đúng người gửi đó.
Đảm bảo rằng Client nhận và hiển thị thành công phản hồi của Server trên màn hình.
Triển khai cơ chế ghi nhật ký (logging) để theo dõi các thông tin cần thiết như trạng thái kết nối, địa chỉ IP của client, các sự kiện giao tiếp, v.v.
Bài 3: REST API 
Xây dựng một hệ thống chat theo phòng (room-based) sử dụng kiến trúc REST API phi trạng thái (stateless) dựa trên giao thức HTTP. Hệ thống cần đáp ứng các yêu cầu sau:
Server phải quản lý các ID Client duy nhất được chỉ định theo thứ tự tham gia và cung cấp các endpoint để tham gia (join), gửi tin nhắn (submit), và liệt kê toàn bộ giao tiếp trong phòng.
Client phải tham gia vào phòng để nhận ID, gửi tin nhắn được gắn thẻ với ID đó, và định kỳ lấy danh sách tin nhắn để hiển thị.
Các Client trước tiên phải gửi một yêu cầu đến Server để đăng ký và tham gia phòng trước khi có thể bắt đầu trò chuyện.
Khi một Client gửi tin nhắn, Server nhận và phát sóng (broadcast) nó vào không gian giao tiếp chung, đảm bảo tất cả các Client khác trong cùng một phòng đều có thể truy cập được tin nhắn đó.

Bài 4: Thiết kế CSDL Quan hệ & Chuẩn hóa (Relational Database Design & Normalization) 
Dựa trên hệ thống chat theo phòng ở bài tập REST API (Exe 7), thiết kế một Cơ sở dữ liệu Quan hệ hoàn chỉnh để lưu trữ dữ liệu lâu dài thay vì chỉ lưu trên RAM của Server. Hệ thống cần đáp ứng các yêu cầu sau:
Thiết kế một Sơ đồ Thực thể - Mối quan hệ (ERD) chứa ít nhất ba thực thể cốt lõi: User (Người dùng), Room (Phòng), và Message (Tin nhắn), xác định rõ các mối quan hệ Một-Nhiều (1-N) hoặc Nhiều-Nhiều (N-M).
Chuẩn hóa lược đồ cơ sở dữ liệu đạt Chuẩn 3 (3NF) để loại bỏ sự dư thừa dữ liệu và đảm bảo tính toàn vẹn của dữ liệu.
Viết các tập lệnh SQL (Ngôn ngữ Định nghĩa Dữ liệu - DDL) (CREATE TABLE) để tạo các bảng này. Bắt buộc phải áp dụng các ràng buộc cần thiết bao gồm PRIMARY KEY, FOREIGN KEY, NOT NULL, và các giá trị DEFAULT (ví dụ: đặt thời gian mặc định của tin nhắn là thời gian hiện tại).
Bài 5: Truy vấn dữ liệu nâng cao với Cơ sở dữ liệu mẫu Sakila 
Tải và thiết lập cơ sở dữ liệu mẫu sakila chính thức của MySQL (một hệ thống quản lý cửa hàng cho thuê DVD). Viết các câu truy vấn SQL (Ngôn ngữ Thao tác Dữ liệu - DML) nâng cao để giải quyết các yêu cầu phân tích dữ liệu sau:
Viết một câu truy vấn sử dụng JOIN cơ bản và các hàm Tổng hợp (Aggregation) để tìm ra Top 5 thể loại phim (categories) tạo ra tổng doanh thu cho thuê cao nhất.
Viết một câu truy vấn sử dụng Lọc phức tạp (mệnh đề HAVING) để lấy danh sách các khách hàng đã thuê nhiều hơn 30 bộ phim, hiển thị Tên, Email và Tổng số lượt thuê của họ, sắp xếp theo số lượng thuê giảm dần.
Viết một câu truy vấn sử dụng Truy vấn con (Subqueries) hoặc Biểu thức Bảng Chung (CTE) để xác định những khách hàng có tổng chi tiêu lớn hơn mức chi tiêu trung bình của toàn bộ khách hàng cộng lại.
Viết một câu truy vấn sử dụng Window Functions để gán Thứ hạng (Rank) cho các bộ phim trong từng thể loại cụ thể dựa trên thời lượng (length) của chúng, hiển thị Tên phim, Thể loại, Thời lượng và Thứ hạng tương ứng.
Bài 6: Lập trình mạng với Hệ thống Giám sát Thời tiết Thông minh (TCP vs. UDP)
Xây dựng một hệ thống giao tiếp đa kênh cho Trạm Thời tiết, bao gồm Kênh Điều khiển bằng TCP (đảm bảo độ tin cậy 100% cho các lệnh quan trọng) và Kênh Viễn trắc bằng UDP (ưu tiên tốc độ truyền phát dữ liệu cảm biến theo thời gian thực). Viết các chương trình mạng và áp dụng kỹ thuật xử lý ngoại lệ (Exception Handling) để giải quyết các yêu cầu sau:
Viết một class CommandServer sử dụng Lập trình Socket (TCP) với ServerSocket để lắng nghe trên cổng 5000, nhận và phản hồi các lệnh ("START" in ra "System initialized...", "SHUTDOWN" in ra "System shutdown..."). Yêu cầu tích hợp thời gian chờ setSoTimeout(5000) để bắt SocketTimeoutException nếu không có tương tác, đồng thời xử lý BindException và IOException.
Viết một class CommandClient sử dụng Socket (TCP) để kết nối tới localhost cổng 5000, thực hiện gửi tuần tự lệnh "START" và "SHUTDOWN". Yêu cầu sử dụng khối try-catch để bắt ConnectException và hiển thị cảnh báo "Error: Remote server is offline!", qua đó kiểm chứng tính tin cậy của TCP khi phía Server chưa khởi động.
Viết một class SensorSender (Trạm thời tiết) sử dụng DatagramSocket và DatagramPacket (UDP) để gửi chuỗi dữ liệu môi trường (ví dụ: "Temp: 28°C, Humidity: 65%") đến cổng 6000.
Viết một class SensorReceiver (Trạm giám sát) sử dụng DatagramSocket (UDP) lắng nghe trên cổng 6000 để nhận các gói tin (packet) liên tục và hiển thị nội dung dữ liệu thời tiết nhận được lên màn hình console.
Viết một báo cáo phân tích ngắn (Critical Thinking) dựa trên quá trình kiểm thử hệ thống để giải thích: (1) Ngoại lệ nào xảy ra và tại sao khi chạy đồng thời hai chương trình CommandServer trên cùng một cổng; (2) Sự khác biệt về bản chất giao thức khiến TCP (CommandClient) báo lỗi khi vắng Server, trong khi UDP (SensorSender) gửi dữ liệu mà không báo lỗi dù không có Receiver.

MVC, GUI, JavaFX Exercises
Bài 1: SceneBuilder và FXML 
Thiết kế một biểu mẫu nhập liệu cho một lĩnh vực mà bạn tự chọn, chẳng hạn như hệ thống đăng ký sinh viên, phần mềm ghi nhận sách thư viện, hoặc trình quản lý kho sản phẩm. Trọng tâm của bài tập là tạo ra một bố cục giao diện người dùng có tổ chức và chuyên nghiệp. Hệ thống cần đáp ứng các yêu cầu sau:
Xây dựng giao diện bằng công cụ SceneBuilder và lưu bố cục hoàn chỉnh dưới dạng một tệp FXML.
Kết hợp ít nhất 3 loại Bố cục (Layouts) khác nhau để cấu trúc biểu mẫu một cách hiệu quả (ví dụ: sử dụng VBox làm khung chứa chính, GridPane để căn chỉnh các trường nhập liệu, và HBox cho các nút bấm hành động ở dưới cùng).
Triển khai đa dạng các Điều khiển giao diện (UI Controls), ví dụ như TextField, CheckBox, Button, v.v.
Áp dụng CSS để định dạng kiểu dáng trực quan cơ bản cho các thành phần giao diện.
Bài 2: Triển khai mô hình MVC - Event Handling 
Sử dụng biểu mẫu nhập liệu đã thiết kế ở Exe 1, triển khai kiến trúc MVC để quản lý logic ứng dụng và các tương tác của người dùng. Hệ thống cần đáp ứng các yêu cầu sau:
Triển khai logic Xử lý sự kiện (Event Handling) sao cho việc nhấp vào nút “Submit” sẽ kích hoạt Controller đọc toàn bộ các giá trị hiện tại từ biểu mẫu.
Thực thi quy trình kiểm tra dữ liệu (Validation) để phát hiện các lỗi, chẳng hạn như trường bị bỏ trống hoặc sai định dạng. Cung cấp phản hồi trực quan trên giao diện để thông báo cho người dùng về việc gửi thành công hoặc các lỗi xác thực cụ thể.
Duy trì sự phân tách vai trò nghiêm ngặt, đảm bảo rằng View chỉ đơn thuần là một bố cục khai báo, trong khi Controller xử lý toàn bộ logic tương tác.
Bài 3: Lưu trữ dữ liệu với Database trong mô hình MVC
 Dựa trên dự án MVC đã tích hợp từ Exe 2, bạn có nhiệm vụ triển khai tính năng lưu trữ dữ liệu để đảm bảo thông tin do người dùng nhập vào có thể được bảo tồn và tải lại giữa các phiên chạy ứng dụng. Hệ thống cần đáp ứng các yêu cầu sau:
Tích hợp ứng dụng với một Cơ sở dữ liệu (Database) sử dụng JDBC.
Triển khai tính năng Save (Lưu) trong Controller nhằm kích hoạt Model chèn các dữ liệu biểu mẫu đã được xác thực vào Cơ sở dữ liệu. Hiển thị một cảnh báo hoặc thông báo trực quan để xác nhận bản ghi đã được lưu thành công hay đã xảy ra lỗi cơ sở dữ liệu.
Triển khai tính năng Load (Tải) để truy xuất các bản ghi đã lưu và tự động điền (populate) dữ liệu từ Cơ sở dữ liệu vào các điều khiển giao diện JavaFX.
Bài 4: Lập trình sự kiện JavaFX nâng cao & Xử lý đa luồng (Concurrency) Sử dụng biểu mẫu nhập liệu và kiến trúc MVC từ các bài trước, nâng cấp trải nghiệm người dùng (UX) và hiệu năng của ứng dụng bằng cách áp dụng các kỹ thuật xử lý sự kiện nâng cao và đa luồng. Hệ thống cần đáp ứng các yêu cầu sau:
Triển khai kiểm tra dữ liệu theo thời gian thực (Real-time Validation) bằng cách sử dụng Change Listeners (textProperty().addListener(...)). Nếu người dùng nhập sai dữ liệu, lập tức đổi màu viền của TextField sang màu đỏ và hiển thị cảnh báo, sau đó chuyển lại bình thường khi dữ liệu được sửa đúng.
Cấu hình Sự kiện Bàn phím (Keyboard Events) để cho phép người dùng chuyển con trỏ (focus) sang trường nhập liệu tiếp theo bằng phím "Enter", và tạo một phím tắt toàn cục (ví dụ: Ctrl + S) để kích hoạt quá trình gửi dữ liệu giống hệt như khi bấm nút "Submit/Save".
Tái cấu trúc (Refactor) các thao tác Lưu (Save) và Tải (Load) dữ liệu để chạy bất đồng bộ trên một luồng nền (ví dụ: sử dụng javafx.concurrent.Task hoặc Thread) nhằm tránh làm đơ (freeze) luồng giao diện chính.
Hiển thị một ProgressIndicator (vòng xoay tải) trực quan trên giao diện trong khi luồng nền đang chạy, và bắt buộc sử dụng Platform.runLater(...) để cập nhật giao diện an toàn với thông báo thành công hoặc thất bại khi tác vụ hoàn thành.

