# Dữ liệu demo toàn diện

Backend tự tạo dữ liệu demo khi chạy profile `dev`. Bộ dữ liệu dùng chung cho REST API và
`admin-web`; dữ liệu nghiệp vụ được gắn đúng năm học để kiểm thử việc cô lập theo năm.

## Khởi động

1. Tạo database MySQL `fschool` và kiểm tra thông tin kết nối trong
   `backend/src/main/resources/application-dev.yml`.
2. Chạy backend:

   ```bash
   cd backend
   mvn spring-boot:run
   ```

3. Chạy admin web:

   ```bash
   cd admin-web
   npm install
   npm run dev
   ```

Seeder chạy trong một transaction và dùng tài khoản admin làm dấu nhận biết. Khởi động lại
backend không tạo bản ghi trùng. Muốn tạo lại từ đầu, hãy xóa database/schema local rồi khởi
động backend.

Có thể điều khiển bằng biến môi trường:

| Biến | Mặc định | Ý nghĩa |
|---|---|---|
| `DEMO_DATA_ENABLED` | `true` | Bật/tắt seed ở profile `dev` |
| `DEMO_DATA_PASSWORD` | `Demo@123` | Mật khẩu chung của tài khoản demo |

## Tài khoản chính

Tất cả tài khoản dưới đây dùng mật khẩu `Demo@123` nếu không đổi biến môi trường.

| Vai trò | Số điện thoại | Tên | Dùng để kiểm thử |
|---|---|---|---|
| Admin | `0868589707` | Quản trị Demo | Toàn bộ cổng quản trị |
| Giáo viên | `0901000001` | Nguyễn Thu Hà | GVCN 12A1, dạy Toán/Tin |
| Giáo viên | `0901000002` | Trần Minh Anh | Dạy Ngữ văn, thông báo tự động kiểm tra chính sách |
| Phụ huynh | `0902000001` | Nguyễn Văn Hùng | Hai con ở 12A1 và 10A1 |
| Học sinh | `0903000001` | Nguyễn Minh An | Điểm, chuyên cần, học phí, tin nhắn |
| Học sinh | `0903000003` | Lê Mai Chi | Tin nhắn chưa đọc |

Tài khoản trạng thái biên dành cho trang quản lý người dùng:

- Giáo viên `0901000099`: trạng thái `INACTIVE`.
- Phụ huynh `0902000099`: trạng thái `LOCKED`.

## Phạm vi dữ liệu

| Nhóm | Kịch bản có sẵn |
|---|---|
| Năm học | `2025-2026` hoàn tất, `2026-2027` đang hoạt động, `2027-2028` là bản nháp thiếu cấu hình |
| Danh mục | Trường, khối 10-12, 2 ca, 10 tiết, 7 môn, phòng học, cấu hình đầu điểm |
| Lớp và phân công | 4 lớp qua 2 năm, GVCN, giáo viên bộ môn, học sinh lên lớp và học sinh ở nhiều lớp |
| Thời khóa biểu | Trạng thái `ACTIVE`, `DRAFT`, `SCHEDULED`, `ARCHIVED`; có cả sáng và chiều |
| Điểm danh | Nhiều ngày, có mặt, nghỉ có phép, nghỉ không phép, phiên mở/đóng và yêu cầu điều chỉnh |
| Đơn nghỉ | `PENDING`, `APPROVED`, `REJECTED`, có tệp đính kèm và liên kết điểm danh |
| Bảng điểm | `DRAFT`, `SUBMITTED`, `PUBLISHED`, `LOCKED`; điểm đủ, điểm thiếu, lịch sử sửa điểm và xếp hạng học kỳ |
| Học phí | `UNPAID`, `PROCESSING`, `PAID`; giao dịch `PENDING`, `SUCCESS`, `FAILED` |
| Thông báo | Theo lớp hoặc toàn bộ tài khoản không phải Admin; `PUBLISHED`, `SYSTEM_REJECTED`, đã đọc/chưa đọc; có cấu hình nhiều câu từ theo năm học |
| Tin nhắn | Phụ huynh–giáo viên, học sinh–giáo viên, trạng thái đã đọc/chưa đọc và tệp đính kèm |
| Hệ thống | Thông báo cá nhân và nhật ký thao tác admin |

## Kịch bản kiểm thử nhanh

1. Đăng nhập admin, chuyển giữa ba năm học và xác nhận dữ liệu lớp không bị lẫn.
2. Ở năm `2026-2027`, kiểm tra 12A1 để thấy đầy đủ lịch, điểm, điểm danh và thông báo.
3. Mở `2027-2028` để kiểm tra màn hình validation/activation khi thiếu cấu hình.
4. Đăng nhập lần lượt giáo viên, phụ huynh, học sinh qua API để kiểm tra phân quyền và dữ liệu cá nhân.
5. Dùng Swagger tại `http://localhost:8080/swagger-ui.html` để gọi các API theo vai trò.

Flutter mobile hiện vẫn dùng luồng chọn vai trò và dữ liệu local ở một số màn hình; tài khoản
demo có hiệu lực ngay cho backend và admin web, đồng thời là bộ dữ liệu chuẩn để nối mobile vào
REST API.
