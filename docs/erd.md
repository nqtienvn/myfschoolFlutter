# MyFschool — ERD (v5 — Final)

Tài liệu mô tả tất cả các bảng MySQL và mối quan hệ cho ứng dụng Sổ liên lạc điện tử.
Tối ưu hiệu năng (index, partition) và dễ scale.

---

## Tổng quan

**Tổng cộng: 26 bảng**

| Nhóm | Bảng |
|------|------|
| **Tài khoản** | `users` |
| **Actor** | `parents`, `students`, `teachers` |
| **Liên kết** | `student_guardians`, `student_classes`, `class_subjects`, `announcement_classes` |
| **Giáo dục** | `classes`, `subjects`, `semesters`, `schedules` |
| **Học tập** | `grades`, `semester_results`, `attendance`, `leave_requests` |
| **Học phí** | `tuition_bills`, `payment_transactions` |
| **Giao tiếp** | `conversations`, `conversation_participants`, `messages`, `announcements`, `announcement_reads` |
| **Hệ thống** | `notifications`, `club_registrations`, `attachments` |

---

## ERD — Bảng & Mối quan hệ

### Nhóm 1: Tài khoản & Vai trò

```text
┌─────────────────────────────────────────────────────────────────────┐
│                          TÀI KHOẢN & VAI TRÒ                        │
└─────────────────────────────────────────────────────────────────────┘

                          ┌──────────────┐
                          │     users     │
                          └──────┬───────┘
                                 │
            ┌────────────────────┼────────────────────┐
            │ 1:1               │ 1:1                │ 1:1
            ▼                    ▼                    ▼
       ┌──────────┐        ┌──────────┐        ┌──────────┐
       │ parents  │        │ students │        │ teachers │
       └──────────┘        └──────────┘        └──────────┘
             │                  ▲   ▲
             │   ┌──────────────┘   └──────────────┐
             │   │                                 │
             ▼   │  ┌─────────────────────────┐    │
       ┌─────────┴──┤  student_guardians       │────┘
       │            │  (father / mother /      │
       │            │   guardian)              │
       │            └─────────────────────────┘
       │
       └── 1 PH có thể nhận nhiều đơn / xem nhiều con
```

### Nhóm 2: Lớp học & Môn học

```text
┌─────────────────────────────────────────────────────────────────────┐
│                          LỚP HỌC & MÔN HỌC                         │
└─────────────────────────────────────────────────────────────────────┘

       ┌──────────┐       ┌─────────────────┐       ┌──────────┐
       │ classes  │◄─────►│ class_subjects   │◄─────►│ subjects │
       └────┬─────┘       └────────┬────────┘       └──────────┘
            │                      │
            │                      ▼
            │                ┌──────────┐
            │                │ teachers │
            │                └──────────┘
            │
            │         ┌──────────────────────┐
            └────────►│  student_classes      │
                      │  (lịch sử lớp theo năm)│
                      └──────────┬───────────┘
                                 │
                           ┌─────┴─────┐
                           │ students  │
                           └───────────┘
```

### Nhóm 3: Thời khóa biểu & Học kỳ

```text
┌─────────────────────────────────────────────────────────────────────┐
│                    THỜI KHÓA BIỂU & HỌC KỲ                         │
└─────────────────────────────────────────────────────────────────────┘

       ┌──────────┐       ┌──────────┐       ┌──────────────┐
       │ classes  │◄─────►│schedules │◄─────►│  semesters    │
       └──────────┘       └────┬─────┘       └──────┬───────┘
                               │                    │
                  ┌────────────┼────────────┐       │
                  ▼                         ▼       │
             ┌──────────┐              ┌──────────┐ │
             │ students │              │ teachers │ │
             └──────────┘              └──────────┘ │
                                                    │
                 ┌──────────────────────────────────┘
                 │
                 │  (semester_results + grades cũng FK → semesters)
                 ▼
```

### Nhóm 4: Điểm số & Tổng kết học kỳ

```text
┌─────────────────────────────────────────────────────────────────────┐
│             ĐIỂM SỐ (chi tiết theo môn) & TỔNG KẾT HỌC KỲ          │
└─────────────────────────────────────────────────────────────────────┘

       ┌──────────┐       ┌──────────┐       ┌──────────┐
       │ students │◄─────►│  grades   │──────►│ subjects │
       └────┬─────┘       └────┬─────┘       └──────────┘
            │                  │
            │                  ▼
            │            ┌────────────┐
            │            │ semesters  │
            │            └────────────┘
            │
            ├──► ┌─────────────────┐       ┌──────────┐
            │    │   attendance     │──────►│ classes  │
            │    └────────┬────────┘       └──────────┘
            │             │
            │             │ (optional)
            │             ▼
            │      ┌──────────────┐
            │      │  schedules   │
            │      └──────────────┘
            │
            └──► ┌─────────────────┐       ┌──────────┐
                 │  leave_requests │──────►│ teachers │
                 └────────┬────────┘       └──────────┘
                          │
                  ┌───────┼────────┐
                  ▼                ▼
          ┌────────────┐   ┌────────────┐
          │attachments │   │attendance  │
          └────────────┘   │(liên kết đơn)│
                           └────────────┘

       ┌──────────┐       ┌────────────────┐
       │ students │◄─────►│semester_results│
       └──────────┘       └────────────────┘
```

### Nhóm 5: Học phí

```text
┌─────────────────────────────────────────────────────────────────────┐
│                         HỌC PHÍ                                     │
└─────────────────────────────────────────────────────────────────────┘

       ┌──────────┐       ┌───────────────┐       ┌─────────────────────┐
       │ students │◄─────►│ tuition_bills  │◄─────►│ payment_transactions│
       └──────────┘       └───────┬───────┘       └─────────────────────┘
                                  │
                    ┌─────────────┼─────────────┐
                    ▼                           ▼
              ┌──────────┐                ┌──────────┐
              │ classes  │                │ semesters│
              └──────────┘                └──────────┘

                    notifications ◄── (optional) ── tuition_bills
```

### Nhóm 6: Giao tiếp (Messenger-style)

```text
┌─────────────────────────────────────────────────────────────────────┐
│                        GIAO TIẾP                                    │
└─────────────────────────────────────────────────────────────────────┘

┌────────────────┐       ┌─────────────────────┐       ┌──────────┐
│ announcements  │◄─────►│announcement_classes  │◄─────►│ classes  │
│ (+target_role) │       └─────────────────────┘       └──────────┘
└───────┬────────┘
        │
        ▼
┌──────────────────┐
│announcement_reads│
└──────────────────┘

┌──────────────────┐       ┌──────────────────────────┐
│   conversations   │◄─────►│conversation_participants  │
│                    │       └──────────────────────────┘
└────────┬─────────┘              │
         │                        ▼
         │                  ┌──────────┐
         │                  │  users   │
         │                  └──────────┘
         ▼
   ┌──────────┐        ┌─────────────┐
   │ messages │◄──────►│ attachments │
   └──────────┘        └─────────────┘

┌──────────────┐       ┌─────────────────────┐
│ notifications│       │club_registrations   │
│ (→tuition_b  │       └─────────────────────┘
│  ills optional)│
└──────────────┘
```

---

## Mapping chi tiết: Bảng → UI

### Bảng `grades` → Detailed Grades Table

```text
┌─────────────────────────────────────────────────────────────────────┐
│  grades (1 dòng = 1 HS × 1 môn × 1 học kỳ)                         │
├──────────────┬──────────────────────────────────────────────────────┤
│ Field        │ UI Column tương ứng                                  │
├──────────────┼──────────────────────────────────────────────────────┤
│ student_id   │ (ẩn — xác định HS)                                  │
│ subject_id   │ Cột "Môn"                                            │
│ semester_id  │ (ẩn — Semester Selector)                             │
│ oral         │ Cột "Miệng"     (điểm miệng)                        │
│ quiz_15m     │ Cột "15p"       (điểm kiểm tra 15 phút)             │
│ mid_term     │ Cột "1 tiết"    (điểm kiểm tra 1 tiết)              │
│ final        │ Cột "HK"        (điểm học kỳ)                       │
│ average      │ Cột "TBM"       (điểm trung bình môn)               │
└──────────────┴──────────────────────────────────────────────────────┘
```

### Bảng `semester_results` → Summary Table

```text
┌─────────────────────────────────────────────────────────────────────┐
│  semester_results (1 dòng = 1 HS × 1 học kỳ)                       │
├──────────────┬──────────────────────────────────────────────────────┤
│ Field        │ UI Cell tương ứng                                    │
├──────────────┼──────────────────────────────────────────────────────┤
│ student_id   │ (ẩn — xác định HS)                                  │
│ semester_id  │ (ẩn — Semester Selector)                             │
│ gpa          │ Cell "GPA"         (điểm trung bình cả HK)          │
│ rank         │ Cell "Xếp hạng"    (số thứ tự trong lớp, VD: 5/42) │
│ honor        │ Cell "Danh hiệu"   (Giỏi/Khá/Trung bình)           │
│ conduct      │ Cell "Hạnh kiểm"   (Tốt/Khá/Trung bình/Yếu)       │
│ academic_    │ Cell "Học lực"     (Giỏi/Khá/Trung bình/Yếu)       │
│   ability    │                                                       │
└──────────────┴──────────────────────────────────────────────────────┘
```

### Bảng `announcements` + `announcement_classes` → Announcements Screen

```text
┌─────────────────────────────────────────────────────────────────────┐
│  announcements (1 dòng = 1 thông báo)                               │
│  announcement_classes (n dòng = thông báo gửi n lớp)                │
├──────────────┬──────────────────────────────────────────────────────┤
│ Field        │ UI tương ứng                                         │
├──────────────┼──────────────────────────────────────────────────────┤
│ title        │ Title card thông báo                                 │
│ body         │ Body nội dung chi tiết                               │
│ target_role  │ Filter: PARENT / STUDENT / ALL                       │
│ teacher_id   │ (ẩn — GV tạo)                                        │
│ announcement │ Liên kết lớp nhận thông báo                          │
│   _classes   │                                                      │
│ announcement │ Trạng thái đã đọc / chưa đọc                        │
│   _reads     │                                                      │
└──────────────┴──────────────────────────────────────────────────────┘
```

---

## Danh sách mối quan hệ chi tiết

| # | Bảng cha | Bảng con | Kiểu | FK | Ý nghĩa |
|---|----------|----------|------|----|----------|
| 1 | `users` | `parents` | 1:1 | `parent.user_id` | Tài khoản phụ huynh |
| 2 | `users` | `students` | 1:1 | `student.user_id` | Tài khoản học sinh |
| 3 | `users` | `teachers` | 1:1 | `teacher.user_id` | Tài khoản giáo viên |
| 5 | `parents` | `student_guardians` | 1:N | `sg.guardian_id` | 1 PH liên kết nhiều HS |
| 6 | `students` | `student_guardians` | 1:N | `sg.student_id` | 1 HS có nhiều người giám hộ |
| 7 | `students` | `student_classes` | 1:N | `sc.student_id` | 1 HS chuyển lớp qua các năm |
| 8 | `classes` | `student_classes` | 1:N | `sc.class_id` | 1 lớp chứa nhiều HS theo năm |
| 9 | `students` | `classes` | N:1 | `student.class_id` | Lớp hiện tại của HS |
| 10 | `classes` | `class_subjects` | 1:N | `cs.class_id` | 1 lớp học nhiều môn |
| 11 | `subjects` | `class_subjects` | 1:N | `cs.subject_id` | 1 môn dạy ở nhiều lớp |
| 12 | `teachers` | `class_subjects` | 1:N | `cs.teacher_id` | 1 GV dạy nhiều lớp-môn |
| 13 | `classes` | `schedules` | 1:N | `sch.class_id` | 1 lớp có nhiều tiết TKB |
| 14 | `subjects` | `schedules` | 1:N | `sch.subject_id` | 1 môn có nhiều tiết TKB |
| 15 | `teachers` | `schedules` | 1:N | `sch.teacher_id` | 1 GV có nhiều tiết dạy |
| 16 | `semesters` | `schedules` | 1:N | `sch.semester_id` | 1 học kỳ có nhiều tiết TKB |
| 17 | `students` | `grades` | 1:N | `g.student_id` | 1 HS có nhiều dòng điểm |
| 18 | `subjects` | `grades` | 1:N | `g.subject_id` | 1 môn có nhiều điểm HS |
| 19 | `semesters` | `grades` | 1:N | `g.semester_id` | 1 học kỳ có nhiều điểm |
| 20 | `students` | `semester_results` | 1:N | `sr.student_id` | 1 HS có tổng kết nhiều HK |
| 21 | `semesters` | `semester_results` | 1:N | `sr.semester_id` | 1 HK có tổng kết nhiều HS |
| 22 | `classes` | `semester_results` | 1:N | `sr.class_id` | 1 lớp có tổng kết HK |
| 23 | `students` | `attendance` | 1:N | `a.student_id` | 1 HS có nhiều bản ghi CC |
| 24 | `teachers` | `attendance` | 1:N | `a.teacher_id` | 1 GV ghi điểm danh nhiều bản |
| 25 | `classes` | `attendance` | 1:N | `a.class_id` | 1 lớp có nhiều bản ghi CC |
| 26 | `schedules` | `attendance` | 1:N | `a.schedule_id` | (optional) Link tiết học cụ thể |
| 27 | `leave_requests` | `attendance` | 1:N | `a.leave_request_id` | (optional) Đơn nghỉ → CC tự cập nhật |
| 28 | `students` | `leave_requests` | 1:N | `lr.student_id` | 1 HS gửi nhiều đơn |
| 29 | `parents` | `leave_requests` | 1:N | `lr.parent_id` | 1 PH tạo nhiều đơn |
| 30 | `teachers` | `leave_requests` | 1:N | `lr.approved_by` | 1 GV duyệt nhiều đơn |
| 31 | `students` | `tuition_bills` | 1:N | `tb.student_id` | 1 HS có nhiều khoản HP |
| 32 | `classes` | `tuition_bills` | 1:N | `tb.class_id` | 1 lớp có nhiều bill HP |
| 33 | `semesters` | `tuition_bills` | 1:N | `tb.semester_id` | 1 học kỳ có nhiều bill HP |
| 34 | `tuition_bills` | `payment_transactions` | 1:N | `pt.bill_id` | 1 bill có nhiều giao dịch |
| 35 | `tuition_bills` | `notifications` | 1:N | `n.tuition_bill_id` | (optional) TB liên quan bill HP |
| 36 | `teachers` | `announcements` | 1:N | `ann.teacher_id` | 1 GV tạo nhiều thông báo |
| 37 | `announcements` | `announcement_classes` | 1:N | `ac.announcement_id` | 1 TB gửi nhiều lớp |
| 38 | `classes` | `announcement_classes` | 1:N | `ac.class_id` | 1 lớp nhận nhiều TB |
| 39 | `announcements` | `announcement_reads` | 1:N | `ar.announcement_id` | Theo dõi đã đọc/chưa đọc |
| 40 | `users` | `announcement_reads` | 1:N | `ar.user_id` | 1 user đọc nhiều thông báo |
| 41 | `conversations` | `conversation_participants` | 1:N | `cp.conversation_id` | 1 hộp thoại nhiều参与者 |
| 42 | `users` | `conversation_participants` | 1:N | `cp.user_id` | 1 user tham gia nhiều hộp thoại |
| 43 | `conversations` | `messages` | 1:N | `m.conversation_id` | 1 hộp thoại nhiều tin nhắn |
| 44 | `users` | `messages` | 1:N | `m.sender_id` | 1 user gửi nhiều tin nhắn |
| 45 | `leave_requests` | `attachments` | 1:N | `att.leave_request_id` | Đơn xin nghỉ có file đính kèm |
| 46 | `messages` | `attachments` | 1:N | `att.message_id` | Tin nhắn có file đính kèm |
| 47 | `users` | `notifications` | 1:N | `n.user_id` | 1 user nhận nhiều TB hệ thống |
| 48 | `students` | `club_registrations` | 1:N | `cr.student_id` | 1 HS đăng ký nhiều CLB |

---

## Chi tiết schema từng bảng

### `users`

```text
┌─────────────────────────────────────────────────────┐
│  users                                               │
├──────────────────┬──────────────────────────────────┤
│ id               │ BIGINT PK AUTO_INCREMENT          │
│ phone            │ VARCHAR(15) UNIQUE NOT NULL       │
│ password         │ VARCHAR(255) NOT NULL (bcrypt)    │
│ name             │ VARCHAR(100) NOT NULL             │
│ email            │ VARCHAR(255) NULL                 │
│ role             │ ENUM: PARENT, STUDENT, TEACHER    │
│ status           │ ENUM: ACTIVE, INACTIVE, LOCKED    │
│ created_at       │ TIMESTAMP DEFAULT CURRENT         │
│ updated_at       │ TIMESTAMP ON UPDATE CURRENT       │
├──────────────────┴──────────────────────────────────┤
│ INDEX idx_users_phone (phone)          — login lookup│
│ INDEX idx_users_role  (role)           — filter role │
└─────────────────────────────────────────────────────┘
```

### `parents`

```text
┌─────────────────────────────────────────────────────┐
│  parents                                             │
├──────────────────┬──────────────────────────────────┤
│ id               │ BIGINT PK AUTO_INCREMENT          │
│ user_id          │ BIGINT FK UNIQUE → users          │
│ address          │ VARCHAR(500) NULL                 │
│ occupation       │ VARCHAR(200) NULL                 │
│ created_at       │ TIMESTAMP DEFAULT CURRENT         │
│ updated_at       │ TIMESTAMP ON UPDATE CURRENT       │
└─────────────────────────────────────────────────────┘
```

### `students`

```text
┌─────────────────────────────────────────────────────┐
│  students                                            │
├──────────────────┬──────────────────────────────────┤
│ id               │ BIGINT PK AUTO_INCREMENT          │
│ user_id          │ BIGINT FK UNIQUE → users          │
│ student_code     │ VARCHAR(20) UNIQUE NOT NULL       │
│   (VD: 12A-01)   │                                   │
│ class_id         │ BIGINT FK → classes (lớp hiện tại)│
│ date_of_birth    │ DATE NULL                         │
│ created_at       │ TIMESTAMP DEFAULT CURRENT         │
│ updated_at       │ TIMESTAMP ON UPDATE CURRENT       │
├──────────────────┴──────────────────────────────────┤
│ INDEX idx_students_class  (class_id)  — query lớp   │
│ INDEX idx_students_code   (student_code) — lookup   │
└─────────────────────────────────────────────────────┘
```

### `teachers`

```text
┌─────────────────────────────────────────────────────┐
│  teachers                                            │
├──────────────────┬──────────────────────────────────┤
│ id               │ BIGINT PK AUTO_INCREMENT          │
│ user_id          │ BIGINT FK UNIQUE → users          │
│ employee_code    │ VARCHAR(20) UNIQUE, tự sinh       │
│                  │ (VD: GV-0001)                     │
│ created_at       │ TIMESTAMP DEFAULT CURRENT         │
│ updated_at       │ TIMESTAMP ON UPDATE CURRENT       │
└─────────────────────────────────────────────────────┘
```

### `student_guardians`

```text
┌─────────────────────────────────────────────────────┐
│  student_guardians                                   │
├──────────────────┬──────────────────────────────────┤
│ id               │ BIGINT PK AUTO_INCREMENT          │
│ student_id       │ BIGINT FK → students              │
│ guardian_id      │ BIGINT FK → parents               │
│ relationship     │ ENUM: FATHER, MOTHER, GUARDIAN    │
│ created_at       │ TIMESTAMP DEFAULT CURRENT         │
├──────────────────┴──────────────────────────────────┤
│ UNIQUE (student_id, guardian_id)                     │
│ INDEX idx_sg_guardian (guardian_id) — PH tìm HS     │
└─────────────────────────────────────────────────────┘
```

### `student_classes`

```text
┌─────────────────────────────────────────────────────┐
│  student_classes                                     │
├──────────────────┬──────────────────────────────────┤
│ id               │ BIGINT PK AUTO_INCREMENT          │
│ student_id       │ BIGINT FK → students              │
│ class_id         │ BIGINT FK → classes                │
│ academic_year    │ VARCHAR(9) NOT NULL (2026-2027)   │
│ created_at       │ TIMESTAMP DEFAULT CURRENT         │
├──────────────────┴──────────────────────────────────┤
│ UNIQUE (student_id, academic_year)                   │
│ INDEX idx_sc_class_year (class_id, academic_year)    │
└─────────────────────────────────────────────────────┘
```

### `classes`

```text
┌─────────────────────────────────────────────────────┐
│  classes                                             │
├──────────────────┬──────────────────────────────────┤
│ id               │ BIGINT PK AUTO_INCREMENT          │
│ name             │ VARCHAR(20) NOT NULL (VD: 12A)   │
│ grade_level      │ TINYINT NOT NULL (1-12)           │
│ academic_year    │ VARCHAR(9) NOT NULL (2026-2027)   │
│ school_name      │ VARCHAR(200) DEFAULT 'FPT Schools'│
│ created_at       │ TIMESTAMP DEFAULT CURRENT         │
│ updated_at       │ TIMESTAMP ON UPDATE CURRENT       │
├──────────────────┴──────────────────────────────────┤
│ UNIQUE (name, academic_year)                         │
│ INDEX idx_classes_year (academic_year)               │
│ INDEX idx_classes_grade (grade_level, academic_year)  │
└─────────────────────────────────────────────────────┘
```

### `subjects`

```text
┌─────────────────────────────────────────────────────┐
│  subjects                                            │
├──────────────────┬──────────────────────────────────┤
│ id               │ BIGINT PK AUTO_INCREMENT          │
│ name             │ VARCHAR(100) NOT NULL             │
│ code             │ VARCHAR(20) UNIQUE NOT NULL       │
│   (VD: TOAN12)   │                                   │
│ created_at       │ TIMESTAMP DEFAULT CURRENT         │
└─────────────────────────────────────────────────────┘
```

### `semesters`

```text
┌─────────────────────────────────────────────────────┐
│  semesters                                           │
├──────────────────┬──────────────────────────────────┤
│ id               │ BIGINT PK AUTO_INCREMENT          │
│ name             │ VARCHAR(50) NOT NULL              │
│   (VD: HK II)    │                                   │
│ academic_year    │ VARCHAR(9) NOT NULL (2026-2027)   │
│ start_date       │ DATE NOT NULL                     │
│ end_date         │ DATE NOT NULL                     │
│ is_current       │ BOOLEAN DEFAULT FALSE             │
│ created_at       │ TIMESTAMP DEFAULT CURRENT         │
├──────────────────┴──────────────────────────────────┤
│ UNIQUE (name, academic_year)                         │
│ INDEX idx_sem_year (academic_year)                   │
└─────────────────────────────────────────────────────┘
```

### `class_subjects`

```text
┌─────────────────────────────────────────────────────┐
│  class_subjects                                      │
├──────────────────┬──────────────────────────────────┤
│ id               │ BIGINT PK AUTO_INCREMENT          │
│ class_id         │ BIGINT FK → classes                │
│ subject_id       │ BIGINT FK → subjects              │
│ teacher_id       │ BIGINT FK → teachers              │
│ is_homeroom      │ BOOLEAN DEFAULT FALSE             │
│ academic_year    │ VARCHAR(9) NOT NULL               │
│ semester_id      │ BIGINT FK → semesters NULL        │
│ created_at       │ TIMESTAMP DEFAULT CURRENT         │
├──────────────────┴──────────────────────────────────┤
│ UNIQUE (class_id, subject_id, academic_year)         │
│ INDEX idx_cs_teacher (teacher_id) — GV tìm lớp      │
│ INDEX idx_cs_class   (class_id)   — lớp tìm GV      │
└─────────────────────────────────────────────────────┘
```

### `schedules`

```text
┌─────────────────────────────────────────────────────┐
│  schedules                                           │
├──────────────────┬──────────────────────────────────┤
│ id               │ BIGINT PK AUTO_INCREMENT          │
│ class_id         │ BIGINT FK → classes                │
│ subject_id       │ BIGINT FK → subjects              │
│ teacher_id       │ BIGINT FK → teachers              │
│ semester_id      │ BIGINT FK → semesters              │
│ day_of_week      │ TINYINT NOT NULL (1=CN..7=T7)     │
│ period           │ TINYINT NOT NULL (tiết 1-10)      │
│ room             │ VARCHAR(20) NULL                  │
│ shift            │ ENUM: MORNING, AFTERNOON          │
│ created_at       │ TIMESTAMP DEFAULT CURRENT         │
├──────────────────┴──────────────────────────────────┤
│ UNIQUE (class_id, semester_id, day_of_week, period)  │
│ INDEX idx_sch_teacher (teacher_id, semester_id)      │
│ INDEX idx_sch_sem     (semester_id, day_of_week)     │
└─────────────────────────────────────────────────────┘
```

### `grades`

```text
┌─────────────────────────────────────────────────────┐
│  grades (1 dòng = 1 HS × 1 môn × 1 HK)             │
├──────────────────┬──────────────────────────────────┤
│ id               │ BIGINT PK AUTO_INCREMENT          │
│ student_id       │ BIGINT FK → students              │
│ subject_id       │ BIGINT FK → subjects              │
│ semester_id      │ BIGINT FK → semesters              │
│ oral             │ DECIMAL(3,2) NULL                 │
│ quiz_15m         │ DECIMAL(3,2) NULL                 │
│ mid_term         │ DECIMAL(3,2) NULL                 │
│ final            │ DECIMAL(3,2) NULL                 │
│ average          │ DECIMAL(3,2) NULL (computed)      │
│ created_at       │ TIMESTAMP DEFAULT CURRENT         │
│ updated_at       │ TIMESTAMP ON UPDATE CURRENT       │
├──────────────────┴──────────────────────────────────┤
│ UNIQUE (student_id, subject_id, semester_id)         │
│ INDEX idx_grades_student_sem                          │
│   (student_id, semester_id) — PH/HS xem điểm HK     │
│ INDEX idx_grades_subject_sem                          │
│   (subject_id, semester_id) — GV xem điểm môn       │
└─────────────────────────────────────────────────────┘
```

### `semester_results`

```text
┌─────────────────────────────────────────────────────┐
│  semester_results (1 dòng = 1 HS × 1 HK)            │
├──────────────────┬──────────────────────────────────┤
│ id               │ BIGINT PK AUTO_INCREMENT          │
│ student_id       │ BIGINT FK → students              │
│ semester_id      │ BIGINT FK → semesters              │
│ class_id         │ BIGINT FK → classes                │
│ gpa              │ DECIMAL(3,2) NULL                 │
│ rank             │ INT NULL                          │
│ honor            │ VARCHAR(50) NULL                  │
│ conduct          │ VARCHAR(50) NULL                  │
│ academic_ability │ VARCHAR(50) NULL                  │
│ created_at       │ TIMESTAMP DEFAULT CURRENT         │
│ updated_at       │ TIMESTAMP ON UPDATE CURRENT       │
├──────────────────┴──────────────────────────────────┤
│ UNIQUE (student_id, semester_id)                     │
│ INDEX idx_sr_class_sem                               │
│   (class_id, semester_id) — xếp hạng trong lớp      │
│ INDEX idx_sr_sem                                     │
│   (semester_id) — thống kê tổng quan HK             │
└─────────────────────────────────────────────────────┘
```

### `attendance`

```text
┌─────────────────────────────────────────────────────┐
│  attendance                                          │
├──────────────────┬──────────────────────────────────┤
│ id               │ BIGINT PK AUTO_INCREMENT          │
│ student_id       │ BIGINT FK → students              │
│ class_id         │ BIGINT FK → classes                │
│ teacher_id       │ BIGINT FK → teachers (người điểm) │
│ schedule_id      │ BIGINT FK → schedules NULL        │
│ leave_request_id │ BIGINT FK → leave_requests NULL   │
│ date             │ DATE NOT NULL                     │
│ shift            │ ENUM: MORNING, AFTERNOON          │
│ status           │ ENUM: PRESENT,                    │
│                  │   ABSENT_WITH_LEAVE,              │
│                  │   ABSENT_WITHOUT_LEAVE            │
│ created_at       │ TIMESTAMP DEFAULT CURRENT         │
│ updated_at       │ TIMESTAMP ON UPDATE CURRENT       │
├──────────────────┴──────────────────────────────────┤
│ UNIQUE (student_id, date, shift)                     │
│ INDEX idx_att_class_date                             │
│   (class_id, date) — điểm danh theo lớp/ngày       │
│ INDEX idx_att_student_date                           │
│   (student_id, date) — PH/HS xem CC theo ngày      │
│ INDEX idx_att_student_sem                            │
│   (student_id) — thống kê tỷ lệ CC                  │
└─────────────────────────────────────────────────────┘
```

### `leave_requests`

```text
┌─────────────────────────────────────────────────────┐
│  leave_requests                                      │
├──────────────────┬──────────────────────────────────┤
│ id               │ BIGINT PK AUTO_INCREMENT          │
│ student_id       │ BIGINT FK → students              │
│ parent_id        │ BIGINT FK → parents               │
│ class_id         │ BIGINT FK → classes                │
│ approved_by      │ BIGINT FK → teachers NULL         │
│ date_from        │ DATE NOT NULL                     │
│ date_to          │ DATE NOT NULL                     │
│ shift            │ ENUM: FULL_DAY, MORNING, AFTERNOON│
│ reason           │ TEXT NOT NULL                     │
│ status           │ ENUM: PENDING, APPROVED, REJECTED │
│ response         │ TEXT NULL (phản hồi GV)           │
│ approved_at      │ TIMESTAMP NULL                    │
│ created_at       │ TIMESTAMP DEFAULT CURRENT         │
│ updated_at       │ TIMESTAMP ON UPDATE CURRENT       │
├──────────────────┴──────────────────────────────────┤
│ INDEX idx_lr_student  (student_id, status)           │
│ INDEX idx_lr_parent   (parent_id)                    │
│ INDEX idx_lr_teacher  (approved_by, status) — GV duyệt│
│ INDEX idx_lr_class    (class_id, status)             │
└─────────────────────────────────────────────────────┘
```

### `tuition_bills`

```text
┌─────────────────────────────────────────────────────┐
│  tuition_bills                                       │
├──────────────────┬──────────────────────────────────┤
│ id               │ BIGINT PK AUTO_INCREMENT          │
│ student_id       │ BIGINT FK → students              │
│ class_id         │ BIGINT FK → classes                │
│ semester_id      │ BIGINT FK → semesters              │
│ name             │ VARCHAR(200) NOT NULL             │
│   (VD: Học phí HK2)│                                │
│ amount           │ DECIMAL(12,2) NOT NULL            │
│ due_date         │ DATE NOT NULL                     │
│ status           │ ENUM: UNPAID, PROCESSING, PAID    │
│ paid_at          │ TIMESTAMP NULL                    │
│ created_at       │ TIMESTAMP DEFAULT CURRENT         │
│ updated_at       │ TIMESTAMP ON UPDATE CURRENT       │
├──────────────────┴──────────────────────────────────┤
│ INDEX idx_tb_student   (student_id, status)          │
│ INDEX idx_tb_class_sem                                 │
│   (class_id, semester_id, status) — GV xem HP lớp   │
│ INDEX idx_tb_sem_status (semester_id, status)        │
└─────────────────────────────────────────────────────┘
```

### `payment_transactions`

```text
┌─────────────────────────────────────────────────────┐
│  payment_transactions                                │
├──────────────────┬──────────────────────────────────┤
│ id               │ BIGINT PK AUTO_INCREMENT          │
│ bill_id          │ BIGINT FK → tuition_bills         │
│ amount           │ DECIMAL(12,2) NOT NULL            │
│ payment_method   │ VARCHAR(50) (VNPAY, QR, CASH)    │
│ transaction_ref  │ VARCHAR(100) NULL                 │
│ status           │ ENUM: PENDING, SUCCESS, FAILED    │
│ paid_at          │ TIMESTAMP NULL                    │
│ created_at       │ TIMESTAMP DEFAULT CURRENT         │
├──────────────────┴──────────────────────────────────┤
│ INDEX idx_pt_bill (bill_id)                          │
└─────────────────────────────────────────────────────┘
```

### `announcements`

```text
┌─────────────────────────────────────────────────────┐
│  announcements                                       │
├──────────────────┬──────────────────────────────────┤
│ id               │ BIGINT PK AUTO_INCREMENT          │
│ teacher_id       │ BIGINT FK → teachers              │
│ title            │ VARCHAR(500) NOT NULL             │
│ body             │ TEXT NOT NULL                     │
│ target_role      │ ENUM: PARENT, STUDENT, ALL        │
│ approval_status  │ PENDING / APPROVED / REJECTED     │
│ recipient_scope  │ CLASSES / SCHOOL                  │
│ created_at       │ TIMESTAMP DEFAULT CURRENT         │
├──────────────────┴──────────────────────────────────┤
│ INDEX idx_ann_teacher (teacher_id) — GV xem đã gửi │
│ INDEX idx_ann_created (created_at) — sort mới nhất  │
└─────────────────────────────────────────────────────┘
```

### `announcement_classes`

```text
┌─────────────────────────────────────────────────────┐
│  announcement_classes                                │
├──────────────────┬──────────────────────────────────┤
│ id               │ BIGINT PK AUTO_INCREMENT          │
│ announcement_id  │ BIGINT FK → announcements         │
│ class_id         │ BIGINT FK → classes                │
├──────────────────┴──────────────────────────────────┤
│ UNIQUE (announcement_id, class_id)                   │
│ INDEX idx_ac_class (class_id) — lớp xem thông báo   │
└─────────────────────────────────────────────────────┘
```

### `announcement_reads`

```text
┌─────────────────────────────────────────────────────┐
│  announcement_reads                                  │
├──────────────────┬──────────────────────────────────┤
│ id               │ BIGINT PK AUTO_INCREMENT          │
│ announcement_id  │ BIGINT FK → announcements         │
│ user_id          │ BIGINT FK → users                  │
│ read_at          │ TIMESTAMP NULL                    │
│ created_at       │ TIMESTAMP DEFAULT CURRENT         │
├──────────────────┴──────────────────────────────────┤
│ UNIQUE (announcement_id, user_id)                    │
│ INDEX idx_ar_user (user_id, read_at) — badge đỏ     │
└─────────────────────────────────────────────────────┘
```

### `conversations`

```text
┌─────────────────────────────────────────────────────┐
│  conversations                                       │
├──────────────────┬──────────────────────────────────┤
│ id               │ BIGINT PK AUTO_INCREMENT          │
│ last_message     │ TEXT NULL (denormalized)          │
│ last_message_at  │ TIMESTAMP NULL (denormalized)     │
│ created_at       │ TIMESTAMP DEFAULT CURRENT         │
│ updated_at       │ TIMESTAMP ON UPDATE CURRENT       │
└─────────────────────────────────────────────────────┘
```

### `conversation_participants`

```text
┌─────────────────────────────────────────────────────┐
│  conversation_participants                           │
├──────────────────┬──────────────────────────────────┤
│ id               │ BIGINT PK AUTO_INCREMENT          │
│ conversation_id  │ BIGINT FK → conversations         │
│ user_id          │ BIGINT FK → users                  │
│ joined_at        │ TIMESTAMP DEFAULT CURRENT         │
│ last_read_at     │ TIMESTAMP NULL (→ badge unread)   │
├──────────────────┴──────────────────────────────────┤
│ UNIQUE (conversation_id, user_id)                    │
│ INDEX idx_cp_user (user_id) — inbox của user        │
└─────────────────────────────────────────────────────┘
```

### `messages`

```text
┌─────────────────────────────────────────────────────┐
│  messages                                            │
├──────────────────┬──────────────────────────────────┤
│ id               │ BIGINT PK AUTO_INCREMENT          │
│ conversation_id  │ BIGINT FK → conversations         │
│ sender_id        │ BIGINT FK → users                  │
│ content          │ TEXT NOT NULL                     │
│ created_at       │ TIMESTAMP DEFAULT CURRENT         │
├──────────────────┴──────────────────────────────────┤
│ INDEX idx_msg_conv                                  │
│   (conversation_id, created_at) — load tin nhắn     │
└─────────────────────────────────────────────────────┘

⚠️ LƯU Ý: messages grows fast. Khi > 10M rows,
xem xét partition by RANGE (YEAR(created_at)) hoặc
archive messages > 1 năm sang messages_archive.
```

### `attachments`

```text
┌─────────────────────────────────────────────────────┐
│  attachments                                         │
├──────────────────┬──────────────────────────────────┤
│ id               │ BIGINT PK AUTO_INCREMENT          │
│ leave_request_id │ BIGINT FK → leave_requests NULL   │
│ message_id       │ BIGINT FK → messages NULL         │
│ file_url         │ VARCHAR(500) NOT NULL             │
│ file_name        │ VARCHAR(255) NOT NULL             │
│ file_size        │ INT NOT NULL (bytes)              │
│ mime_type        │ VARCHAR(100) NOT NULL             │
│ created_at       │ TIMESTAMP DEFAULT CURRENT         │
├──────────────────┴──────────────────────────────────┤
│ INDEX idx_att_lr   (leave_request_id)               │
│ INDEX idx_att_msg  (message_id)                     │
└─────────────────────────────────────────────────────┘
```

### `notifications`

```text
┌─────────────────────────────────────────────────────┐
│  notifications                                       │
├──────────────────┬──────────────────────────────────┤
│ id               │ BIGINT PK AUTO_INCREMENT          │
│ user_id          │ BIGINT FK → users                  │
│ tuition_bill_id  │ BIGINT FK → tuition_bills NULL    │
│ title            │ VARCHAR(500) NOT NULL             │
│ body             │ TEXT NULL                         │
│ tag              │ VARCHAR(50) (Học phí, CLB, ...)   │
│ is_read          │ BOOLEAN DEFAULT FALSE             │
│ created_at       │ TIMESTAMP DEFAULT CURRENT         │
├──────────────────┴──────────────────────────────────┤
│ INDEX idx_noti_user  (user_id, is_read) — TB chưa đọc│
│ INDEX idx_noti_tag   (tag)                           │
│ INDEX idx_noti_created (created_at) — sort           │
└─────────────────────────────────────────────────────┘
```

### `club_registrations`

```text
┌─────────────────────────────────────────────────────┐
│  club_registrations                                  │
├──────────────────┬──────────────────────────────────┤
│ id               │ BIGINT PK AUTO_INCREMENT          │
│ student_id       │ BIGINT FK → students              │
│ club_name        │ VARCHAR(200) NOT NULL             │
│ academic_year    │ VARCHAR(9) NOT NULL               │
│ status           │ ENUM: REGISTERED, CANCELLED       │
│ registered_at    │ TIMESTAMP DEFAULT CURRENT         │
├──────────────────┴──────────────────────────────────┤
│ UNIQUE (student_id, club_name, academic_year)        │
│ INDEX idx_cr_student (student_id)                    │
└─────────────────────────────────────────────────────┘
```

---

## Tối ưu hiệu năng & Scale

### 1. Indexing Strategy

```text
┌─────────────────────────────────────────────────────────────────┐
│                    INDEXING STRATEGY                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  BẢNG QUAN TRỌNG NHẤT (query nhiều nhất):                      │
│                                                                 │
│  grades                                                         │
│  ├── idx_grades_student_sem (student_id, semester_id)          │
│  │   → PH/HS xem điểm: WHERE student_id=? AND semester_id=?  │
│  └── idx_grades_subject_sem (subject_id, semester_id)          │
│      → GV xem điểm môn: WHERE subject_id=? AND semester_id=? │
│                                                                 │
│  attendance                                                     │
│  ├── idx_att_class_date (class_id, date)                       │
│  │   → GV điểm danh: WHERE class_id=? AND date=?              │
│  └── idx_att_student_date (student_id, date)                   │
│      → PH/HS xem CC: WHERE student_id=? ORDER BY date DESC   │
│                                                                 │
│  messages                                                       │
│  └── idx_msg_conv (conversation_id, created_at)                │
│      → Load tin nhắn: WHERE conversation_id=? ORDER BY date   │
│                                                                 │
│  notifications                                                  │
│  └── idx_noti_user (user_id, is_read)                          │
│      → Badge chưa đọc: WHERE user_id=? AND is_read=FALSE      │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 2. Partitioning Strategy (khi scale)

```text
┌─────────────────────────────────────────────────────────────────┐
│                  PARTITIONING STRATEGY                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  BẢNG NÊN PARTITION:                                            │
│                                                                 │
│  1. messages            → RANGE BY YEAR(created_at)             │
│     Lý do: grow nhanh, query luôn theo thời gian gần           │
│     Archive: messages > 1 năm → messages_archive                │
│                                                                 │
│  2. attendance          → RANGE BY YEAR(date)                   │
│     Lý do: 1 HS 2 dòng/ngày × 42 HS × 200 ngày = 16,800/năm │
│                                                                 │
│  3. payment_transactions → RANGE BY YEAR(created_at)            │
│     Lý do: audit trail, query theo thời gian                   │
│                                                                 │
│  BẢNG KHÔNG CẦN PARTITION:                                      │
│  grades, semester_results, schedules — grow chậm                │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 3. Denormalization Strategy

```text
┌─────────────────────────────────────────────────────────────────┐
│               DENORMALIZATION (TỐI ƯU QUERY)                    │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  conversations.last_message / last_message_at                   │
│  → Không cần JOIN messages để hiển thị preview trong inbox     │
│  → UPDATE khi có tin nhắn mới                                   │
│                                                                 │
│  grades.average                                                 │
│  → Compute từ oral, quiz_15m, mid_term, final                  │
│  → Lưu sẵn để tránh tính lại mỗi lần query                     │
│                                                                 │
│  semester_results (tách khỏi grades)                            │
│  → Summary table riêng, không cần aggregate grades mỗi lần    │
│  → Teacher nhập điểm → trigger UPDATE semester_results         │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 4. Soft Delete Strategy

```text
┌─────────────────────────────────────────────────────────────────┐
│                   SOFT DELETE                                   │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  KHÔNG dùng hard delete cho:                                    │
│  ├── grades             → academic audit trail                  │
│  ├── attendance         → lịch sử điểm danh                    │
│  ├── leave_requests     → lịch sử đơn                          │
│  ├── payment_transactions → audit tài chính                    │
│  ├── announcements      → có thể cần xem lại                   │
│                                                                 │
│  Thêm field deleted_at TIMESTAMP NULL cho các bảng trên.       │
│  Query mặc định: WHERE deleted_at IS NULL                       │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 5. Caching Strategy

```text
┌─────────────────────────────────────────────────────────────────┐
│                    CACHING STRATEGY                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  REDIS CACHE cho:                                               │
│  ├── schedules          → thay đổi ít, query nhiều (TKB)       │
│  ├── subjects           → gần như tĩnh                          │
│  ├── classes            → gần như tĩnh theo năm                 │
│  ├── semesters          → tĩnh                                  │
│  └── user profile       → thay đổi ít                          │
│                                                                 │
│  INVALIDATE khi:                                                │
│  ├── Teacher sửa TKB → xóa cache schedules[class_id]           │
│  ├── Admin thêm lớp → xóa cache classes                        │
│  └── Bắt đầu HK mới → xóa cache semesters                     │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 6. Scaling Roadmap

```text
┌─────────────────────────────────────────────────────────────────┐
│                    SCALING ROADMAP                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  PHASE 1 (Current — 1 trường, < 1000 users):                   │
│  ├── MySQL single instance                                     │
│  ├── Redis cache cho reference data                            │
│  └── OK với schema hiện tại                                    │
│                                                                 │
│  PHASE 2 (5-10 trường, 10K users):                              │
│  ├── Read Replica cho reports/thống kê                         │
│  ├── Partition messages + attendance                           │
│  ├── CDN cho attachments (images, files)                       │
│  └── Message Queue cho notifications                           │
│                                                                 │
│  PHASE 3 (Multi-school SaaS, 100K+ users):                     │
│  ├── Sharding by school_id (thêm school_id vào mọi bảng)       │
│  ├── Microservice: tuition, grades, messaging tách riêng       │
│  ├── Elasticsearch cho search (tin nhắn, thông báo)            │
│  └── TimescaleDB cho attendance (time-series)                  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 7. Schema Future-Proofing

```text
┌─────────────────────────────────────────────────────────────────┐
│               FUTURE-PROOF FIELDS                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Mỗi bảng có:                                                   │
│  ├── id              BIGINT PK (cho phép scale shard)          │
│  ├── created_at      TIMESTAMP DEFAULT CURRENT                 │
│  └── updated_at      TIMESTAMP ON UPDATE CURRENT               │
│                                                                 │
│  ENUMs thay vì VARCHAR cho status/type:                         │
│  ├── users.role      ENUM (không insert sai data)              │
│  ├── grades (điểm)   DECIMAL(3,2) (không insert text)          │
│  └── attendance      ENUM (không insert sai status)            │
│                                                                 │
│  academic_year VARCHAR(9) thống nhất:                            │
│  ├── classes.academic_year                                      │
│  ├── semesters.academic_year                                    │
│  ├── student_classes.academic_year                              │
│  ├── class_subjects.academic_year                               │
│  └── club_registrations.academic_year                           │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## Thay đổi so với v4

| # | Thay đổi | Lý do |
|---|----------|-------|
| 1 | ➕ `classes.academic_year` | Query lớp theo năm học |
| 2 | ➕ `semesters.academic_year` | HK II - 2026-2027 cần năm học |
| 3 | ➕ Index cho mọi bảng lớn | Tối ưu query performance |
| 4 | ➕ Partitioning strategy | messages, attendance scale > 10M rows |
| 5 | ➕ Denormalization plan | conversations.last_message, grades.average |
| 6 | ➕ Soft delete strategy | Audit trail cho grades, attendance, payments |
| 7 | ➕ Caching strategy | Redis cho schedules, subjects, classes |
| 8 | ➕ Scaling roadmap | Phase 1→2→3 từ 1 trường đến SaaS |
| 9 | ➕ `is_current` trong semesters | Xác định HK đang diễn ra |
| 10 | ➕ `shift` trong schedules + attendance | Phân biệt buổi sáng/chiều rõ ràng |

---

*Phiên bản: 5.0 (Final) — Ngày cập nhật: 2026-06-24*
*Dựa trên: screen-flow.md + review v2/v3/v4 + optimization + scalability planning*
