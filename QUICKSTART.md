# Chạy nhanh trong Eclipse

Ứng dụng cần **JDK 21**, Eclipse có **Gradle Buildship**, và **SQL Server đang chạy**.

1. Mở `database/sqlserver/create-database.sql` trong SSMS và chạy để tạo database `MoHinhTieuDuong`.
2. Trong Eclipse chọn **File → Import… → Gradle → Existing Gradle Project**.
3. Chọn thư mục gốc dự án (thư mục có `settings.gradle` và `gradlew.bat`), rồi bấm **Finish**. Chờ Gradle đồng bộ xong.
4. Chọn **Run → Run Configurations… → Gradle Task**, bấm nút tạo cấu hình mới.
5. Chọn dự án gốc **Doan**, nhập task `:desktop-app:run`, rồi mở thẻ **Environment**.
6. Bấm **New…** để thêm `SQLSERVER_USERNAME` và `SQLSERVER_PASSWORD`. Nếu SQL Server không phải instance `SQLEXPRESS` mặc định, thêm `SQLSERVER_URL`. Bấm **Apply → Run**.

Cửa sổ desktop sẽ mở; backend chạy tự động. Không ghi mật khẩu vào mã nguồn. Nếu Gradle báo sai phiên bản Java, vào **Window → Preferences → Java → Installed JREs** và chọn JDK 21.

## Lỗi thường gặp

- **Không kết nối được SQL Server:** kiểm tra dịch vụ SQL Server, TCP/IP, tên instance, tài khoản/mật khẩu và quyền trên database. Nếu dùng `SQLEXPRESS`, bật dịch vụ **SQL Server Browser** trong Windows Services; nếu không, cấu hình cổng TCP cố định và đặt `SQLSERVER_URL`.
- **Không tìm thấy database:** chạy script trong SSMS trước; kiểm tra database có tên `MoHinhTieuDuong`.
- **Gradle không tải được thư viện:** kiểm tra Internet rồi nhấp phải dự án **Doan → Gradle → Refresh Gradle Project**.
- **Không có cửa sổ hoặc chương trình dừng khi khởi động:** đọc thông báo lỗi trong hộp thoại và tab **Console** của Eclipse. Ứng dụng cần SQL Server để khởi động.

Kiểm tra biên dịch và kiểm thử từ PowerShell tại thư mục dự án:

```powershell
.\gradlew.bat :java-backend:test :desktop-app:build
```
