# MyFschool — Screen Flow Chi Tiết

Tài liệu mô tả toàn bộ luồng màn hình (Screen Flow) từ Login đến mọi screen cho 3 actor:
**Phụ huynh**, **Học sinh**, **Giáo viên**. Mỗi flow bao gồm user action, screen tiếp theo,
API call, và state change.

---

## Mục lục

1. [Global Flow (Chung)](#1-global-flow)
2. [HomeParent Flow](#2-homeparent-flow)
3. [HomeStudent Flow](#3-homestudent-flow)
4. [HomeTeacher Flow](#4-hometeacher-flow)
5. [Bottom Navigation Tabs](#5-bottom-navigation-tabs)

---

## 1. Global Flow

Chung cho tất cả 3 actor: Login → Role Selection → AppShell.

### 1.1. Login Screen (SCR-COM-01)

```text
[SCR-COM-01: LOGIN SCREEN]
  │
  ├── Input: Số điện thoại + Mật khẩu
  ├── API: POST /api/auth/login → JWT token + roles[]
  ├── State: Save JWT to SecureStorage
  │
  ├── (Nếu login thất bại) → Hiện lỗi inline, giữ nguyên form
  │
  └── (Login thành công) ──────────────────►
                                               │
[SCR-COM-02: ROLE SELECTION SCREEN]           │
  │                                            │
  ├── Hiển thị danh sách roles từ JWT          │
  │   (Ví dụ: [Phụ huynh, Học sinh, Giáo viên])│
  │                                            │
  ├── User chọn role ──────────────────────────┤
  │                                            │
  │   ├── Chọn "Phụ huynh" → AppShell(actor: PARENT)
  │   ├── Chọn "Học sinh"  → AppShell(actor: STUDENT)
  │   └── Chọn "Giáo viên" → AppShell(actor: TEACHER)
  │
  └── API: GET /api/user/profile (tải thông tin user + roles)
```

### 1.2. AppShell — 4 Tabs

```text
[APP SHELL — IndexedStack + Nested Navigator — 4 tabs]
  │
  ├── Tab 0: 🏠 Trang chủ ─────► Home [actor-specific]
  │     ├── Parent → HomeParent (Student Switcher + 5 buttons)
  │     ├── Student → HomeStudent (Profile card + 5 buttons)
  │     └── Teacher → HomeTeacher (Profile card + 8 buttons)
  │
  ├── Tab 1: 💬 Tin nhắn ──────► ConversationsScreen(actor)
  │     ├── Parent/Student → Thread list (PH ↔ GV)
  │     └── Teacher → TeacherInboxScreen (Unified inbox)
  │
  ├── Tab 2: 📢 Thông báo ─────► AnnouncementsScreen(actor)
  │     ├── Parent → Thông báo lớp + TB Học phí
  │     ├── Student → Thông báo cá nhân
  │     └── Teacher → Thông báo đã gửi + Tracking
  │
  └── Tab 3: 👤 Tài khoản ─────► AccountProfileScreen(actor)
        ├── Hồ sơ cá nhân
        ├── Đổi mật khẩu
        └── Đăng xuất → pushAndRemoveUntil → LoginScreen
```

---

## 2. HomeParent Flow

```text
[APP SHELL — Tab 0: Trang chủ]
  │
  ▼
[SCR-PH-01: HOME PARENT (HomeParent)]
  │
  ├── [COMPONENT: SharedHeader]
  │
  ├── [COMPONENT: Student Switcher — Horizontal ListView]
  │     ├── Chọn con 1: "Nguyễn Minh An" → setState(_selectedStudentIndex = 0)
  │     │     └── Cập nhật toàn bộ data trên Home (bills, notifs, attendance)
  │     └── Chọn con 2: "Nguyễn Minh Bảo" → setState(_selectedStudentIndex = 1)
  │           └── Cập nhật toàn bộ data trên Home
  │
  └── [COMPONENT: GridView 2 columns — 5 Feature Buttons]
        │
        ├──► [BUTTON 1: "Thời khóa biểu"]
        │         │
        │         ▼
        │    [SCR-PH-06: SCHEDULE SCREEN]
        │      ├── AppBar: "Thời khóa biểu lớp"
        │      ├── Month Selector (← Tháng 06/2026 →)
        │      ├── Week Day Selector (T2-T7, highlighted selected)
        │      ├── Section "Buổi sáng": LessonCards (Tiết, Môn, GV, Phòng)
        │      └── Section "Buổi chiều": LessonCards
        │
        ├──► [BUTTON 2: "Bảng điểm"]
        │         │
        │         ▼
        │    [SCR-PH-04: GRADES SCREEN]
        │      ├── AppBar: "Bảng kết quả học tập"
        │      ├── 🔀 Switch "Mô phỏng" (toggle simulation mode)
        │      │     ├── ON → Banner hướng dẫn + Dropdown Hạnh kiểm
        │      │     │     └── Click ô điểm → [BOTTOM SHEET: Edit Grade]
        │      │     │           ├── Numerical: Nhập miệng/15p/1 tiết/học kỳ
        │      │     │           └── Comment: Chọn Đ/KĐ từng cột
        │      │     └── OFF → Reset về gốc, ẩn banner
        │      ├── Semester Selector (← HK II - 2026-2027 →)
        │      ├── Summary Table (GPA, Xếp hạng, Danh hiệu, Hạnh kiểm, Học lực)
        │      ├── Detailed Grades Table (DataTable scroll ngang)
        │      │     └── Cột: Môn, Miệng, 15p, 1 tiết, HK, TBM
        │      └── "Nhận xét GVCN" card
        │
        ├──► [BUTTON 3: "Chuyên cần"]
        │         │
        │         ▼
        │    [SCR-PH-05: ATTENDANCE SCREEN]
        │      ├── AppBar: "Chuyên cần"
        │      ├── Stats Card (Tỷ lệ đi học % + Đơn chờ duyệt)
        │      └── "Nhật ký điểm danh" → AttendanceEventTiles
        │            ├── Ngày | Buổi sáng/chiều | StatusPill (Có mặt/Muộn/Vắng)
        │            └── Lý do / Liên kết đơn nghỉ
        │
        ├──► [BUTTON 4: "Đơn từ"]
        │         │
        │         ▼
        │    [SCR-PH-07: LEAVE REQUEST LIST]
        │      ├── AppBar: "Đơn xin nghỉ học"
        │      ├── [BUTTON: "Tạo đơn xin nghỉ"]
        │      │         │
        │      │         ▼ (push + await result)
        │      │    [SCR-PH-08: LEAVE REQUEST CREATE]
        │      │      ├── Form: Ngày/buổi (Dropdown Cả ngày/Sáng/Chiều)
        │      │      ├── Form: Lý do (TextField, required)
        │      │      ├── Checkbox: "Đính kèm minh chứng"
        │      │      └── [BUTTON: "Gửi đơn"]
        │      │            ├── Validate: lý do rỗng → SnackBar lỗi
        │      │            ├── Tạo LeaveRequest(status: 'Pending')
        │      │            ├── API: POST /api/leave-requests
        │      │            └── Pop(context, result: newRequest)
        │      │                 └── ListScreen: insert(0, newRequest) + SnackBar OK
        │      │
        │      └── "Lịch sử đơn xin nghỉ" → LeaveRequestTiles
        │            ├── Title + StatusPill (Pending/Approved/Rejected)
        │            ├── Ngày | Lý do
        │            └── Phản hồi từ GV (nếu có)
        │
        └──► [BUTTON 5: "TB Học phí"]
                  │
                  ▼
             [BOTTOM SHEET: Tuition Notifications]
               ├── Kiểm tra: unpaidSum = tổng bill 'Chưa đóng'
               │
               ├── (Nếu unpaidSum == 0)
               │     └── "Không có thông báo học phí nào mới."
               │
               └── (Nếu unpaidSum > 0)
                     ├── Danh sách tuitionNotifs (tag == 'Học phí')
                     │     └── Cards: Icon ⚠️ + Title + Body
                     └── [BUTTON: "Đóng học phí"]
                           │
                           ▼ (pop bottom sheet, push screen)
                     [SCR-PH-TUITION: TUITION PAYMENT SCREEN]
                       ├── AppBar: "Thanh toán học phí"
                       ├── Student info card
                       ├── Danh sách bills (Unpaid + Paid)
                       │     └── Mỗi bill: Tên khoản, Số tiền, StatusPill
                       ├── Tổng chưa đóng
                       ├── [BUTTON: "Thanh toán"] (nếu có unpaid)
                       │         │
                       │         ▼
                       │    [BOTTOM SHEET: QR Code]
                       │      ├── Mã VietQR (mock)
                       │      ├── Số tiền + Nội dung CK
                       │      └── [BUTTON: "Xác nhận chuyển khoản"]
                       │            ├── setState: bill.status → 'Đang xử lý'
                       │            ├── API: POST /api/tuition/pay
                       │            └── Ẩn thông báo học phí liên quan
                       │
                       └── "Lịch sử giao dịch" (Paid bills)
```

### Cross-actor triggers từ HomeParent:

```text
[HOME PARENT — Cross-actor triggers]
  │
  ├── Tạo đơn xin nghỉ → API tạo bill
  │     └── GV nhận notification trên [SCR-GV-04: TEACHER LEAVE REQUESTS]
  │
  ├── GV duyệt đơn Approved
  │     └── PH thấy status='Approved' trong [SCR-PH-07: LEAVE REQUEST LIST]
  │     └── Chuyên cần tự động cập nhật: 'Vắng có phép' + mã đơn
  │
  └── Gửi tin nhắn GV
        └── (qua Tab 1: Tin nhắn — không qua Home)
```

---

## 3. HomeStudent Flow

```text
[APP SHELL — Tab 0: Trang chủ]
  │
  ▼
[SCR-HS-01: HOME STUDENT (HomeStudent)]
  │
  ├── [COMPONENT: SharedHeader]
  │         (KHÔNG có icon 🔔 thông báo)
  │
  ├── [COMPONENT: Green Profile Card]
  │     ├── Icon 🎓 + Tên học sinh ("Nguyễn Minh An")
  │     └── "Lớp 12A • Mã HS: 12A-01 • FPT Schools"
  │
  └── [COMPONENT: GridView 2 columns — 5 Feature Buttons]
        │
        ├──► [BUTTON 1: "Thời khóa biểu"]
        │         │
        │         ▼
        │    [SCR-HS-SCHEDULE: SCHEDULE SCREEN]
        │      ├── AppBar: "Thời khóa biểu lớp"
        │      ├── Month Selector (← Tháng 06/2026 →)
        │      ├── Week Day Selector (T2-T7, highlighted selected)
        │      ├── Section "Buổi sáng": LessonCards
        │      └── Section "Buổi chiều": LessonCards
        │
        ├──► [BUTTON 2: "Bảng điểm"]
        │         │
        │         ▼
        │    [SCR-HS-GRADES: GRADES SCREEN]
        │      ├── AppBar: "Bảng kết quả học tập"
        │      ├── 🔀 Switch "Mô phỏng" (toggle simulation mode)
        │      │     ├── ON → Banner hướng dẫn + Dropdown Hạnh kiểm
        │      │     │     └── Click ô điểm → [BOTTOM SHEET: Edit Grade]
        │      │     │           ├── Numerical: Nhập miệng/15p/1 tiết/học kỳ
        │      │     │           └── Comment: Chọn Đ/KĐ từng cột
        │      │     └── OFF → Reset về gốc, ẩn banner
        │      ├── Semester Selector (← HK II - 2026-2027 →)
        │      ├── Summary Table (GPA, Xếp hạng, Danh hiệu, Hạnh kiểm, Học lực)
        │      ├── Detailed Grades Table (DataTable scroll ngang)
        │      └── "Nhận xét GVCN" card
        │
        ├──► [BUTTON 3: "Chuyên cần"]
        │         │
        │         ▼
        │    [SCR-HS-ATTENDANCE: ATTENDANCE SCREEN]
        │      ├── AppBar: "Chuyên cần"
        │      ├── Stats Card (Tỷ lệ đi học % + Đơn chờ duyệt)
        │      └── "Nhật ký điểm danh" → AttendanceEventTiles
        │            ├── Ngày | Buổi sáng/chiều | StatusPill
        │            └── Lý do / Liên kết đơn nghỉ
        │
        ├──► [BUTTON 4: "Học phí"]  ← GỘP Học phí + TB Học phí
        │         │                    (giống pattern phụ huynh)
        │         ▼
        │    [BOTTOM SHEET: Student Tuition Alert]
        │      ├── Kiểm tra: unpaidSum = tổng bill 'Chưa đóng'
        │      │
        │      ├── (Nếu unpaidSum == 0)
        │      │     ├── Banner xanh: "ĐÃ HOÀN THÀNH HỌC PHÍ"
        │      │     └── "Học sinh Nguyễn Minh An đã hoàn thành đầy đủ..."
        │      │
        │      └── (Nếu unpaidSum > 0)
        │            ├── Banner đỏ: "CÒN KHOẢN HỌC PHÍ CHƯA ĐÓNG"
        │            ├── Chi tiết số tiền + hạn nộp (30/06/2026)
        │            ├── Danh sách tuitionNotifs (tag == 'Học phí')
        │            │     └── Cards: Icon ⚠️ + Title + Body
        │            └── [BUTTON: "Đóng học phí"]
        │                  │
        │                  ▼ (pop bottom sheet, push screen)
        │            [SCR-HS-TUITION: TUITION PAYMENT SCREEN]
        │              ├── AppBar: "Thanh toán học phí"
        │              ├── Student info card (tên + lớp)
        │              ├── Danh sách bills (Unpaid + Paid)
        │              │     └── Mỗi bill: Tên khoản, Số tiền, StatusPill
        │              ├── Tổng chưa đóng
        │              ├── [BUTTON: "Thanh toán"] (nếu có unpaid)
        │              │         │
        │              │         ▼
        │              │    [BOTTOM SHEET: QR Code]
        │              │      ├── Mã VietQR (mock)
        │              │      ├── Số tiền + Nội dung CK
        │              │      └── [BUTTON: "Xác nhận chuyển khoản"]
        │              │            ├── bill.status → 'Đang xử lý'
        │              │            ├── API: POST /api/tuition/pay
        │              │            └── Ẩn thông báo học phí liên quan
        │              │
        │              └── "Lịch sử giao dịch" (Paid bills)
        │
        └──► [BUTTON 5: "Đơn từ & Biểu mẫu"]
                  │
                  ▼
             [SCR-HS-FORMS: FORMS SCREEN]
               ├── AppBar: "Đơn từ & Biểu mẫu"
               └── Danh sách biểu mẫu đăng ký CLB trong trường
                     ├── CLB Bóng đá — [Button "Đăng ký"]
                     ├── CLB Tiếng Anh — [Button "Đăng ký"]
                     ├── CLB Lập trình — [Button "Đăng ký"]
                     └── ... (các CLB khác)
```

### So sánh HomeStudent vs HomeParent:

```text
┌─────────────────────┬──────────────────────┬──────────────────────┐
│     Chức năng        │   Phụ huynh (5 btn)  │   Học sinh (5 btn)   │
├─────────────────────┼──────────────────────┼──────────────────────┤
│ Thời khóa biểu      │         ✅            │         ✅            │
│ Bảng điểm           │         ✅            │         ✅            │
│ Chuyên cần          │         ✅            │         ✅            │
│ Học phí (gộp TB)    │    ✅ (bottom sheet)  │    ✅ (bottom sheet)  │
│ Đơn từ              │  LeaveRequest (xin nghỉ)│  FormsScreen (đăng ký CLB)│
└─────────────────────┴──────────────────────┴──────────────────────┘

Pattern chung: Cả 2 đều dùng Bottom Sheet → Payment Screen
  khi có học phí chưa đóng (giống nhau 100%)
```

---

## 4. HomeTeacher Flow

```text
[APP SHELL — Tab 0: Trang chủ]
  │
  ▼
[SCR-GV-01: HOME TEACHER (HomeTeacher)]
  │
  ├── [COMPONENT: SharedHeader]
  │         (KHÔNG có icon 💬 tin nhắn)
  │
  ├── [COMPONENT: Orange Profile Card]
  │     ├── Icon 👨‍🏫 + "GVCN Lớp 12A"
  │     └── "Môn dạy: PRM393 - SE1913 • FPT Schools"
  │
  └── [COMPONENT: GridView 2×4 — 8 Feature Buttons]
        │
        ├──► [BUTTON 1: "Lớp được phân công"]
        │         │
        │         ▼
        │    [SCR-GV-02: ASSIGNED CLASSES SCREEN]
        │      ├── AppBar: "Lớp được phân công"
        │      ├── Section "Lớp chủ nhiệm"
        │      │     └── ClassCard (12A — GVCN, 42 HS, 2 vắng chưa có đơn)
        │      ├── Section "Lớp giảng dạy"
        │      │     ├── ClassCard (SE1913 — PRM393, 36 HS)
        │      │     └── ClassCard (11B — Kỹ năng dự án, 39 HS)
        │      └── Section "Danh bạ nhanh"
        │            └── Liện hệ PH (SĐT, tỷ lệ đọc thông báo)
        │
        ├──► [BUTTON 2: "Điểm danh lớp"]
        │         │
        │         ▼
        │    [SCR-GV-03: TEACHER ATTENDANCE SCREEN]
        │      ├── AppBar: "Điểm danh lớp"
        │      ├── Stats Panel (Có mặt / Muộn / Vắng)
        │      └── "Danh sách lớp 12A" → RosterTiles
        │            ├── Mỗi HS: Tên + Mã
        │            ├── Dropdown trạng thái:
        │            │     ├── "Có mặt" (green)
        │            │     ├── "Muộn" (warning)
        │            │     ├── "Vắng có phép" (blue)
        │            │     └── "Vắng không phép" (red)
        │            └── API: POST /api/attendance
        │                  └── Ghi audit log
        │
        ├──► [BUTTON 3: "Duyệt đơn xin nghỉ"]
        │         │
        │         ▼
        │    [SCR-GV-04: TEACHER LEAVE REQUESTS SCREEN]
        │      ├── AppBar: "Duyệt đơn xin nghỉ"
        │      └── "Danh sách đơn chờ duyệt" → PendingRequestCards
        │            ├── Mỗi đơn: Tên HS + Thời gian + Lý do
        │            └── 2 actions:
        │                  ├── [BUTTON "Duyệt"] → Approved
        │                  │     ├── API: PUT /api/leave-requests/{id}/approve
        │                  │     ├── State: Cập nhật chuyên cần →
        │                  │     │   'Vắng có phép' + mã đơn
        │                  │     └── SnackBar: "Đã duyệt đơn..."
        │                  │
        │                  └── [BUTTON "Từ chối"] → Rejected
        │                        ├── API: PUT /api/leave-requests/{id}/reject
        │                        └── SnackBar: "Đã từ chối đơn..."
        │
        ├──► [BUTTON 4: "Nhập & Upload điểm"]
        │         │
        │         ▼
        │    [SCR-GV-05: GRADES WEB SCREEN]
        │      ├── AppBar: "Nhập điểm lớp"
        │      ├── Selector: Lớp / Môn / Học kỳ
        │      │
        │      ├── Tab "Nhập tay":
        │      │     ├── DataTable (scroll ngang)
        │      │     │     └── Rows: Tên HS | Miệng | 15p | 1 tiết | HK
        │      │     ├── Inline edit cells (tap để sửa)
        │      │     ├── Validate thang điểm 0-10
        │      │     └── [BUTTON: "Lưu nháp"] / [BUTTON: "Công bố"]
        │      │
        │      └── Tab "Upload Excel":
        │            ├── [BUTTON: "Tải template"]
        │            │     └── API: GET /api/grades/template?class=12A&subject=PRM393
        │            ├── [BUTTON: "Chọn file Excel"]
        │            │     └── File picker → upload
        │            ├── Preview: Dòng hợp lệ (green) / Dòng lỗi (red)
        │            │     ├── Lỗi: Sai định dạng điểm
        │            │     ├── Lỗi: Sai mã HS
        │            │     └── Lỗi: Trùng cột
        │            └── [BUTTON: "Xác nhận Import"]
        │                  ├── API: POST /api/grades/import
        │                  └── Rollback nếu có lỗi nghiêm trọng
        │
        ├──► [BUTTON 5: "Gửi thông báo lớp"]
        │         │
        │         ▼
        │    [SCR-GV-07: ANNOUNCEMENTS CREATE SCREEN]
        │      ├── AppBar: "Tạo thông báo mới"
        │      ├── Form: Tiêu đề (TextField, required)
        │      ├── Form: Nội dung (TextField, required, multiline)
        │      ├── Selector: Lớp nhận (Dropdown: 12A, SE1913...)
        │      ├── Checkbox: "Bắt buộc xác nhận từ PH"
        │      └── [BUTTON: "Gửi thông báo"]
        │            ├── Validate: tiêu đề/nội dung rỗng → SnackBar lỗi
        │            ├── API: POST /api/announcements
        │            ├── Pop → SnackBar: "Đã gửi thông báo..."
        │            └── requiresReply = true → PH thấy badge đỏ
        │
        ├──► [BUTTON 6: "Thống kê lớp học"]
        │         │
        │         ▼
        │    [SCR-GV-09: TEACHER STATS SCREEN]
        │      ├── AppBar: "Thống kê lớp học"
        │      ├── Section "Tổng quan lớp 12A"
        │      │     ├── StatItem: Tỷ lệ chuyên cần (94%) — Progress bar
        │      │     ├── StatItem: Điểm TB học tập (7.8/10) — Progress bar
        │      │     └── StatItem: Tương tác PH đã đọc (86%) — Progress bar
        │      └── Section "Cảnh báo tự động từ AI"
        │            ├── WarningItem (red): "3 HS vắng quá 2 buổi..."
        │            └── WarningItem (yellow): "Điểm TB Tiếng Anh giảm 0.4..."
        │
        ├──► [BUTTON 7: "QL Học phí"] ← có badge count (unpaidCount)
        │         │
        │         ▼
        │    [SCR-GV-TUITION: TEACHER TUITION SCREEN]
        │      ├── AppBar: "Quản lý học phí lớp"
        │      ├── Stats (Tổng HS / Đã đóng / Chưa đóng)
        │      ├── Filter chips (Tất cả / Đã đóng / Chưa đóng)
        │      └── Student list → StudentTuitionTiles
        │            ├── Tên HS + Lớp
        │            ├── StatusPill (Đã đóng green / Chưa đóng red)
        │            └── [ICON: Gửi nhắc] (nếu chưa đóng)
        │                  └── API: POST /api/tuition/remind/{studentId}
        │                       └── SnackBar: "Đã gửi nhắc nhở..."
        │
        └──► [BUTTON 8: "Lịch dạy"]
                  │
                  ▼
             [SCR-GV-SCHEDULE: SCHEDULE SCREEN]
               ├── AppBar: "Lịch dạy"
               ├── Month Selector (← Tháng 06/2026 →)
               ├── Week Day Selector (T2-T7)
               ├── Section "Buổi sáng": LessonCards
               └── Section "Buổi chiều": LessonCards
```

### Cross-actor triggers từ HomeTeacher:

```text
[HOME TEACHER — Cross-actor triggers]
  │
  ├── Duyệt đơn nghỉ Approved
  │     ├── Cập nhật DB: attendance = 'Vắng có phép'
  │     ├── PH thấy status='Approved' trong LeaveRequestList
  │     └── HS thấy 'Vắng có phép' trong AttendanceScreen
  │
  ├── Gửi thông báo lớp → API POST /api/announcements
  │     ├── PH thấy trong Tab 2: Thông báo (requiresReply: true → badge đỏ)
  │     └── HS thấy trong Tab 2: Thông báo
  │
  ├── Nhập/Upload điểm → API POST /api/grades
  │     ├── PH thấy điểm mới trong GradesScreen
  │     └── HS thấy điểm mới trong GradesScreen
  │
  ├── Gửi nhắc nhở học phí → API POST /api/tuition/remind
  │     ├── PH thấy tuition notification mới
  │     └── HS thấy tuition notification mới
  │
  └── Trả lời tin nhắn PH
        └── PH thấy reply trong Tab 1: Tin nhắn
```

---

## 5. Bottom Navigation Tabs

### 5.1. Tab 1: 💬 Tin nhắn (ConversationsScreen)

```text
[TAB 1: TIN NHẮN — ConversationsScreen(actor)]
  │
  ├──► [IF actor == TEACHER]
  │         │
  │         ▼
  │    [SCR-GV-08: TEACHER INBOX SCREEN]
  │      ├── AppBar: "Tin nhắn phụ huynh"
  │      └── "Luồng tin nhắn cần phản hồi" → ThreadCards
  │            ├── Mỗi thread:
  │            │     ├── Tên PH
  │            │     ├── Tên HS + Lớp ("Trần Hoàng Nam - 12A")
  │            │     ├── Tin nhắn preview (dòng cuối)
  │            │     ├── Tag pill (Đơn nghỉ / Điểm / Thông báo)
  │            │     └── Thời gian
  │            └── Tap thread → ChatDetailScreen
  │                  └── Gửi reply → thêm tin nhắn mock trong UI hiện tại
  │                       └── PH nhận trong Tab 1
  │
  └──► [IF actor == PARENT hoặc STUDENT]
            │
            ▼
       [SCR-COM-TINNHAN: CONVERSATIONS SCREEN]
         ├── AppBar: "Tin nhắn liên lạc"
         └── "Hộp thoại gần đây" → ThreadCards
               ├── Mỗi thread:
               │     ├── Tên GV
               │     ├── Tên GV ("Cô Nguyễn Thu Hà")
               │     ├── Tin nhắn preview (dòng cuối)
               │     └── Thời gian
               └── Tap thread → ChatDetailScreen
                     └── Gửi reply → thêm tin nhắn mock trong UI hiện tại
                          └── GV nhận trong TeacherInboxScreen
```

### 5.2. Tab 2: 📢 Thông báo (AnnouncementsScreen)

```text
[TAB 2: THÔNG BÁO — AnnouncementsScreen(actor)]
  │
  ├──► [IF actor == PARENT]
  │         │
  │         ▼
  │    [SCR-COM-THONGBAO: ANNOUNCEMENTS SCREEN]
  │      ├── AppBar: "Trung tâm thông báo"
  │      └── "Thông báo lớp học & nhà trường" → AnnouncementCards
  │            ├── Mỗi thông báo:
  │            │     ├── Title ("Lịch thi cuối kỳ II")
  │            │     ├── Body (nội dung chi tiết)
  │            │     └── Tag pill:
  │            │           ├── "Quan trọng" (orange) — TB từ nhà trường
  │            │           ├── "Cần phản hồi" (yellow) — requiresReply: true
  │            │           │     └── Nếu chưa xác nhận → badge đỏ trên Tab
  │            │           │     └── Tap → [BOTTOM SHEET: Xác nhận]
  │            │           │           └── [BUTTON: "Xác nhận đã đọc"]
  │            │           │                 ├── API: PUT /api/announcements/{id}/read
  │            │           │                 └── Badge đỏ biến mất
  │            │           └── "Tin hệ thống" (blue) — TB chung
  │            │
  │            └── includes: TB học phí từ GV gửi
  │                  └── (đã hiển thị ở HomeParent bottom sheet)
  │
  ├──► [IF actor == STUDENT]
  │         │
  │         ▼
  │    [SCR-COM-THONGBAO: ANNOUNCEMENTS SCREEN]
  │      ├── AppBar: "Trung tâm thông báo"
  │      └── "Thông báo cá nhân" → AnnouncementCards
  │            ├── TB lớp học (Lịch thi, lịch kiểm tra...)
  │            ├── TB hệ thống (Cập nhật, bảo trì...)
  │            └── Tag pills tương tự parent
  │
  └──► [IF actor == TEACHER]
            │
            ▼
       [SCR-COM-THONGBAO: ANNOUNCEMENTS SCREEN]
         ├── AppBar: "Trung tâm thông báo"
         └── "Thông báo đã gửi & Tracking" → AnnouncementCards
               ├── Mỗi thông báo đã gửi:
               │     ├── Title + Body
               │     ├── Tag "Đã gửi" (green)
               │     ├── Thống kê: "Đã đọc 78%"
               │     └── Tap → Chi tiết tracking
               │           ├── Tổng số PH nhận
               │           ├── Đã đọc / Chưa đọc
               │           └── Danh sách PH chưa đọc
               │                 └── [BUTTON: "Nhắc nhở"]
               │                       └── API: POST /api/announcements/{id}/nudge
```

### 5.3. Tab 3: 👤 Tài khoản (AccountProfileScreen)

```text
[TAB 3: TÀI KHOẢN — AccountProfileScreen(actor)]
  │
  ├──► [COMPONENT: Bento ID Card]
  │     ├── Tên + Subtitle (vai trò)
  │     ├── ID Label + Value (Mã PH/GV/HS)
  │     ├── Dept Label + Value (Học kỳ/Bộ môn/Lớp)
  │     └── Decorative icon background (opacity 5%)
  │
  ├──► [COMPONENT: Contact Info Card]
  │     ├── Email
  │     ├── SĐT
  │     └── StatusPill "Đang hoạt động" (green)
  │
  ├──► [COMPONENT: Cài đặt & Thiết lập]
  │     ├── "Đổi mật khẩu" → Tap (chưa có screen)
  │     ├── "Ngôn ngữ hiển thị" → Tiếng Việt / English
  │     └── "Giao diện hiển thị" → Chế độ sáng/tối
  │
  └──► [BUTTON: "Đăng xuất khỏi ứng dụng"] (red)
            │
            ├── Xác nhận logout
            ├── API: POST /api/auth/logout (invalidate JWT)
            ├── Xóa JWT từ SecureStorage
            └── pushAndRemoveUntil → [SCR-COM-LOGIN: LOGIN SCREEN]
                  └── SnackBar: "Đã đăng xuất khỏi tài khoản!"
```

---

## Tổng kết: Cross-Actor Interactions

### Qua Tin nhắn (Tab 1)

```text
[CROSS-ACTOR: Tin nhắn (Tab 1)]
  │
  ├── PH/HS gửi tin nhắn → GV thấy trong TeacherInboxScreen
  │
  └── GV reply tin nhắn → PH/HS thấy trong ConversationsScreen
```

### Qua Thông báo (Tab 2)

```text
[CROSS-ACTOR: Thông báo (Tab 2)]
  │
  ├── GV tạo thông báo → PH/HS thấy trong AnnouncementsScreen
  │     └── requiresReply: true → badge đỏ cho đến khi xác nhận
  │
  └── PH xác nhận → Badge đỏ biến mất (cả tabs đều realtime update)
```

### Qua Học phí

```text
[CROSS-ACTOR: Học phí]
  │
  ├── PH/HS thanh toán → API POST /api/tuition/pay
  │     └── TeacherTuitionScreen cập nhật trạng thái
  │
  └── GV gửi nhắc nhở → API POST /api/tuition/remind
        └── PH/HS thấy tuition notification mới
```

### Qua Đơn xin nghỉ

```text
[CROSS-ACTOR: Đơn xin nghỉ]
  │
  ├── PH tạo đơn → API POST /api/leave-requests
  │     └── GV thấy trong TeacherLeaveRequestsScreen
  │
  ├── GV duyệt Approved → API PUT /api/leave-requests/{id}/approve
  │     ├── PH thấy status='Approved' trong LeaveRequestList
  │     ├── HS thấy 'Vắng có phép' trong AttendanceScreen
  │     └── Chuyên cần tự động cập nhật + mã đơn liên kết
  │
  └── GV từ chối → API PUT /api/leave-requests/{id}/reject
        └── PH thấy status='Rejected' trong LeaveRequestList
```

### Qua Điểm số

```text
[CROSS-ACTOR: Điểm số]
  │
  ├── GV nhập/Upload điểm → API POST /api/grades
  │     ├── PH thấy điểm mới trong GradesScreen
  │     └── HS thấy điểm mới trong GradesScreen
  │
  └── HS mô phỏng điểm (simulation mode)
        └── Only client-side, không ảnh hưởng DB
```

---

## Bảng tổng hợp API

| Actor | Screen | Action | API Endpoint | MySQL Table |
|:---|:---|:---|:---|:---|
| Tất cả | Login | Đăng nhập | `POST /api/auth/login` | `users` |
| Tất cả | Profile | Xem thông tin | `GET /api/user/profile` | `users` |
| Tất cả | Logout | Đăng xuất | `POST /api/auth/logout` | `sessions` |
| PH/HS | Schedule | Xem TKB | `GET /api/schedule` | `schedules` |
| PH/HS | Grades | Xem điểm | `GET /api/grades/{studentId}` | `grades` |
| PH/HS | Attendance | Xem chuyên cần | `GET /api/attendance/{studentId}` | `attendance` |
| PH | LeaveRequest | Tạo đơn | `POST /api/leave-requests` | `leave_requests` |
| PH | Tuition | Xem TB học phí | `GET /api/notifications?tag=tuition` | `notifications` |
| PH/HS | Tuition | Thanh toán | `POST /api/tuition/pay` | `payment_transactions`, `tuition_bills` |
| HS | Forms | Đăng ký CLB | `POST /api/clubs/register` | `club_registrations` |
| GV | AssignedClasses | Xem lớp | `GET /api/classes` | `classes` |
| GV | Attendance | Điểm danh | `POST /api/attendance` | `attendance` |
| GV | LeaveRequest | Duyệt đơn | `PUT /api/leave-requests/{id}/approve` | `leave_requests`, `attendance` |
| GV | LeaveRequest | Từ chối | `PUT /api/leave-requests/{id}/reject` | `leave_requests` |
| GV | Grades | Nhập điểm | `POST /api/grades` | `grades` |
| GV | Grades | Upload Excel | `POST /api/grades/import` | `grades` |
| GV | Grades | Tải template | `GET /api/grades/template` | — |
| GV | Announcements | Tạo thông báo | `POST /api/announcements` | `announcements` |
| GV | Stats | Xem thống kê | `GET /api/stats/{classId}` | `attendance`, `grades` |
| GV | Tuition | QL học phí | `GET /api/classes/{id}/tuition-status` | `tuition_bills` |
| GV | Tuition | Nhắc nhở | `POST /api/tuition/remind/{studentId}` | `notifications` |
| Teacher | Inbox | Xem inbox | `GET /api/messages/inbox` | `messages` |
| PH/HS | Messages | Gửi tin nhắn | `POST /api/messages` | `messages` |
| PH | Announcements | Đã đọc | `PUT /api/announcements/{id}/read` | `announcement_reads` |
| GV | Announcements | Nhắc nhở | `POST /api/announcements/{id}/nudge` | `notifications` |

---

*Phiên bản: 1.0 — Ngày tạo: 2026-06-24*
*Dựa trên: business_model.md + codebase Flutter hiện tại*
