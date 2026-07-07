# Spec: Admin Role Web App + Spring Boot Permission Split

* **Tác giả:** Claude
* **Ngày tạo:** 2026-07-07
* **Trạng thái:** Chờ duyệt

---

## 1. Mục tiêu

Thêm role `ADMIN` là vai trò cao nhất của MyFschool để cấu hình toàn hệ thống qua web app React và backend Spring Boot. Admin không có bảng riêng; admin là một bản ghi trong bảng `users` với `role = ADMIN`.

Mục tiêu v1:
- Admin đăng nhập web bằng JWT hiện có.
- Admin xem/lọc tài khoản và khóa/mở tài khoản.
- Admin cấu hình học vụ: lớp, môn, học kỳ, phân công giáo viên, thời khóa biểu, học phí, thông báo.
- Teacher không còn quyền cấu hình hệ thống. Teacher chỉ giữ nghiệp vụ giáo viên và quyền đọc dữ liệu cần cho UI teacher.
- Tài liệu test case viết theo `00-policytest` và `06-Testing` hiện có.

---

## 2. Phạm vi

### 2.1 In scope

| Nhóm | Phạm vi |
| :--- | :--- |
| Auth/Role | Thêm `ADMIN` vào enum backend, JWT claim role, authorization Spring Security. |
| Admin Web | Tạo app React riêng dưới `admin-web/`, đăng nhập, dashboard, menu quản trị. |
| User Management v1 | Xem danh sách user, lọc theo role/status/keyword, khóa/mở tài khoản. |
| Academic Config | Admin CRUD cấu hình lớp, môn, học kỳ, phân công, lịch, học phí, thông báo. |
| Permission Split | API đọc giữ cho actor cần xem; API ghi cấu hình chuyển sang admin only. |
| Test Docs | Tạo `06-Testing/TEST_CASES/UC09_AdminRole_TESTCASE.md` theo template, cập nhật index. |

### 2.2 Out of scope v1

| Ngoài phạm vi | Lý do |
| :--- | :--- |
| Tạo user thủ công | Dễ tạo dữ liệu lệch vì chưa map student/parent/teacher profile. |
| Import account CSV/Excel | Tách phase riêng vì cần validate, preview lỗi, mapping lớp/mã học sinh/phụ huynh/giáo viên, rollback. |
| Bảng `admin` riêng | Không cần; `users.role = ADMIN` đủ cho v1. |
| Phân quyền con như `ADMIN_ACCOUNTS`, `ADMIN_ACADEMIC` | Chỉ thêm khi có nhiều loại admin thật. |
| Audit log | Thêm khi cần truy vết thao tác admin theo compliance. |
| Admin làm thay nghiệp vụ teacher | V1 admin cấu hình hệ thống, không nhập điểm/điểm danh/duyệt đơn thay teacher. |

---

## 3. Kiến trúc quyền

### 3.1 Role model

Backend hiện có `UserRole` trong `backend/src/main/java/vn/edu/fpt/myfschool/common/enums/UserRole.java`:

```java
public enum UserRole {
    PARENT, STUDENT, TEACHER, ADMIN
}
```

Không thêm bảng admin. Seed hoặc migration thêm một user admin vào bảng `users`.

### 3.2 Authority

`CustomUserDetails.getAuthorities()` hiện tạo authority theo role:

```java
return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
```

Vì vậy thêm `ADMIN` vào enum là đủ để Spring Security nhận `ROLE_ADMIN`.

### 3.3 Quyền admin

`ADMIN` được phép:
- Vào admin web.
- Xem/lọc user.
- Khóa/mở user.
- CRUD cấu hình học vụ: lớp, môn, học kỳ, phân công giáo viên, thời khóa biểu, học phí, thông báo.

### 3.4 Quyền teacher sau khi tách

`TEACHER` được phép:
- Điểm danh.
- Nhập/sửa điểm.
- Duyệt/từ chối đơn nghỉ.
- Gửi thông báo trong phạm vi lớp của mình.
- Xem dữ liệu cần cho UI teacher: lớp, môn, học kỳ, học sinh, lịch, thông báo liên quan.

`TEACHER` không được:
- Tạo/sửa/xóa lớp.
- Tạo/sửa/xóa môn.
- Tạo/sửa/xóa học kỳ hoặc đặt học kỳ hiện tại.
- Phân công giáo viên/môn/lớp.
- Tạo/sửa/xóa lịch hệ thống.
- Tạo/xóa học phí cấu hình.
- Gọi API `/api/admin/**`.

---

## 4. Backend design

### 4.1 Security rules

Quy tắc chung:
- API read: giữ `PARENT`, `STUDENT`, `TEACHER` nếu UI hiện tại cần; thêm `ADMIN` nếu admin web cần xem.
- API write cấu hình hệ thống: `hasRole('ADMIN')`.
- API nghiệp vụ teacher: giữ `hasRole('TEACHER')` hoặc `hasAnyRole('TEACHER','ADMIN')` chỉ khi admin web thật sự cần gọi.
- API admin: `hasRole('ADMIN')`.

Ví dụ:

```java
@PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
@GetMapping
public ResponseEntity<ApiResponse<List<SubjectDto>>> listSubjects(...) { ... }

@PreAuthorize("hasRole('ADMIN')")
@PostMapping
public ResponseEntity<ApiResponse<SubjectDto>> createSubject(...) { ... }
```

### 4.2 Admin user API v1

Tạo controller riêng:

```text
backend/src/main/java/vn/edu/fpt/myfschool/controller/AdminUserController.java
backend/src/main/java/vn/edu/fpt/myfschool/service/AdminUserService.java
backend/src/main/java/vn/edu/fpt/myfschool/common/dto/AdminUserDto.java
backend/src/main/java/vn/edu/fpt/myfschool/common/dto/UpdateUserStatusRequest.java
```

Endpoints:

```http
GET /api/admin/users?role=&status=&keyword=&page=&size=
PUT /api/admin/users/{id}/status
```

Không có `POST /api/admin/users` trong v1.

### 4.3 User status update

Input tối thiểu:

```json
{
  "status": "ACTIVE"
}
```

Allowed status dùng enum hiện có `UserStatus`. API trả `400` nếu status sai, `404` nếu user không tồn tại.

### 4.4 Academic config permissions

Các controller cần rà soát permission:
- `ClassController`
- `SubjectController`
- `SemesterController`
- `ScheduleController`
- `TuitionBillController`
- `AnnouncementController`
- các endpoint phân công giáo viên/môn/lớp hiện có trong class/subject service

Rule:
- `GET`: actor hiện tại cần xem thì giữ; thêm admin nếu admin web dùng.
- `POST`, `PUT`, `DELETE` cấu hình: admin only.
- Endpoint teacher nghiệp vụ: teacher only.

---

## 5. React admin web design

Tạo app mới dưới `admin-web/`.

```text
admin-web/
├── src/
│   ├── api/
│   │   ├── client.ts
│   │   └── auth.ts
│   ├── pages/
│   │   ├── LoginPage.tsx
│   │   ├── UsersPage.tsx
│   │   ├── ClassesPage.tsx
│   │   ├── SubjectsPage.tsx
│   │   ├── SemestersPage.tsx
│   │   ├── AssignmentsPage.tsx
│   │   ├── SchedulesPage.tsx
│   │   ├── TuitionPage.tsx
│   │   └── AnnouncementsPage.tsx
│   ├── App.tsx
│   ├── main.tsx
│   └── styles.css
```

YAGNI choices:
- Dùng `fetch`, không thêm API client dependency.
- Dùng `localStorage` cho JWT v1.
- Không thêm state manager.
- Router có thể dùng hash route hoặc state đơn giản; thêm `react-router` chỉ khi navigation phức tạp.
- Không thêm UI library; CSS thường đủ cho admin v1.

Admin menu v1:
- Tài khoản
- Lớp học
- Môn học
- Học kỳ
- Phân công GV
- Thời khóa biểu
- Học phí
- Thông báo

---

## 6. Data flow

### 6.1 Login admin web

```text
Admin Web Login
→ POST /api/auth/login
→ backend trả JWT + user.role
→ nếu role != ADMIN: chặn vào admin web
→ lưu token localStorage
→ gọi API admin/config bằng Authorization: Bearer <token>
```

Frontend chặn UI để UX rõ. Backend vẫn là nguồn quyền thật.

### 6.2 User management v1

```text
UsersPage
→ GET /api/admin/users?role=&status=&keyword=
→ AdminUserController
→ AdminUserService
→ UserRepository
→ AdminUserDto[]
```

Khóa/mở user:

```text
UsersPage action
→ PUT /api/admin/users/{id}/status
→ validate status
→ update users.status
→ trả AdminUserDto
```

### 6.3 Academic config

```text
Academic pages
→ GET/POST/PUT/DELETE /api/classes, /api/subjects, /api/semesters, /api/schedules, /api/tuition, /api/announcements
→ controller hiện có
→ service hiện có
→ repository hiện có
```

Admin write được cấu hình. Teacher chỉ read hoặc gọi nghiệp vụ giáo viên.

---

## 7. Error handling

Backend dùng `GlobalExceptionHandler` hiện có.

| Trường hợp | HTTP status | UI xử lý |
| :--- | :---: | :--- |
| Chưa đăng nhập/token sai | `401` | Xóa token, về login. |
| Không phải admin gọi `/api/admin/**` | `403` | Hiện “Bạn không có quyền truy cập chức năng này.” |
| Filter/status/role sai | `400` | Hiện message cạnh form/filter. |
| Không tìm thấy user/config entity | `404` | Hiện message từ backend. |
| Trùng tên lớp theo năm học, mã môn, hoặc học kỳ hiện hành | `409` | Hiện message conflict, không retry tự động. |
| Network fail | client-side | Hiện “Không kết nối được máy chủ.” |

Không log password/JWT đầy đủ trong evidence hoặc server log.

---

## 8. Test documentation

Test case phải viết theo template hiện có:
- Policy/guideline: `00-policytest/TESTING_GUIDELINES.md`
- Hướng dẫn viết test case: `00-policytest/TESTCASE_WRITING_GUIDE.md`
- Template: `06-Testing/TEST_CASES/UC_TESTCASE_TEMPLATE.md`

Tạo mới:

```text
06-Testing/TEST_CASES/UC09_AdminRole_TESTCASE.md
```

Cập nhật:

```text
06-Testing/TEST_CASES/TESTCASE_INDEX.md
```

Coverage UC09:
- Admin login, JWT role `ADMIN`.
- Non-admin bị chặn khỏi admin web/API.
- Admin xem/lọc user.
- Admin khóa/mở user.
- Admin write cấu hình học vụ.
- Teacher write cấu hình bị `403`.
- Teacher read/API nghiệp vụ vẫn hoạt động.
- Import account nằm ngoài phạm vi v1.

---

## 9. Verification plan

Backend:

```bash
cd backend
mvn test
```

Admin web:

```bash
cd admin-web
npm run build
```

Manual smoke:
1. Login admin vào admin web.
2. Vào Tài khoản, list/filter user, khóa/mở một user test.
3. Vào Lớp/Môn/Học kỳ, tạo/sửa/xóa cấu hình test.
4. Login teacher, xác nhận không vào admin web.
5. Teacher vẫn xem được dữ liệu cần cho UI teacher và dùng nghiệp vụ teacher.

---

## 10. Acceptance criteria

- `users.role = ADMIN` hoạt động qua JWT/Spring Security.
- Admin web chỉ cho `ADMIN` vào.
- `/api/admin/**` chỉ cho `ADMIN`.
- Teacher không còn quyền write cấu hình hệ thống.
- Teacher UI không bị mất quyền read cần thiết.
- Không có bảng admin mới.
- Không có tạo user thủ công trong v1.
- Test case UC09 được tạo đúng template và index được cập nhật.
