# Doan — Ứng dụng đánh giá nguy cơ tiểu đường

Đây là ứng dụng desktop Java có giao diện tiếng Việt. Ứng dụng dùng mô hình dự đoán đi kèm, lưu tài khoản và lịch sử vào Microsoft SQL Server. Khi mở ứng dụng, backend Spring Boot được chạy tự động bên trong; không cần mở backend riêng.

## Các thư mục và tệp chính

| Thư mục/tệp | Dùng để làm gì |
|---|---|
| `desktop-app` | Cửa sổ desktop, các màn hình dự đoán, lịch sử, thống kê và tài khoản. |
| `java-backend` | Xử lý đăng nhập, dự đoán, lưu dữ liệu và cung cấp API cho giao diện desktop. |
| `database/sqlserver/create-database.sql` | Tạo cơ sở dữ liệu và các bảng trên SQL Server. |
| `gradlew.bat` | Chạy Gradle trên Windows, tự dùng đúng phiên bản Gradle của dự án. |
| `README.md` | Hướng dẫn đầy đủ và giải thích cấu trúc dự án. |
| `QUICKSTART.md` | Các bước chạy nhanh trong Eclipse. |

Tên lớp và biến trong mã nguồn được giữ theo quy ước Java (tiếng Anh); tên màn hình, nút bấm và hướng dẫn cho người dùng được viết bằng tiếng Việt. Không nên dịch tên lớp/gói vì sẽ làm thay đổi cấu trúc mã và khó tích hợp thư viện Java hơn.

## Chạy trong Eclipse

### 1. Cài công cụ

- Cài JDK 21.
- Cài Eclipse IDE for Java Developers và Gradle Buildship.
- Cài Microsoft SQL Server và SQL Server Management Studio (SSMS).

Trong Eclipse, mở **Window → Preferences → Java → Installed JREs** và kiểm tra đã chọn JDK 21. Nếu danh sách chưa có JDK 21, bấm **Add… → Standard VM**, chọn thư mục cài JDK rồi chọn nó làm mặc định.

### 2. Tạo cơ sở dữ liệu

Mở [database/sqlserver/create-database.sql](database/sqlserver/create-database.sql) bằng SSMS và chạy script. Script tạo cơ sở dữ liệu `MoHinhTieuDuong`, hai bảng lưu dữ liệu và chỉ mục. Tài khoản dùng để chạy ứng dụng cần quyền đọc/ghi dữ liệu và tạo bảng/chỉ mục; tài khoản quản trị SQL có thể dùng để thiết lập lần đầu.

Ứng dụng mặc định kết nối SQL Server ở máy này, instance `SQLEXPRESS`:

```text
jdbc:sqlserver://localhost;instanceName=SQLEXPRESS;databaseName=MoHinhTieuDuong;encrypt=false;trustServerCertificate=true
```

Nếu dùng instance khác hoặc SQL Server chạy ở máy/cổng khác, cập nhật `SQLSERVER_URL`. Đảm bảo dịch vụ SQL Server đang chạy và bật **TCP/IP**. Kết nối theo tên instance cần SQL Server Browser đang chạy; nếu không muốn dùng Browser, đặt cổng TCP cố định cho SQL Server rồi dùng địa chỉ `localhost:<cổng>` trong URL.

### 3. Nhập dự án vào Eclipse

1. Chọn **File → Import… → Gradle → Existing Gradle Project → Next**.
2. Ở **Project root directory**, chọn thư mục chứa `settings.gradle` và `gradlew.bat` (thư mục gốc `Doan`, không chọn riêng `desktop-app` hay `java-backend`).
3. Chọn dùng **Gradle Wrapper**, rồi bấm **Finish**.
4. Chờ Gradle tải thư viện và đồng bộ xong. Nếu có lỗi, nhấp phải dự án gốc **Doan → Gradle → Refresh Gradle Project**.

### 4. Tạo cấu hình chạy và nhập thông tin SQL

1. Chọn **Run → Run Configurations…**.
2. Chọn **Gradle Task**, bấm nút tạo cấu hình mới.
3. Đặt tên tùy ý, chọn dự án gốc **Doan**, và nhập task `:desktop-app:run`.
4. Mở thẻ **Environment**, bấm **New…**, rồi thêm:

```text
SQLSERVER_USERNAME=<tên đăng nhập SQL Server>
SQLSERVER_PASSWORD=<mật khẩu SQL Server>
```

Nếu không dùng `SQLEXPRESS`, thêm `SQLSERVER_URL` với địa chỉ kết nối của bạn. Không cần thêm `SECRET_KEY` để chạy trên máy cá nhân. Bấm **Apply → Run**. Lần sau có thể chọn cấu hình đã tạo trong nút **Run**.

Lần đầu chạy, Gradle cần Internet để tải thư viện. Ứng dụng không thể mở nếu chưa kết nối được SQL Server. Trên máy hiện tại, dịch vụ **SQL Server Browser đang bị tắt**; nhấn `Win + R`, nhập `services.msc`, tìm **SQL Server Browser**, mở **Properties**, đổi **Startup type** thành **Automatic**, rồi bấm **Start** nếu dùng URL instance `SQLEXPRESS`. Hoặc cấu hình cổng TCP cố định và sửa `SQLSERVER_URL`.

## Chạy trên Windows không qua Eclipse

Mở PowerShell tại thư mục gốc dự án, đặt thông tin SQL của bạn rồi chạy:

```powershell
$env:SQLSERVER_USERNAME = "<tên đăng nhập SQL Server>"
$env:SQLSERVER_PASSWORD = "<mật khẩu SQL Server>"
# Chỉ cần đặt URL này nếu không dùng SQLEXPRESS mặc định:
$env:SQLSERVER_URL = "jdbc:sqlserver://localhost;instanceName=SQLEXPRESS;databaseName=MoHinhTieuDuong;encrypt=false;trustServerCertificate=true"
.\gradlew.bat :desktop-app:run
```

Tạo bộ chạy độc lập (sau đó có thể chạy bằng tệp `.bat`):

```powershell
.\gradlew.bat :desktop-app:installDist
.\desktop-app\build\install\desktop-app\bin\desktop-app.bat
```

## Tính năng

- Dự đoán nguy cơ từ các chỉ số sức khỏe và hiển thị mức thấp, trung bình hoặc cao.
- Đăng ký, đăng nhập và phân quyền tài khoản quản trị.
- Lưu, lọc, xem và xóa lịch sử dự đoán; xem thống kê từ dữ liệu đã lưu.

Kết quả chỉ có tính chất tham khảo, không thay thế chẩn đoán của nhân viên y tế. Dữ liệu từ MongoDB cũ không tự chuyển sang SQL Server.

## Kiểm tra dự án

Chạy tại thư mục gốc bằng PowerShell:

```powershell
.\gradlew.bat :java-backend:test :desktop-app:build
```

Lệnh này chạy kiểm thử backend và biên dịch/đóng gói ứng dụng desktop.
