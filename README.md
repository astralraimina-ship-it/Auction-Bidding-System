# Hệ thống đấu giá trực tuyến thời gian thực (Realtime Auction System)

Hệ thống đấu giá trực tuyến này được phát triển dựa trên kiến trúc Client-Server, áp dụng các nguyên lý lập trình hướng đối tượng (OOP), mẫu thiết kế (Design Patterns) kết hợp với cơ chế đồng bộ dữ liệu thời gian thực qua TCP Socket và mô hình Observer. Hệ thống giải quyết tốt bài toán xử lý đồng thời (Concurrency) và tối ưu trải nghiệm người dùng bằng các tính năng nâng cao như Tự động đấu giá (Auto-Bidding) và Chống bắn tỉa phút chót (Anti-Sniping).

---

## Các công nghệ sử dụng

Dự án sử dụng các công nghệ và thư viện tiêu chuẩn bao gồm:

* Ngôn ngữ cốt lõi: Java (JDK 17)
* Giao diện người dùng: JavaFX và FXML (Mô hình MVC)
* Hệ quản trị cơ sở dữ liệu: MySQL
* Quản lý dự án và Build: Maven
* Kết nối mạng liên máy: Radmin VPN (Tạo mạng LAN ảo)
* Kiểm thử phần mềm: JUnit 5 (Unit Testing)
* Tự động hóa tích hợp: GitHub Actions (CI/CD Workflow)

---

## Kiến trúc và Design Patterns đã áp dụng

1. Kiến trúc Client - Server: Giao tiếp hai chiều liên tục thông qua TCP Socket kết hợp đa luồng (Runnable), giúp Server xử lý nhiều kết nối đồng thời từ các Client một cách độc lập.
2. Mô hình MVC (Model - View - Controller): Tách biệt rõ ràng giữa tầng giao diện (FXML), tầng điều khiển (JavaFX Controllers), tầng dữ liệu (Model/Entity) và tầng tương tác cơ sở dữ liệu (ItemDAO, UserDAO, BidDAO).
3. Observer Pattern: Đây là trọng tâm của tính năng cập nhật thời gian thực. Khi một Client gửi lệnh đặt giá mới, hệ thống sẽ tự động thông báo và cập nhật tức thời bảng giá, lịch sử phiên và số dư ví cho toàn bộ các Client khác đang online mà không cần tải lại giao diện.
4. Singleton Pattern: Áp dụng tại lớp BidPublisher để đảm bảo một điểm điều phối dữ liệu duy nhất toàn cục.

---

## Các tính năng cốt lõi và điểm nhấn công nghệ

### 1. Tính năng cơ bản
* Quản lý thành viên và sản phẩm: Hỗ trợ đăng nhập, đăng ký, hiển thị danh mục sản phẩm đấu giá trực quan kèm hình ảnh
* Đấu giá thời gian thực: Cập nhật bước giá và người dẫn đầu lập tức sang tất cả các máy trạm đang theo dõi phòng đấu giá.
* Xử lý đồng thời (Concurrency): Sử dụng cơ chế synchronized block dựa trên từng ID sản phẩm cụ thể, đảm bảo tính toàn vẹn dữ liệu khi có nhiều người dùng hoặc bot tự động cùng bấm đặt giá tại một thời điểm.
* Hóa đơn và thanh toán: Tự động khấu trừ số dư ví điện tử của người chiến thắng trực tiếp trong Database sau khi phiên đấu giá khép lại.

### 2. Tính năng nâng cao
* Auto-Bidding (Tự động trả giá): Người dùng có thể cài đặt mức ngân sách tối đa và bước giá mong muốn. Server sẽ tự động tính toán nâng giá để giữ thế dẫn đầu một cách hợp lý và công bằng nhất khi có đối thủ khác đặt giá thủ công hoặc bot Auto-bid khác nhảy vào.
* Anti-Sniping (Chống trả giá sát nút): Nếu phát hiện có lượt đặt giá phát sinh trong vòng 1 phút cuối cùng trước khi đóng phòng, hệ thống tự động gia hạn thêm 2 phút để đảm bảo tính minh bạch, tránh hiện tượng nghẽn mạng phút chót làm mất cơ hội của người khác.
* Trực quan hóa lịch sử giá (Bid History Visualization): Thay vì chỉ đọc log dạng văn bản khô khan, hệ thống tích hợp biểu đồ đường (LineChart) thời gian thực. Mỗi khi giá thay đổi, một điểm tọa độ mới sẽ được vẽ lên đồ thị giúp người dùng theo dõi diễn biến căn phòng trực quan hơn.
* Mua ngay (BIN - Buy It Now): Cho phép kết thúc phiên đấu giá ngay lập tức bằng cách trả mức giá trần quy định, hệ thống sẽ chốt đơn và từ chối các lượt đặt giá sau đó.
---

## Hướng dẫn cài đặt và khởi chạy

### 1. Chuẩn bị môi trường
* Cài đặt JDK 17 và cấu hình biến môi trường JAVA_HOME.
* Cài đặt MySQL Server và công cụ MySQL Workbench.
* Sử dụng một IDE hỗ trợ tốt JavaFX (khuyến khích dùng IntelliJ IDEA).
* Phần mềm Radmin VPN: Dùng để kết nối liên mạng giữa các máy tính ở xa vào chung một Host Server.

### 2. Cấu hình mạng LAN ảo với Radmin VPN
Để thực hiện kiểm thử hệ thống với nhiều máy tính ở các môi trường mạng khác nhau kết nối chung vào Server, cấu hình mạng được thiết lập như sau:

* Thông tin phòng Radmin VPN:
  * **Network Name:** idontgiveafaboutit
  * **Password:** 123456
* Hướng dẫn kết nối:
  1. Máy Host (chạy Server) và các máy Client từ xa đều cần cài đặt phần mềm Radmin VPN và cùng tham gia vào phòng mạng trên.
  2. Máy chạy Server tiến hành lấy địa chỉ IPv4 do Radmin VPN cấp và bật khởi chạy lớp `AuctionServer.java`.
  3. Trên mã nguồn các máy Client, mở file cấu hình Socket (ví dụ `AuctionClient.java`) và thay đổi địa chỉ IP kết nối từ `localhost` thành đúng địa chỉ IPv4 Radmin VPN của máy Host.

### 3. Khởi tạo Cơ sở dữ liệu
* Mở MySQL Workbench, tạo một Schema mới tên là auction_db.
* Chạy file script SQL (database.sql kèm theo dự án) để tạo các bảng cần thiết.
* Thay đổi thông tin tài khoản và mật khẩu kết nối MySQL trong file DBConnection.java cho đúng với cấu hình máy cá nhân của bạn.

### 4. Build và chạy dự án trên IntelliJ IDEA

* Bước 1 (Chạy Server): Mở dự án bằng IntelliJ, đợi Maven tải xong các thư viện bổ trợ. Tìm đến file src/main/java/com/auction/server/AuctionServer.java, kích chuột phải và chọn Run.
* Bước 2 (Chạy Client): Tìm đến file src/main/java/com/auction/client/AuctionClient.java, kích chuột phải và chọn Run. Để kiểm thử tính năng realtime, bạn có thể vào mục Run Configuration của IntelliJ và tích chọn Allow multiple instances để mở song song nhiều giao diện Client cùng lúc.

---

## Kiểm thử và Tự động hóa (Testing & CI/CD)

* Unit Test: Dự án xây dựng sẵn các kịch bản kiểm thử điều kiện biên đối với logic xử lý Auto-Bid và Anti-Snipe thông qua JUnit 5. Bạn có thể thực hiện chạy test nhanh bằng lệnh `mvn clean test` ở thư mục gốc.
* CI/CD Workflow: Tích hợp sẵn file cấu hình .github/workflows/maven.yml. Mỗi khi mã nguồn được push lên GitHub, hệ thống GitHub Actions sẽ tự động khởi tạo môi trường Ubuntu ảo, cài đặt phần mềm đồ họa ảo (Xvfb) hỗ trợ cho JavaFX, tiến hành biên dịch và chạy toàn bộ các bài Unit Test tự động để kiểm tra chất lượng mã nguồn.
