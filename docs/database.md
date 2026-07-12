# MyFschool — Database Design (MySQL)

Tài liệu thiết kế cơ sở dữ liệu chi tiết cho ứng dụng Sổ liên lạc điện tử.
Engine: **InnoDB** | Charset: **utf8mb4** | Collation: **utf8mb4_unicode_ci**

---

## Mục lục

1. [Tổng quan](#1-tổng-quan)
2. [ERD](#2-erd)
3. [Schema chi tiết](#3-schema-chi-tiết)
4. [Mối quan hệ & Foreign Keys](#4-mối-quan-h.ForeignKey)
5. [Indexing Strategy](#5-indexing-strategy)
6. [Sample Data](#6-sample-data)
7. [Stored Procedures](#7-stored-procedures)
8. [Optimization & Scaling](#8-optimization--scaling)

---

## 1. Tổng quan

**Tổng cộng: 27 bảng**

```
┌─────────────────────────────────────────────────────────┐
│                    TỔNG QUAN DATABASE                    │
├─────────────────────────────────────────────────────────┤
│  Database:   myfschool                                   │
│  Engine:     InnoDB (transaction + FK support)          │
│  Charset:    utf8mb4 (hỗ trợ emoji + tiếng Việt)       │
│  Collation:  utf8mb4_unicode_ci                         │
│  Tables:     26                                          │
│  Foreign Keys: 47                                        │
│  Indexes:    ~75                                         │
└─────────────────────────────────────────────────────────┘
```

| Nhóm | Số bảng | Bảng |
|------|---------|------|
| **Tài khoản** | 1 | `users` |
| **Actor** | 3 | `parents`, `students`, `teachers` |
| **Liên kết** | 4 | `student_guardians`, `student_classes`, `class_subjects`, `announcement_classes` |
| **Giáo dục** | 4 | `classes`, `subjects`, `semesters`, `schedules` |
| **Học tập** | 4 | `grades`, `semester_results`, `attendance`, `leave_requests` |
| **Học phí** | 2 | `tuition_bills`, `payment_transactions` |
| **Giao tiếp** | 5 | `conversations`, `conversation_participants`, `messages`, `announcements`, `announcement_reads` |
| **Hệ thống** | 3 | `notifications`, `club_registrations`, `attachments` |

---

## 2. ERD

Xem chi tiết ERD tại: [erd.md](./erd.md)

---

## 3. Schema chi tiết

### 3.1. Tạo Database

```sql
CREATE DATABASE IF NOT EXISTS myfschool
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE myfschool;
```

---

### 3.2. Nhóm Tài khoản

#### `users`

```sql
CREATE TABLE users (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  phone       VARCHAR(15)     NOT NULL,
  password    VARCHAR(255)    NOT NULL COMMENT 'bcrypt hash',
  name        VARCHAR(100)    NOT NULL,
  email       VARCHAR(255)    NULL,
  avatar      VARCHAR(500)    NULL,
  role        ENUM('PARENT','STUDENT','TEACHER') NOT NULL,
  status      ENUM('ACTIVE','INACTIVE','LOCKED') NOT NULL DEFAULT 'ACTIVE',
  created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  UNIQUE KEY uk_users_phone (phone),
  INDEX idx_users_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Tài khoản người dùng (chung cho 3 role)';
```

### 3.3. Nhóm Actor

#### `parents`

```sql
CREATE TABLE parents (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id     BIGINT UNSIGNED NOT NULL,
  address     VARCHAR(500)    NULL,
  occupation  VARCHAR(200)    NULL,
  created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  UNIQUE KEY uk_parents_user (user_id),
  CONSTRAINT fk_parents_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Thông tin phụ huynh';
```

#### `students`

```sql
CREATE TABLE students (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id       BIGINT UNSIGNED NOT NULL,
  student_code  VARCHAR(20)     NOT NULL COMMENT 'VD: 12A-01',
  class_id      BIGINT UNSIGNED NULL COMMENT 'Lớp hiện tại',
  date_of_birth DATE            NULL,
  created_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  UNIQUE KEY uk_students_user (user_id),
  UNIQUE KEY uk_students_code (student_code),
  INDEX idx_students_class (class_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Thông tin học sinh';
```

#### `teachers`

```sql
CREATE TABLE teachers (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id       BIGINT UNSIGNED NOT NULL,
  employee_code VARCHAR(20)     NOT NULL COMMENT 'Tự sinh, VD: GV-0001',
  created_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  UNIQUE KEY uk_teachers_user (user_id),
  UNIQUE KEY uk_teachers_code (employee_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Thông tin giáo viên';
```

---

### 3.4. Nhóm Liên kết

#### `student_guardians`

```sql
CREATE TABLE student_guardians (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  student_id    BIGINT UNSIGNED NOT NULL,
  guardian_id   BIGINT UNSIGNED NOT NULL,
  relationship  ENUM('FATHER','MOTHER','GUARDIAN') NOT NULL,
  created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  UNIQUE KEY uk_sg_pair (student_id, guardian_id),
  INDEX idx_sg_guardian (guardian_id),
  CONSTRAINT fk_sg_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_sg_guardian FOREIGN KEY (guardian_id) REFERENCES parents(id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Liên kết học sinh - người giám hộ (cha/mẹ/người giám hộ)';
```

#### `student_classes`

```sql
CREATE TABLE student_classes (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  student_id    BIGINT UNSIGNED NOT NULL,
  class_id      BIGINT UNSIGNED NOT NULL,
  academic_year VARCHAR(9) NOT NULL COMMENT 'VD: 2026-2027',
  created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  UNIQUE KEY uk_sc_student_year (student_id, academic_year),
  INDEX idx_sc_class_year (class_id, academic_year),
  CONSTRAINT fk_sc_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_sc_class FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Lịch sử lớp của học sinh theo từng năm học';
```

#### `class_subjects`

```sql
CREATE TABLE class_subjects (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  class_id      BIGINT UNSIGNED NOT NULL,
  subject_id    BIGINT UNSIGNED NOT NULL,
  teacher_id    BIGINT UNSIGNED NOT NULL,
  is_homeroom   TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'GVCN = 1, BM = 0',
  academic_year VARCHAR(9) NOT NULL COMMENT 'VD: 2026-2027',
  semester_id   BIGINT UNSIGNED NULL,
  created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  UNIQUE KEY uk_cs_class_subject_year (class_id, subject_id, academic_year),
  INDEX idx_cs_teacher (teacher_id),
  INDEX idx_cs_class (class_id),
  CONSTRAINT fk_cs_class FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_cs_subject FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_cs_teacher FOREIGN KEY (teacher_id) REFERENCES teachers(id) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_cs_semester FOREIGN KEY (semester_id) REFERENCES semesters(id) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Phân công giáo viên dạy môn - lớp';
```

#### `announcement_classes`

```sql
CREATE TABLE announcement_classes (
  id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  announcement_id  BIGINT UNSIGNED NOT NULL,
  class_id         BIGINT UNSIGNED NOT NULL,

  PRIMARY KEY (id),
  UNIQUE KEY uk_ac_ann_class (announcement_id, class_id),
  INDEX idx_ac_class (class_id),
  CONSTRAINT fk_ac_announcement FOREIGN KEY (announcement_id) REFERENCES announcements(id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_ac_class FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Thông báo gửi đến nhiều lớp (M:N)';
```

---

### 3.5. Nhóm Giáo dục

#### `classes`

```sql
CREATE TABLE classes (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  name          VARCHAR(20)     NOT NULL COMMENT 'VD: 12A, SE1913',
  grade_level   TINYINT UNSIGNED NOT NULL COMMENT '1-12',
  academic_year VARCHAR(9) NOT NULL COMMENT 'VD: 2026-2027',
  school_name   VARCHAR(200) NOT NULL DEFAULT 'FPT Schools',
  created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  UNIQUE KEY uk_classes_name_year (name, academic_year),
  INDEX idx_classes_year (academic_year),
  INDEX idx_classes_grade (grade_level, academic_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Lớp học';
```

#### `subjects`

```sql
CREATE TABLE subjects (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  name        VARCHAR(100)    NOT NULL COMMENT 'VD: Toán, Tiếng Anh',
  code        VARCHAR(20)     NOT NULL COMMENT 'VD: TOAN12, ENG12',
  created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  UNIQUE KEY uk_subjects_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Môn học';
```

#### `semesters`

```sql
CREATE TABLE semesters (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  name          VARCHAR(50) NOT NULL COMMENT 'VD: HK I, HK II',
  academic_year VARCHAR(9) NOT NULL COMMENT 'VD: 2026-2027',
  start_date    DATE NOT NULL,
  end_date      DATE NOT NULL,
  is_current    TINYINT(1) NOT NULL DEFAULT 0,
  created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  UNIQUE KEY uk_sem_name_year (name, academic_year),
  INDEX idx_sem_year (academic_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Học kỳ';
```

#### `schedules`

```sql
CREATE TABLE schedules (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  class_id    BIGINT UNSIGNED NOT NULL,
  subject_id  BIGINT UNSIGNED NOT NULL,
  teacher_id  BIGINT UNSIGNED NOT NULL,
  semester_id BIGINT UNSIGNED NOT NULL,
  day_of_week TINYINT UNSIGNED NOT NULL COMMENT '1=CN, 2=T2, ..., 7=T7',
  period      TINYINT UNSIGNED NOT NULL COMMENT 'Tiết 1-10',
  room        VARCHAR(20) NULL,
  shift       ENUM('MORNING','AFTERNOON') NOT NULL DEFAULT 'MORNING',
  created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  UNIQUE KEY uk_sch_class_sem_day_period (class_id, semester_id, day_of_week, period),
  INDEX idx_sch_teacher (teacher_id, semester_id),
  INDEX idx_sch_sem (semester_id, day_of_week),
  CONSTRAINT fk_sch_class FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_sch_subject FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_sch_teacher FOREIGN KEY (teacher_id) REFERENCES teachers(id) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_sch_semester FOREIGN KEY (semester_id) REFERENCES semesters(id) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Thời khóa biểu';
```

---

### 3.6. Nhóm Học tập

#### `grades`

```sql
CREATE TABLE grades (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  student_id  BIGINT UNSIGNED NOT NULL,
  subject_id  BIGINT UNSIGNED NOT NULL,
  semester_id BIGINT UNSIGNED NOT NULL,
  oral        DECIMAL(3,2) NULL COMMENT 'Điểm miệng',
  quiz_15m    DECIMAL(3,2) NULL COMMENT 'Điểm 15 phút',
  mid_term    DECIMAL(3,2) NULL COMMENT 'Điểm 1 tiết',
  final       DECIMAL(3,2) NULL COMMENT 'Điểm học kỳ',
  average     DECIMAL(3,2) NULL COMMENT 'TBM môn (computed)',
  created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  UNIQUE KEY uk_grades_student_subject_sem (student_id, subject_id, semester_id),
  INDEX idx_grades_student_sem (student_id, semester_id),
  INDEX idx_grades_subject_sem (subject_id, semester_id),
  CONSTRAINT fk_grades_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_grades_subject FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_grades_semester FOREIGN KEY (semester_id) REFERENCES semesters(id) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Điểm chi tiết theo môn (1 dòng = 1 HS × 1 môn × 1 HK)';
```

#### `semester_results`

```sql
CREATE TABLE semester_results (
  id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  student_id        BIGINT UNSIGNED NOT NULL,
  semester_id       BIGINT UNSIGNED NOT NULL,
  class_id          BIGINT UNSIGNED NOT NULL,
  gpa               DECIMAL(3,2) NULL COMMENT 'Điểm trung bình học kỳ',
  `rank`            INT UNSIGNED NULL COMMENT 'Xếp hạng trong lớp',
  honor             VARCHAR(50) NULL COMMENT 'Danh hiệu: Giỏi, Khá, TB',
  conduct           VARCHAR(50) NULL COMMENT 'Hạnh kiểm: Tốt, Khá, TB, Yếu',
  academic_ability  VARCHAR(50) NULL COMMENT 'Học lực: Giỏi, Khá, TB, Yếu',
  created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  UNIQUE KEY uk_sr_student_sem (student_id, semester_id),
  INDEX idx_sr_class_sem (class_id, semester_id),
  INDEX idx_sr_sem (semester_id),
  CONSTRAINT fk_sr_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_sr_semester FOREIGN KEY (semester_id) REFERENCES semesters(id) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_sr_class FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Tổng kết học kỳ (1 dòng = 1 HS × 1 HK)';
```

#### `attendance`

```sql
CREATE TABLE attendance (
  id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  student_id       BIGINT UNSIGNED NOT NULL,
  class_id         BIGINT UNSIGNED NOT NULL,
  teacher_id       BIGINT UNSIGNED NOT NULL COMMENT 'Người điểm danh',
  schedule_id      BIGINT UNSIGNED NULL COMMENT 'Link tiết học (optional)',
  leave_request_id BIGINT UNSIGNED NULL COMMENT 'Link đơn nghỉ (optional)',
  date             DATE NOT NULL,
  shift            ENUM('MORNING','AFTERNOON') NOT NULL,
  status           ENUM('PRESENT','ABSENT_WITH_LEAVE','ABSENT_WITHOUT_LEAVE') NOT NULL,
  created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  UNIQUE KEY uk_att_student_date_shift (student_id, date, shift),
  INDEX idx_att_class_date (class_id, date),
  INDEX idx_att_student_date (student_id, date),
  INDEX idx_att_student (student_id),
  CONSTRAINT fk_att_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_att_class FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_att_teacher FOREIGN KEY (teacher_id) REFERENCES teachers(id) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_att_schedule FOREIGN KEY (schedule_id) REFERENCES schedules(id) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT fk_att_leave FOREIGN KEY (leave_request_id) REFERENCES leave_requests(id) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Chuyên cần (điểm danh)';
```

Mỗi bản ghi tương ứng một học sinh trong một **buổi học**. Chỉ tạo điểm danh khi
lớp có `schedule` trong thời khóa biểu hiệu lực của ngày và buổi tương ứng.
Sau lần lưu đầu tiên, thay đổi của giáo viên được lưu vào
`attendance_correction_requests` ở trạng thái `PENDING`; bảng `attendance` chỉ
được cập nhật sau khi Admin duyệt.

#### `leave_requests`

```sql
CREATE TABLE leave_requests (
  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  student_id   BIGINT UNSIGNED NOT NULL,
  parent_id    BIGINT UNSIGNED NOT NULL COMMENT 'Người tạo đơn',
  class_id     BIGINT UNSIGNED NOT NULL,
  approved_by  BIGINT UNSIGNED NULL COMMENT 'GV duyệt đơn',
  date_from    DATE NOT NULL,
  date_to      DATE NOT NULL,
  shift        ENUM('FULL_DAY','MORNING','AFTERNOON') NOT NULL DEFAULT 'FULL_DAY',
  reason       TEXT NOT NULL,
  status       ENUM('PENDING','APPROVED','REJECTED') NOT NULL DEFAULT 'PENDING',
  response     TEXT NULL COMMENT 'Phản hồi từ GV',
  approved_at  TIMESTAMP NULL,
  created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  INDEX idx_lr_student (student_id, status),
  INDEX idx_lr_parent (parent_id),
  INDEX idx_lr_teacher (approved_by, status),
  INDEX idx_lr_class (class_id, status),
  CONSTRAINT fk_lr_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_lr_parent FOREIGN KEY (parent_id) REFERENCES parents(id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_lr_class FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_lr_teacher FOREIGN KEY (approved_by) REFERENCES teachers(id) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Đơn xin nghỉ học';
```

---

### 3.7. Nhóm Học phí

#### `tuition_bills`

```sql
CREATE TABLE tuition_bills (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  student_id  BIGINT UNSIGNED NOT NULL,
  class_id    BIGINT UNSIGNED NOT NULL,
  semester_id BIGINT UNSIGNED NOT NULL,
  name        VARCHAR(200) NOT NULL COMMENT 'VD: Học phí HK2, Phí cơ sở vật chất',
  amount      DECIMAL(12,2) NOT NULL,
  due_date    DATE NOT NULL,
  status      ENUM('UNPAID','PROCESSING','PAID') NOT NULL DEFAULT 'UNPAID',
  paid_at     TIMESTAMP NULL,
  created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  INDEX idx_tb_student (student_id, status),
  INDEX idx_tb_class_sem (class_id, semester_id, status),
  INDEX idx_tb_sem_status (semester_id, status),
  CONSTRAINT fk_tb_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_tb_class FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_tb_semester FOREIGN KEY (semester_id) REFERENCES semesters(id) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Khoản học phí';
```

#### `payment_transactions`

```sql
CREATE TABLE payment_transactions (
  id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  bill_id          BIGINT UNSIGNED NOT NULL,
  amount           DECIMAL(12,2) NOT NULL,
  payment_method   VARCHAR(50) NULL COMMENT 'VNPAY, QR, CASH',
  transaction_ref  VARCHAR(100) NULL,
  status           ENUM('PENDING','SUCCESS','FAILED') NOT NULL DEFAULT 'PENDING',
  paid_at          TIMESTAMP NULL,
  created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  INDEX idx_pt_bill (bill_id),
  CONSTRAINT fk_pt_bill FOREIGN KEY (bill_id) REFERENCES tuition_bills(id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Giao dịch thanh toán học phí';
```

---

### 3.8. Nhóm Giao tiếp

#### `announcements`

```sql
CREATE TABLE announcements (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  teacher_id     BIGINT UNSIGNED NOT NULL COMMENT 'Người tạo thông báo',
  title          VARCHAR(500) NOT NULL,
  body           TEXT NOT NULL,
  target_role    ENUM('PARENT','STUDENT','ALL') NOT NULL DEFAULT 'ALL',
  requires_reply TINYINT(1) NOT NULL DEFAULT 0,
  created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  INDEX idx_ann_teacher (teacher_id),
  INDEX idx_ann_created (created_at),
  CONSTRAINT fk_ann_teacher FOREIGN KEY (teacher_id) REFERENCES teachers(id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Thông báo (GV tạo → gửi nhiều lớp)';
```

#### `announcement_reads`

```sql
CREATE TABLE announcement_reads (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  announcement_id BIGINT UNSIGNED NOT NULL,
  user_id         BIGINT UNSIGNED NOT NULL,
  read_at         TIMESTAMP NULL,
  created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  UNIQUE KEY uk_ar_ann_user (announcement_id, user_id),
  INDEX idx_ar_user (user_id, read_at),
  CONSTRAINT fk_ar_announcement FOREIGN KEY (announcement_id) REFERENCES announcements(id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_ar_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Trạng thái đọc thông báo';
```

#### `conversations`

```sql
CREATE TABLE conversations (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  last_message  TEXT NULL COMMENT 'Denormalized: tin nhắn cuối cùng',
  last_message_at TIMESTAMP NULL COMMENT 'Thời gian tin nhắn cuối',
  created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Hộp thoại tin nhắn (Messenger-style)';
```

#### `conversation_participants`

```sql
CREATE TABLE conversation_participants (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  conversation_id BIGINT UNSIGNED NOT NULL,
  user_id         BIGINT UNSIGNED NOT NULL,
  joined_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_read_at    TIMESTAMP NULL COMMENT 'Đọc tin nhắn cuối (→ badge unread)',

  PRIMARY KEY (id),
  UNIQUE KEY uk_cp_conv_user (conversation_id, user_id),
  INDEX idx_cp_user (user_id),
  CONSTRAINT fk_cp_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_cp_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Thành viên tham gia hộp thoại';
```

#### `messages`

```sql
CREATE TABLE messages (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  conversation_id BIGINT UNSIGNED NOT NULL,
  sender_id       BIGINT UNSIGNED NOT NULL,
  content         TEXT NOT NULL,
  created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  INDEX idx_msg_conv (conversation_id, created_at),
  CONSTRAINT fk_msg_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_msg_sender FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Tin nhắn trong hộp thoại';
```

---

### 3.9. Nhóm Hệ thống

#### `notifications`

```sql
CREATE TABLE notifications (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id         BIGINT UNSIGNED NOT NULL,
  tuition_bill_id BIGINT UNSIGNED NULL COMMENT 'TB liên quan HP (optional)',
  title           VARCHAR(500) NOT NULL,
  body            TEXT NULL,
  tag             VARCHAR(50) NULL COMMENT 'Học phí, CLB, Hệ thống...',
  is_read         TINYINT(1) NOT NULL DEFAULT 0,
  created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  INDEX idx_noti_user (user_id, is_read),
  INDEX idx_noti_tag (tag),
  INDEX idx_noti_created (created_at),
  CONSTRAINT fk_noti_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_noti_bill FOREIGN KEY (tuition_bill_id) REFERENCES tuition_bills(id) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Thông báo hệ thống';
```

#### `club_registrations`

```sql
CREATE TABLE club_registrations (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  student_id    BIGINT UNSIGNED NOT NULL,
  club_name     VARCHAR(200) NOT NULL,
  academic_year VARCHAR(9) NOT NULL COMMENT 'VD: 2026-2027',
  status        ENUM('REGISTERED','CANCELLED') NOT NULL DEFAULT 'REGISTERED',
  registered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  UNIQUE KEY uk_cr_student_club_year (student_id, club_name, academic_year),
  INDEX idx_cr_student (student_id),
  CONSTRAINT fk_cr_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Đăng ký câu lạc bộ';
```

#### `attachments`

```sql
CREATE TABLE attachments (
  id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  leave_request_id  BIGINT UNSIGNED NULL,
  message_id        BIGINT UNSIGNED NULL,
  file_url          VARCHAR(500) NOT NULL,
  file_name         VARCHAR(255) NOT NULL,
  file_size         INT UNSIGNED NOT NULL COMMENT 'Bytes',
  mime_type         VARCHAR(100) NOT NULL,
  created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  INDEX idx_att_lr (leave_request_id),
  INDEX idx_att_msg (message_id),
  CONSTRAINT fk_att_lr FOREIGN KEY (leave_request_id) REFERENCES leave_requests(id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_att_msg FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='File đính kèm (đơn xin nghỉ, tin nhắn)';
```

---

## 4. Mối quan hệ & Foreign Keys

### 4.1. Danh sách 48 Foreign Keys

| # | Table | Column | FK → Table | On Delete | On Update |
|---|-------|--------|------------|-----------|-----------|
| 2 | `parents` | `user_id` | `users.id` | CASCADE | CASCADE |
| 3 | `students` | `user_id` | `users.id` | CASCADE | CASCADE |
| 4 | `students` | `class_id` | `classes.id` | RESTRICT | CASCADE |
| 5 | `teachers` | `user_id` | `users.id` | CASCADE | CASCADE |
| 6 | `student_guardians` | `student_id` | `students.id` | CASCADE | CASCADE |
| 7 | `student_guardians` | `guardian_id` | `parents.id` | CASCADE | CASCADE |
| 8 | `student_classes` | `student_id` | `students.id` | CASCADE | CASCADE |
| 9 | `student_classes` | `class_id` | `classes.id` | RESTRICT | CASCADE |
| 10 | `class_subjects` | `class_id` | `classes.id` | RESTRICT | CASCADE |
| 11 | `class_subjects` | `subject_id` | `subjects.id` | RESTRICT | CASCADE |
| 12 | `class_subjects` | `teacher_id` | `teachers.id` | RESTRICT | CASCADE |
| 13 | `class_subjects` | `semester_id` | `semesters.id` | SET NULL | CASCADE |
| 14 | `schedules` | `class_id` | `classes.id` | RESTRICT | CASCADE |
| 15 | `schedules` | `subject_id` | `subjects.id` | RESTRICT | CASCADE |
| 16 | `schedules` | `teacher_id` | `teachers.id` | RESTRICT | CASCADE |
| 17 | `schedules` | `semester_id` | `semesters.id` | RESTRICT | CASCADE |
| 18 | `grades` | `student_id` | `students.id` | CASCADE | CASCADE |
| 19 | `grades` | `subject_id` | `subjects.id` | RESTRICT | CASCADE |
| 20 | `grades` | `semester_id` | `semesters.id` | RESTRICT | CASCADE |
| 21 | `semester_results` | `student_id` | `students.id` | CASCADE | CASCADE |
| 22 | `semester_results` | `semester_id` | `semesters.id` | RESTRICT | CASCADE |
| 23 | `semester_results` | `class_id` | `classes.id` | RESTRICT | CASCADE |
| 24 | `attendance` | `student_id` | `students.id` | CASCADE | CASCADE |
| 25 | `attendance` | `class_id` | `classes.id` | RESTRICT | CASCADE |
| 26 | `attendance` | `teacher_id` | `teachers.id` | RESTRICT | CASCADE |
| 27 | `attendance` | `schedule_id` | `schedules.id` | SET NULL | CASCADE |
| 28 | `attendance` | `leave_request_id` | `leave_requests.id` | SET NULL | CASCADE |
| 29 | `leave_requests` | `student_id` | `students.id` | CASCADE | CASCADE |
| 30 | `leave_requests` | `parent_id` | `parents.id` | CASCADE | CASCADE |
| 31 | `leave_requests` | `class_id` | `classes.id` | RESTRICT | CASCADE |
| 32 | `leave_requests` | `approved_by` | `teachers.id` | SET NULL | CASCADE |
| 33 | `tuition_bills` | `student_id` | `students.id` | CASCADE | CASCADE |
| 34 | `tuition_bills` | `class_id` | `classes.id` | RESTRICT | CASCADE |
| 35 | `tuition_bills` | `semester_id` | `semesters.id` | RESTRICT | CASCADE |
| 36 | `payment_transactions` | `bill_id` | `tuition_bills.id` | CASCADE | CASCADE |
| 37 | `announcements` | `teacher_id` | `teachers.id` | CASCADE | CASCADE |
| 38 | `announcement_classes` | `announcement_id` | `announcements.id` | CASCADE | CASCADE |
| 39 | `announcement_classes` | `class_id` | `classes.id` | CASCADE | CASCADE |
| 40 | `announcement_reads` | `announcement_id` | `announcements.id` | CASCADE | CASCADE |
| 41 | `announcement_reads` | `user_id` | `users.id` | CASCADE | CASCADE |
| 42 | `conversation_participants` | `conversation_id` | `conversations.id` | CASCADE | CASCADE |
| 43 | `conversation_participants` | `user_id` | `users.id` | CASCADE | CASCADE |
| 44 | `messages` | `conversation_id` | `conversations.id` | CASCADE | CASCADE |
| 45 | `messages` | `sender_id` | `users.id` | CASCADE | CASCADE |
| 46 | `notifications` | `user_id` | `users.id` | CASCADE | CASCADE |
| 47 | `notifications` | `tuition_bill_id` | `tuition_bills.id` | SET NULL | CASCADE |
| 48 | `club_registrations` | `student_id` | `students.id` | CASCADE | CASCADE |
| 49 | `attachments` | `leave_request_id` | `leave_requests.id` | CASCADE | CASCADE |
| 50 | `attachments` | `message_id` | `messages.id` | CASCADE | CASCADE |

### 4.2. ON DELETE Strategy

```
┌───────────────────────────────────────────────────────────┐
│                   ON DELETE STRATEGY                       │
├───────────────────────────────────────────────────────────┤
│                                                           │
│  CASCADE — Xóa kèm dữ liệu con:                         │
│  ├── user → parents, students, teachers                  │
│  ├── student → grades, attendance, leave_requests, etc.  │
│  ├── announcement → announcement_classes, reads          │
│  └── conversation → participants, messages               │
│                                                           │
│  RESTRICT — Không cho xóa nếu có dữ liệu con:           │
│  ├── class → students, class_subjects, schedules         │
│  ├── subject → grades, schedules, class_subjects         │
│  └── semester → grades, schedules, tuition_bills         │
│                                                           │
│  SET NULL — Xóa cha, giữ con với FK = NULL:             │
│  ├── schedule → attendance.schedule_id                   │
│  ├── leave_request → attendance.leave_request_id         │
│  ├── teacher → leave_requests.approved_by                │
│  └── tuition_bill → notifications.tuition_bill_id        │
│                                                           │
└───────────────────────────────────────────────────────────┘
```

---

## 5. Indexing Strategy

### 5.1. Danh sách Index

```sql
-- ========================================
-- USERS
-- ========================================
-- idx_users_phone     → Login lookup (WHERE phone = ?)
-- idx_users_role      → Filter by role (WHERE role = 'PARENT')
-- uk_users_phone      → Unique constraint

-- ========================================
-- STUDENTS
-- ========================================
-- idx_students_class  → Query lớp (WHERE class_id = ?)
-- idx_students_code   → Lookup mã HS (WHERE student_code = ?)

-- ========================================
-- GRADES
-- ========================================
-- idx_grades_student_sem  → PH/HS xem điểm HK
--   (student_id, semester_id)
-- idx_grades_subject_sem  → GV xem điểm môn
--   (subject_id, semester_id)

-- ========================================
-- ATTENDANCE
-- ========================================
-- idx_att_class_date      → Điểm danh theo lớp/ngày
--   (class_id, date)
-- idx_att_student_date    → PH/HS xem CC theo ngày
--   (student_id, date)

-- ========================================
-- MESSAGES
-- ========================================
-- idx_msg_conv            → Load tin nhắn
--   (conversation_id, created_at)

-- ========================================
-- NOTIFICATIONS
-- ========================================
-- idx_noti_user           → Badge chưa đọc
--   (user_id, is_read)

-- ========================================
-- LEAVE_REQUESTS
-- ========================================
-- idx_lr_student          → HS xem đơn
--   (student_id, status)
-- idx_lr_teacher          → GV duyệt đơn
--   (approved_by, status)
```

### 5.2. Query Hot Paths

| # | Query | Index được dùng | Screen |
|---|-------|-----------------|--------|
| 1 | `SELECT * FROM grades WHERE student_id=? AND semester_id=?` | `idx_grades_student_sem` | Bảng điểm PH/HS |
| 2 | `SELECT * FROM grades WHERE subject_id=? AND semester_id=?` | `idx_grades_subject_sem` | Nhập điểm GV |
| 3 | `SELECT * FROM attendance WHERE class_id=? AND date=?` | `idx_att_class_date` | Điểm danh GV |
| 4 | `SELECT * FROM attendance WHERE student_id=? ORDER BY date DESC` | `idx_att_student_date` | Chuyên cần PH/HS |
| 5 | `SELECT * FROM messages WHERE conversation_id=? ORDER BY created_at` | `idx_msg_conv` | Tin nhắn |
| 6 | `SELECT * FROM notifications WHERE user_id=? AND is_read=0` | `idx_noti_user` | Badge thông báo |
| 7 | `SELECT * FROM leave_requests WHERE approved_by=? AND status='PENDING'` | `idx_lr_teacher` | Duyệt đơn GV |
| 8 | `SELECT * FROM tuition_bills WHERE class_id=? AND semester_id=? AND status=?` | `idx_tb_class_sem` | QL HP GV |

---

## 6. Sample Data

### 6.1. Users

```sql
INSERT INTO users (phone, password, name, email, role, status) VALUES
-- Parents
('0901234001', '$2b$10$abc123...', 'Nguyễn Văn An', 'an@email.com', 'PARENT', 'ACTIVE'),
('0901234002', '$2b$10$abc123...', 'Trần Thị Bình', 'binh@email.com', 'PARENT', 'ACTIVE'),
('0901234003', '$2b$10$abc123...', 'Lê Hoàng Dũng', 'dung@email.com', 'PARENT', 'ACTIVE'),

-- Students
('0901234101', '$2b$10$abc123...', 'Nguyễn Minh An', 'anhsv@email.com', 'STUDENT', 'ACTIVE'),
('0901234102', '$2b$10$abc123...', 'Nguyễn Minh Bảo', 'baosv@email.com', 'STUDENT', 'ACTIVE'),
('0901234103', '$2b$10$abc123...', 'Trần Hoàng Nam', 'namsv@email.com', 'STUDENT', 'ACTIVE'),

-- Teachers
('0901234201', '$2b$10$abc123...', 'Nguyễn Thu Hà', 'hagv@email.com', 'TEACHER', 'ACTIVE'),
('0901234202', '$2b$10$abc123...', 'Phạm Văn Khánh', 'khanhgv@email.com', 'TEACHER', 'ACTIVE');
```

### 6.2. Classes

```sql
INSERT INTO classes (name, grade_level, academic_year, school_name) VALUES
('12A', 12, '2026-2027', 'FPT Schools'),
('11B', 11, '2026-2027', 'FPT Schools'),
('SE1913', 12, '2026-2027', 'FPT Schools');
```

### 6.3. Semesters

```sql
INSERT INTO semesters (name, academic_year, start_date, end_date, is_current) VALUES
('HK I',  '2026-2027', '2026-09-01', '2027-01-15', 0),
('HK II', '2026-2027', '2027-01-16', '2027-05-30', 1);
```

### 6.4. Subjects

```sql
INSERT INTO subjects (name, code) VALUES
('Toán',         'TOAN12'),
('Ngữ văn',      'VAN12'),
('Tiếng Anh',    'ENG12'),
('Vật lý',       'LY12'),
('Hóa học',      'HOA12'),
('Tin học',      ' tin12'),
('PRM393 - SE',  'PRM393'),
('Kỹ năng dự án','KYNANG');
```

### 6.5. Students

```sql
INSERT INTO students (user_id, student_code, class_id, date_of_birth) VALUES
(4, '12A-01', 1, '2008-03-15'),
(5, '12A-02', 1, '2008-07-22'),
(6, 'SE-01',  3, '2008-01-10');
```

### 6.6. Teachers

```sql
INSERT INTO teachers (user_id, employee_code) VALUES
(7, 'GV-0007'),
(8, 'GV-0008');
```

### 6.7. Student Guardians

```sql
INSERT INTO student_guardians (student_id, guardian_id, relationship) VALUES
-- Nguyễn Minh An: Bố = user#1, Mẹ = user#2
(1, 1, 'FATHER'),
(1, 2, 'MOTHER'),
-- Nguyễn Minh Bảo: Bố = user#1, Mẹ = user#2 (cùng gia đình)
(2, 1, 'FATHER'),
(2, 2, 'MOTHER'),
-- Trần Hoàng Nam: Bố = user#3
(3, 3, 'FATHER');
```

### 6.8. Class Subjects

```sql
INSERT INTO class_subjects (class_id, subject_id, teacher_id, is_homeroom, academic_year, semester_id) VALUES
-- Lớp 12A
(1, 1, 1, 0, '2026-2027', NULL),  -- Toán - Cô Hà
(1, 2, 2, 1, '2026-2027', NULL),  -- Văn - Thầy Khánh (GVCN)
(1, 3, 1, 0, '2026-2027', NULL),  -- Anh - Cô Hà
-- SE1913
(3, 7, 1, 1, '2026-2027', NULL);  -- PRM393 - Cô Hà (GVCN)
```

### 6.9. Grades

```sql
INSERT INTO grades (student_id, subject_id, semester_id, oral, quiz_15m, mid_term, final, average) VALUES
-- Nguyễn Minh An - HK II - Toán
(1, 1, 2, 8.50, 9.00, 7.50, 8.00, 8.25),
-- Nguyễn Minh An - HK II - Văn
(1, 2, 2, 7.00, 8.00, 7.00, 7.50, 7.38),
-- Nguyễn Minh An - HK II - Anh
(1, 3, 2, 9.00, 8.50, 8.00, 9.00, 8.63);
```

### 6.10. Semester Results

```sql
INSERT INTO semester_results (student_id, semester_id, class_id, gpa, `rank`, honor, conduct, academic_ability) VALUES
(1, 2, 1, 8.25, 5, 'Khá', 'Tốt', 'Khá'),
(2, 2, 1, 7.80, 8, 'Khá', 'Khá', 'Khá'),
(3, 2, 3, 8.90, 1, 'Giỏi', 'Tốt', 'Giỏi');
```

### 6.11. Attendance

```sql
INSERT INTO attendance (student_id, class_id, teacher_id, date, shift, status) VALUES
(1, 1, 1, '2026-06-20', 'MORNING', 'PRESENT'),
(1, 1, 1, '2026-06-20', 'AFTERNOON', 'PRESENT'),
(2, 1, 1, '2026-06-20', 'MORNING', 'PRESENT'),
(2, 1, 1, '2026-06-20', 'AFTERNOON', 'PRESENT'),
(3, 3, 1, '2026-06-20', 'MORNING', 'ABSENT_WITH_LEAVE'),
(3, 3, 1, '2026-06-20', 'AFTERNOON', 'ABSENT_WITH_LEAVE');
```

### 6.12. Leave Requests

```sql
INSERT INTO leave_requests (student_id, parent_id, class_id, date_from, date_to, shift, reason, status, approved_by, approved_at) VALUES
(3, 3, 3, '2026-06-20', '2026-06-20', 'FULL_DAY', 'Em bị ốm, cần nghỉ học', 'APPROVED', 1, '2026-06-20 08:00:00');
```

### 6.13. Tuition Bills

```sql
INSERT INTO tuition_bills (student_id, class_id, semester_id, name, amount, due_date, status) VALUES
(1, 1, 2, 'Học phí HK II', 15000000.00, '2027-02-28', 'PAID'),
(2, 1, 2, 'Học phí HK II', 15000000.00, '2027-02-28', 'UNPAID'),
(3, 3, 2, 'Học phí HK II', 18000000.00, '2027-02-28', 'UNPAID');
```

### 6.14. Announcements

```sql
INSERT INTO announcements (teacher_id, title, body, target_role, requires_reply) VALUES
(1, 'Lịch thi cuối kỳ II', 'Thông báo lịch thi cuối kỳ học kỳ II năm học 2026-2027', 'ALL', 0),
(2, 'Họp phụ huynh cuối kỳ', 'Thời gian: 15/05/2027. Địa điểm: Hội trường.', 'PARENT', 1);
```

---

## 7. Stored Procedures

### 7.1. Tính TBM môn (grades.average)

```sql
DELIMITER //
CREATE PROCEDURE sp_calculate_subject_average(IN p_student_id INT, IN p_semester_id INT)
BEGIN
  UPDATE grades
  SET average = ROUND(
    (COALESCE(oral, 0) +
     COALESCE(quiz_15m, 0) * 2 +
     COALESCE(mid_term, 0) * 3 +
     COALESCE(final, 0) * 4) / 10,
    2
  )
  WHERE student_id = p_student_id AND semester_id = p_semester_id;
END //
DELIMITER ;
```

### 7.2. Tính GPA học kỳ (semester_results)

```sql
DELIMITER //
CREATE PROCEDURE sp_calculate_semester_gpa(IN p_student_id INT, IN p_semester_id INT)
BEGIN
  DECLARE v_gpa DECIMAL(3,2);

  SELECT ROUND(AVG(average), 2) INTO v_gpa
  FROM grades
  WHERE student_id = p_student_id AND semester_id = p_semester_id AND average IS NOT NULL;

  UPDATE semester_results
  SET gpa = v_gpa,
      academic_ability = CASE
        WHEN v_gpa >= 8.0 THEN 'Giỏi'
        WHEN v_gpa >= 6.5 THEN 'Khá'
        WHEN v_gpa >= 5.0 THEN 'Trung bình'
        ELSE 'Yếu'
      END
  WHERE student_id = p_student_id AND semester_id = p_semester_id;
END //
DELIMITER ;
```

### 7.3. Cập nhật attendance khi duyệt đơn Approved

```sql
DELIMITER //
CREATE PROCEDURE sp_approve_leave_request(IN p_leave_id INT, IN p_teacher_id INT)
BEGIN
  DECLARE v_student_id INT;
  DECLARE v_date_from DATE;
  DECLARE v_date_to DATE;
  DECLARE v_shift ENUM('FULL_DAY','MORNING','AFTERNOON');

  -- Lấy thông tin đơn
  SELECT student_id, date_from, date_to, shift
  INTO v_student_id, v_date_from, v_date_to, v_shift
  FROM leave_requests WHERE id = p_leave_id;

  -- Cập nhật đơn
  UPDATE leave_requests
  SET status = 'APPROVED', approved_by = p_teacher_id, approved_at = NOW()
  WHERE id = p_leave_id;

  -- Tự động cập nhật attendance
  IF v_shift = 'FULL_DAY' THEN
    INSERT INTO attendance (student_id, class_id, teacher_id, date, shift, status, leave_request_id)
    SELECT v_student_id, lr.class_id, p_teacher_id, d.dt, 'MORNING', 'ABSENT_WITH_LEAVE', p_leave_id
    FROM leave_requests lr,
    (SELECT DATE_ADD(v_date_from, INTERVAL n DAY) AS dt
     FROM (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
           UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) numbers
     WHERE DATE_ADD(v_date_from, INTERVAL n DAY) <= v_date_to) d
    WHERE lr.id = p_leave_id
    ON DUPLICATE KEY UPDATE status = 'ABSENT_WITH_LEAVE', leave_request_id = p_leave_id;

    INSERT INTO attendance (student_id, class_id, teacher_id, date, shift, status, leave_request_id)
    SELECT v_student_id, lr.class_id, p_teacher_id, d.dt, 'AFTERNOON', 'ABSENT_WITH_LEAVE', p_leave_id
    FROM leave_requests lr,
    (SELECT DATE_ADD(v_date_from, INTERVAL n DAY) AS dt
     FROM (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
           UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) numbers
     WHERE DATE_ADD(v_date_from, INTERVAL n DAY) <= v_date_to) d
    WHERE lr.id = p_leave_id
    ON DUPLICATE KEY UPDATE status = 'ABSENT_WITH_LEAVE', leave_request_id = p_leave_id;
  ELSE
    INSERT INTO attendance (student_id, class_id, teacher_id, date, shift, status, leave_request_id)
    SELECT v_student_id, lr.class_id, p_teacher_id, d.dt, v_shift, 'ABSENT_WITH_LEAVE', p_leave_id
    FROM leave_requests lr,
    (SELECT DATE_ADD(v_date_from, INTERVAL n DAY) AS dt
     FROM (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
           UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) numbers
     WHERE DATE_ADD(v_date_from, INTERVAL n DAY) <= v_date_to) d
    WHERE lr.id = p_leave_id
    ON DUPLICATE KEY UPDATE status = 'ABSENT_WITH_LEAVE', leave_request_id = p_leave_id;
  END IF;
END //
DELIMITER ;
```

---

## 8. Optimization & Scaling

### 8.1. Partitioning (khi scale > 10M rows)

```sql
-- Partition messages theo năm
ALTER TABLE messages PARTITION BY RANGE (YEAR(created_at)) (
  PARTITION p2026 VALUES LESS THAN (2027),
  PARTITION p2027 VALUES LESS THAN (2028),
  PARTITION p2028 VALUES LESS THAN (2029),
  PARTITION pmax  VALUES LESS THAN MAXVALUE
);

-- Partition attendance theo năm
ALTER TABLE attendance PARTITION BY RANGE (YEAR(date)) (
  PARTITION p2026 VALUES LESS THAN (2027),
  PARTITION p2027 VALUES LESS THAN (2028),
  PARTITION p2028 VALUES LESS THAN (2029),
  PARTITION pmax  VALUES LESS THAN MAXVALUE
);
```

### 8.2. Soft Delete (audit trail)

```sql
-- Thêm cột deleted_at cho các bảng quan trọng
ALTER TABLE grades ADD COLUMN deleted_at TIMESTAMP NULL DEFAULT NULL AFTER updated_at;
ALTER TABLE attendance ADD COLUMN deleted_at TIMESTAMP NULL DEFAULT NULL AFTER updated_at;
ALTER TABLE leave_requests ADD COLUMN deleted_at TIMESTAMP NULL DEFAULT NULL AFTER updated_at;
ALTER TABLE payment_transactions ADD COLUMN deleted_at TIMESTAMP NULL DEFAULT NULL AFTER created_at;
ALTER TABLE announcements ADD COLUMN deleted_at TIMESTAMP NULL DEFAULT NULL AFTER created_at;

-- Index cho soft delete
CREATE INDEX idx_grades_deleted ON grades(deleted_at);
CREATE INDEX idx_att_deleted ON attendance(deleted_at);
CREATE INDEX idx_lr_deleted ON leave_requests(deleted_at);
CREATE INDEX idx_pt_deleted ON payment_transactions(deleted_at);
CREATE INDEX idx_ann_deleted ON announcements(deleted_at);

-- Query mặc định: WHERE deleted_at IS NULL
```

### 8.3. Denormalization

```sql
-- conversations: cập nhật last_message khi có tin nhắn mới
DELIMITER //
CREATE TRIGGER trg_messages_after_insert
AFTER INSERT ON messages
FOR EACH ROW
BEGIN
  UPDATE conversations
  SET last_message = NEW.content,
      last_message_at = NEW.created_at
  WHERE id = NEW.conversation_id;
END //
DELIMITER ;

-- grades: tự động tính average khi insert/update điểm
DELIMITER //
CREATE TRIGGER trg_grades_before_update
BEFORE UPDATE ON grades
FOR EACH ROW
BEGIN
  SET NEW.average = ROUND(
    (COALESCE(NEW.oral, 0) +
     COALESCE(NEW.quiz_15m, 0) * 2 +
     COALESCE(NEW.mid_term, 0) * 3 +
     COALESCE(NEW.final, 0) * 4) / 10,
    2
  );
END //
DELIMITER ;
```

### 8.4. Caching (Redis)

```
┌───────────────────────────────────────────────────────────┐
│                    REDIS CACHE LAYERS                      │
├───────────────────────────────────────────────────────────┤
│                                                           │
│  STATIC DATA (TTL: 24h):                                  │
│  ├── subjects:*           → Danh sách môn học            │
│  ├── classes:*            → Danh sách lớp                 │
│  └── semesters:*          → Danh sách học kỳ             │
│                                                           │
│  SEMI-STATIC (TTL: 1h):                                   │
│  ├── schedules:class:{id} → TKB theo lớp                 │
│  ├── schedules:teacher:{id} → TKB theo GV                │
│  └── user:{id}:profile    → Thông tin user                │
│                                                           │
│  REALTIME (TTL: 30s):                                     │
│  ├── noti:unread:{user_id} → Số TB chưa đọc             │
│  ├── msg:unread:{user_id}  → Số tin chưa đọc            │
│  └── tuitions:unpaid:{class_id} → Số HS chưa đóng HP    │
│                                                           │
└───────────────────────────────────────────────────────────┘
```

---

*Phiên bản: 1.0 — Ngày tạo: 2026-06-24*
*Dựa trên: erd.md v5 (Final)*
