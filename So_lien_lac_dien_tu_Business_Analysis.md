# Thiết kế nghiệp vụ Sổ liên lạc điện tử

## 1. Mục tiêu

Thiết kế hệ thống quản lý sổ liên lạc điện tử theo mô hình nhiều năm
học, nhiều học kỳ, lưu được lịch sử và dễ mở rộng.

------------------------------------------------------------------------

# 2. Quy trình cấu hình của Admin

## Bước 1. Khởi tạo năm học

-   Admin tạo Năm học.
-   Hệ thống tự sinh:
    -   Học kỳ 1
    -   Học kỳ 2

### Trạng thái

  Đối tượng    Trạng thái ban đầu
  ------------ --------------------
  SchoolYear   DRAFT
  Semester 1   NOT_STARTED
  Semester 2   NOT_STARTED

### Business Rules

-   Chỉ có 1 năm học ACTIVE.
-   DRAFT được sửa.
-   ACTIVE không được sửa ngày bắt đầu/kết thúc.
-   ACTIVE không được xóa.

------------------------------------------------------------------------

## Bước 2. Cấu hình năm học

Khuyến nghị tách Master Data khỏi dữ liệu năm học.

### Master Data

-   Khối lớp
-   Môn học
-   Ca học
-   Tiết học

### Theo năm học

-   Môn áp dụng
-   Ca học áp dụng
-   Tiết học áp dụng

Không nên tạo lại dữ liệu master cho từng năm học.

------------------------------------------------------------------------

## Bước 3. Quản lý giáo viên

Teacher là dữ liệu dùng nhiều năm.

Không tạo lại giáo viên mỗi năm.

Chỉ tạo phân công theo năm học.

TeacherAssignment

-   Teacher
-   SchoolYear
-   Semester
-   Subject
-   Role

Role

-   Homeroom Teacher
-   Subject Teacher

------------------------------------------------------------------------

## Bước 4. Sinh lớp hàng loạt

Ví dụ

Khối 10

Số lớp: 10

Kiểu đặt tên: A

Sinh

-   10A1
-   ...
-   10A10

Business Rules

-   Tên lớp không trùng trong năm học.
-   Một lớp chỉ có một GVCN.

------------------------------------------------------------------------

## Bước 5. Import học sinh & phụ huynh

Import Excel.

Thông tin

### Student

-   Họ tên
-   Ngày sinh
-   Giới tính
-   Địa chỉ
-   Lớp

### Parent

-   Họ tên
-   Quan hệ
-   SĐT
-   Nghề nghiệp
-   Địa chỉ

Sau khi import

-   Tự tạo tài khoản Student
-   Tự tạo tài khoản Parent

Khuyến nghị

Username Student = StudentCode

Username Parent = Phone

Bắt buộc đổi mật khẩu lần đầu.

Validation

-   Lớp tồn tại
-   SĐT hợp lệ
-   Không trùng mã HS
-   Không trùng Email
-   Không trùng CCCD (nếu có)

Nếu lỗi

Xuất file error.xlsx.

------------------------------------------------------------------------

## Bước 6. Phân công giảng dạy

Admin chọn

Lớp

↓

Môn

↓

Giáo viên

Business Rules

-   Một lớp + môn + học kỳ chỉ có một giáo viên.
-   Giáo viên có thể dạy nhiều lớp.
-   Không phân công giáo viên chưa thuộc năm học.

------------------------------------------------------------------------

## Bước 7. Kiểm tra dữ liệu

Hệ thống tự kiểm tra

-   Có GVCN
-   Có học sinh
-   Có phân công môn
-   Không trùng dữ liệu

Nếu còn lỗi

Không cho ACTIVE.

------------------------------------------------------------------------

## Bước 8. ACTIVE

Sau khi ACTIVE

-   Không sửa cấu hình quan trọng.
-   Bắt đầu nhập điểm.
-   Điểm danh.
-   Sổ liên lạc hoạt động.

------------------------------------------------------------------------

# 3. Business Process

``` text
Create School Year
        ↓
Configure School Year
        ↓
Add Teachers
        ↓
Generate Classes
        ↓
Import Students
        ↓
Import Parents
        ↓
Assign Homeroom Teacher
        ↓
Assign Subject Teacher
        ↓
Validate
        ↓
Activate School Year
        ↓
Running
```

------------------------------------------------------------------------

# 4. BPMN (Logic)

``` text
Start
 ↓
Create SchoolYear
 ↓
Auto Create Semester
 ↓
Configure Master Data
 ↓
Import Teacher
 ↓
Generate Class
 ↓
Import Student
 ↓
Assign Teachers
 ↓
Validate
 ↓
Activate
 ↓
End
```

------------------------------------------------------------------------

# 5. Business Rules

## School Year

-   SY-001 Chỉ có một ACTIVE.
-   SY-002 ACTIVE không sửa.
-   SY-003 DRAFT được sửa.
-   SY-004 ACTIVE không xóa.

## Semester

-   SE-001 HK1 trước HK2.
-   SE-002 Chỉ ACTIVE một học kỳ.
-   SE-003 Không xóa.

## Teacher

-   TE-001 SĐT duy nhất.
-   TE-002 Email duy nhất.
-   TE-003 Một giáo viên dạy nhiều lớp.
-   TE-004 Có thể vừa GVCN vừa GVBM.

## Class

-   CL-001 Không trùng tên.
-   CL-002 Một GVCN.
-   CL-003 Không xóa nếu có học sinh.

## Student

-   ST-001 Mã HS duy nhất.
-   ST-002 Một học sinh thuộc một lớp trong một năm học.
-   ST-003 Không import nếu lớp chưa tồn tại.

## Parent

-   PA-001 Một phụ huynh có nhiều con.
-   PA-002 SĐT duy nhất.

## Teaching Assignment

-   TA-001 Một lớp + môn + học kỳ chỉ có một giáo viên.
-   TA-002 Không phân công trùng.
-   TA-003 Không sửa khi học kỳ đã khóa.

------------------------------------------------------------------------

# 6. Quyền Giáo viên Chủ nhiệm

## Quản lý

-   Xem lớp chủ nhiệm
-   Xem học sinh
-   Xem phụ huynh

## Điểm danh

-   Điểm danh theo ngày

## Hạnh kiểm

-   Nhận xét tuần
-   Nhận xét tháng
-   Nhận xét học kỳ
-   Xếp loại hạnh kiểm

## Liên lạc

-   Gửi thông báo
-   Duyệt đơn nghỉ học

## Báo cáo

-   Sĩ số
-   Chuyên cần
-   Hạnh kiểm
-   Học lực

## Điểm

-   Xem toàn bộ điểm lớp
-   Không sửa điểm môn của giáo viên khác

------------------------------------------------------------------------

# 7. Quyền Giáo viên Bộ môn

-   Xem lớp được phân công
-   Nhập điểm
-   Sửa điểm trong thời gian cho phép
-   Nhập nhận xét môn học
-   Điểm danh theo tiết (nếu áp dụng)
-   Xem lịch dạy
-   Giao bài tập
-   Xem thống kê môn học

Không được

-   Xếp loại hạnh kiểm
-   Duyệt đơn nghỉ
-   Xem dữ liệu ngoài phạm vi phân công

------------------------------------------------------------------------

# 8. So sánh quyền

  Chức năng              GVCN          GVBM
  ---------------------- ------------- ------------------------
  Xem học sinh           ✔             ✔ (lớp được phân công)
  Điểm danh ngày         ✔             ✘
  Điểm danh tiết         Có thể        ✔
  Nhập điểm              Nếu dạy môn   ✔
  Nhận xét môn           Nếu dạy môn   ✔
  Nhận xét chủ nhiệm     ✔             ✘
  Hạnh kiểm              ✔             ✘
  Xem toàn bộ điểm lớp   ✔             ✘
  Gửi thông báo lớp      ✔             Giới hạn theo môn
  Duyệt đơn nghỉ         ✔             ✘

------------------------------------------------------------------------

# 9. Khuyến nghị kiến trúc

-   Chỉ có một tài khoản `TEACHER`.
-   Quyền được xác định bởi phân công:
    -   Homeroom Assignment
    -   Subject Assignment
-   Dữ liệu luôn gắn với:
    -   SchoolYear
    -   Semester
-   Khóa dữ liệu sau khi học kỳ/năm học kết thúc.
-   Tách Master Data khỏi dữ liệu nghiệp vụ.
-   Thiết kế theo RBAC kết hợp Data Scope để mở rộng dễ dàng.
