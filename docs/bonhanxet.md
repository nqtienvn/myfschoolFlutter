# Thiết kế lại Bảng điểm PH/HS & Xây dựng Web Giáo viên

## Mô tả thay đổi

1. **Giao diện PH/HS**:
   - Thiết kế lại màn hình **Bảng điểm** cho Phụ huynh và Học sinh thành **1 trang cuộn duy nhất** (không tab):
     - **Bảng tổng kết học lực** — Danh hiệu, ĐTB, Hạnh kiểm, Học lực, Xếp hạng + Nhận xét GVCN.
     - **Bảng điểm chi tiết TẤT CẢ môn** — 1 bảng duy nhất (kiểu tracuu.vnedu.vn) + cột Nhận xét GV từng môn.
   - Loại bỏ các nút riêng lẻ trên Home (Nhận xét HK, Lịch họp, KT & VP).
   - Bỏ hiển thị vi phạm cho PH/HS (chỉ dùng nội bộ GV để Submit).

2. **Hồ sơ lớp chủ nhiệm**:
   - Loại bỏ thống kê **"Tương tác phụ huynh (đã đọc)"** ở cả bản Mobile và Web. Không cần tính toán/hiển thị tỷ lệ đọc thông báo.

3. **Web Giáo viên (Teacher Web Portal) — Song song với App Mobile**:
   - Xây dựng thêm một phân hệ Web dành riêng cho Giáo viên (GVBM & GVCN) để tăng tính tiện lợi khi nhập liệu trên máy tính.
   - Phân hệ Web này tích hợp trực tiếp cùng dự án `admin-web` nhưng có trang đăng nhập riêng biệt.
   - **Giữ nguyên ứng dụng Mobile dành cho Giáo viên (GV không bị bỏ app)**. Cả 2 nền tảng Mobile App và Web Portal dành cho Giáo viên sẽ hoạt động song song và đồng bộ dữ liệu với nhau qua API. Các thay đổi về nghiệp vụ (nhập nhận xét, submit vi phạm, bỏRequiresReply...) sẽ được cập nhật đồng thời trên cả App Mobile và Web.

---

## Nghiệp vụ thay đổi

### Phụ huynh (PARENT)

| Trước | Sau |
|-------|-----|
| Nút **Bảng điểm** → xem từng môn (ExpansionTile) | Nút **Bảng điểm** → 1 trang cuộn: tổng kết + bảng điểm tất cả môn |
| Nút **Nhận xét HK** → màn hình riêng | ❌ Bỏ nút → tích hợp vào bảng tổng kết + cột NX trong bảng điểm |
| Nút **Lịch họp & tuyên dương** → màn hình riêng | ❌ Bỏ hẳn (lịch họp chuyển sang Thông báo) |

### Học sinh (STUDENT)

| Trước | Sau |
|-------|-----|
| Nút **Bảng điểm** → xem từng môn | Nút **Bảng điểm** → 1 trang cuộn: tổng kết + bảng điểm |
| Nút **Nhận xét HK** → màn hình riêng | ❌ Bỏ nút → tích hợp vào bảng tổng kết + cột NX |
| Nút **KT & VP** → màn hình riêng | ❌ Bỏ hẳn (danh hiệu ở bảng tổng kết, vi phạm bỏ hiển thị) |

### Vi phạm — Thay đổi nghiệp vụ

| Trước | Sau |
|-------|-----|
| PH/HS thấy danh sách vi phạm (StudentEvent VIOLATION) | ❌ PH/HS **KHÔNG** thấy vi phạm |
| Vi phạm hiển thị ở StudentEngagementScreen | Vi phạm chỉ dùng để gửi Admin tính hạnh kiểm |
| GVCN ghi nhận vi phạm + công bố cho PH/HS | GVCN ghi nhận vi phạm → **Submit** → Admin tổng hợp và chạy logic tự động tính hạnh kiểm |

### Công thức tự động tính Hạnh kiểm từ Vi phạm (Admin xử lý)

**Quy tắc**: Mặc định hạnh kiểm = `Tốt`. Cứ **3 vi phạm** thì **trừ 1 bậc**.

#### Thang hạnh kiểm (4 bậc)

```
Tốt  →  Khá  →  Trung bình  →  Yếu
 (0)     (1)       (2)          (3)
```

#### Bảng tính

| Số vi phạm | Số bậc trừ | Hạnh kiểm |
|------------|-----------|-----------|
| 0 – 2     | 0         | **Tốt**   |
| 3 – 5     | 1         | **Khá**   |
| 6 – 8     | 2         | **Trung bình** |
| ≥ 9       | 3         | **Yếu**   |

#### Công thức

```
bậc_trừ = floor(số_vi_phạm / 3)
hạnh_kiểm = thang[min(bậc_trừ, 3)]
```

#### Luồng xử lý mới (Độc lập & Hệ thống tự gom)

```
GV bộ môn nhập nhận xét môn học ──→ Gửi thẳng lên hệ thống (Không qua GVCN duyệt)
                                                                │
GVCN nhập nhận xét chung & vi phạm ─→ Gửi thẳng lên hệ thống (Không quản lý GVBM)
                                                                │
                                                                ▼
Hệ thống Admin tự gom nhận xét môn (GVBM) và nhận xét chung/vi phạm (GVCN)
  │
  ├─ Tự động đếm vi phạm và tính hạnh kiểm gợi ý: floor(violations / 3)
  ├─ Tổng hợp kết quả học tập (GPA, xếp hạng, học lực, hạnh kiểm, danh hiệu)
  └─ Admin nhấn 1 nút "CÔNG BỐ" duy nhất để công bố toàn bộ kết quả lên app
                                                                │
                                                                ▼
PH/HS xem kết quả đã công bố đầy đủ (điểm số, hạnh kiểm, danh hiệu, tất cả nhận xét)
```

### Nghiệp vụ của Giáo viên (Thay đổi lớn)

#### 1. Giáo viên Bộ môn (GVBM)
- Nhập nhận xét môn học, điểm mạnh, cần cải thiện cho học sinh $\rightarrow$ Gửi lên hệ thống.
- **Không còn bị GVCN trả lại** (bỏ trạng thái `RETURNED` và nút "Trả lại" từ phía GVCN). Nhận xét gửi thẳng lên database để Admin quản lý.

#### 2. Giáo viên Chủ nhiệm (GVCN)
Màn hình nhận xét học sinh của GVCN (`_HomeroomStudentReportScreen`) được thiết kế cực kỳ tinh gọn, **không chia tab** mà chỉ tập trung vào phần việc của GVCN:
- **Nhập Nhận xét chung** của GVCN cho học sinh.
- **Ghi nhận lỗi vi phạm**:
  - Hiển thị danh sách các lỗi vi phạm đã ghi nhận trong học kỳ của học sinh đó.
  - Nút **"Thêm vi phạm"** mở Dialog để nhập lỗi vi phạm mới (bao gồm: Tiêu đề, Phân loại, Mô tả chi tiết, Ngày vi phạm).
  - Cho phép sửa/xóa các lỗi vi phạm trực tiếp.
- **Không có quyền chọn hạnh kiểm** (Trường dropdown Hạnh kiểm bị xóa bỏ hoàn toàn).
- Nút **"Lưu nháp"** và **"Submit"** (Thay thế cho nút "Công bố"). Khi nhấn "Submit", toàn bộ dữ liệu nhận xét chung và danh sách vi phạm của học sinh đó được lưu thẳng vào hệ thống để gửi Admin.
- Tại màn hình danh sách học sinh của GVCN: Nút **"Công bố cả lớp"** được thay bằng **"Submit cả lớp"**.
- **Không hiển thị/quản lý nhận xét của GVBM** (GVCN không cần quan tâm tiến độ GVBM đã nộp hay chưa). GVBM tự chịu trách nhiệm nộp nhận xét môn của họ.

#### 3. Nghiệp vụ Thông báo của Giáo viên
- **Gửi thông báo**:
  - Loại bỏ hoàn toàn tùy chọn *"Yêu cầu xác nhận hoặc phản hồi"* khi tạo/sửa thông báo (Xóa SwitchListTile `requiresReply` trên UI). Giá trị `requiresReply` sẽ luôn được truyền là `false` khi lưu hoặc cập nhật.
- **Xem thông báo**:
  - Trang **Thông báo** chính của Giáo viên (hiển thị từ AppShell tab Thông báo) sẽ hiển thị danh sách **thông báo đã nhận** (nhận từ Admin/Nhà trường gửi cho giáo viên) giống như giao diện hiển thị của phụ huynh và học sinh.
  - Danh sách **Thông báo đã gửi** trước đây sẽ được di chuyển vào bên trong trang **"Gửi thông báo lớp"** (`AnnouncementsCreateScreen`) để giáo viên có thể vừa tạo thông báo mới, vừa theo dõi lịch sử và trạng thái duyệt của các thông báo mình đã gửi ở cùng một nơi.

#### 4. Nghiệp vụ Học phí (Bỏ hẳn khỏi GVCN)
- **Bỏ quản lý học phí đối với GVCN**:
  - GVCN không cần theo dõi hay nhắc nhở đóng học phí của học sinh lớp chủ nhiệm nữa.
  - Nút/phần **"QL Học phí"** sẽ bị loại bỏ hoàn toàn khỏi cả Mobile App của Giáo viên và Web Giáo viên.
  - Việc nhắc nhở nộp học phí sẽ do hệ thống tự động chạy **cron job** từ phía Admin gửi thông báo định kỳ (theo khoảng thời gian thiết lập) trực tiếp tới các phụ huynh/học sinh chưa hoàn thành học phí.

---

## User Flow mới

```
Home PH / HS
  └─ [Bảng điểm] ──→ GradesScreen (1 trang cuộn)
       │
       ├─ Dropdown: Năm học · Học kỳ
       │
       ├─ ① BẢNG TỔNG KẾT HỌC LỰC
       │    ┌──────────────────┬──────────────────────────────────┐
       │    │ Danh hiệu        │ HS Tiên tiến                     │
       │    ├──────────────────┼──────────────────────────────────┤
       │    │ Điểm TB          │ 7.8                              │
       │    ├──────────────────┼──────────────────────────────────┤
       │    │ Hạnh kiểm        │ Tốt                              │
       │    ├──────────────────┼──────────────────────────────────┤
       │    │ Học lực           │ Khá                              │
       │    ├──────────────────┼──────────────────────────────────┤
       │    │ Xếp hạng         │ 5 / 45                           │
       │    ├──────────────────┼──────────────────────────────────┤
       │    │ Nhận xét GVCN    │ "Em ngoan, học tốt, tích cực     │
       │    │                  │  tham gia hoạt động..."           │
       │    │                  │                    — Cô Nguyễn A  │
       │    └──────────────────┴──────────────────────────────────┘
       │    * Nguồn: semester_results + periodic review (generalComment)
       │
       └─ ② BẢNG ĐIỂM CHI TIẾT (1 bảng, tất cả môn + cột Nhận xét)
            ┌──────────┬────────┬────────┬────────┬───────┬──────┬─────────────────┐
            │ Môn học  │Đ.miệng│ Đ.15p  │ Đ.1tiết│ HK    │ TBM  │ Nhận xét GV     │
            ├──────────┼────────┼────────┼────────┼───────┼──────┼─────────────────┤
            │ Toán học │  9     │ 8 8 8  │ 8 9    │  6    │ 7.8  │ "Tư duy tốt..." │
            │ Vật lí   │  9     │ 5 8    │ 6 8    │  5    │ 6.5  │ "Cần cố gắng.." │
            │ Hóa học  │  8     │ 8 8    │ 8 7    │  7    │ 7.5  │ "Làm bài tốt.." │
            │ ...      │  ...   │ ...    │ ...    │ ...   │ ...  │ ...             │
            │ Thể dục  │  Đ     │ Đ Đ    │ Đ Đ   │  Đ    │  Đ   │ "Tích cực..."   │
            └──────────┴────────┴────────┴────────┴───────┴──────┴─────────────────┘
            * Cuộn ngang nếu nhiều cột
            * Cột "Nhận xét GV" = comment từ GV bộ môn (SubjectPeriodicReview)
            * Nhấn vào ô nhận xét để xem đầy đủ (nếu text dài)
            * Nếu GV chưa gửi NX: hiện "—"
```

---

## Chi tiết từng phần

### ① Bảng tổng kết Học lực

Dữ liệu từ **2 nguồn**:

| Hạng mục | Field | Nguồn |
|----------|-------|-------|
| Danh hiệu | `honor` | `semester_results` |
| Điểm TB | `gpa` | `semester_results` |
| Hạnh kiểm | `conduct` | `semester_results` |
| Học lực | `academic_ability` | `semester_results` |
| Xếp hạng | `rank` | `semester_results` |
| **Nhận xét GVCN** | `generalComment` + `homeroomTeacherName` | `periodic_report` (published) |

**Hiển thị**: Bảng 2 cột dọc (label | value), mỗi hàng 1 hạng mục.
- Hàng "Nhận xét GVCN" có thể nhiều dòng, hiển thị italic + tên GV bên dưới.
- Nếu chưa có `semester_results`: "Chưa có kết quả tổng kết."
- Nếu chưa có nhận xét GVCN: "Chưa có nhận xét." (nhưng vẫn hiển thị các hàng khác)

### ② Bảng điểm chi tiết — Tất cả môn + Nhận xét

Dữ liệu từ **2 nguồn**:

**Cột điểm** — từ `transcript.subjects[].scores[]`:

| Cột | Nguồn | Ghi chú |
|-----|-------|---------|
| Môn học | `subjectName` | Cột cố định (sticky left) |
| Điểm miệng | scores "miệng" / "Kiểm tra miệng" | Nhiều điểm → cách nhau bằng dấu cách |
| Điểm 15 phút | scores "15 phút" | Tương tự |
| Điểm 1 tiết | scores "1 tiết" / "giữa kỳ" | Tương tự |
| Học kỳ | scores "học kỳ" / "cuối kỳ" | Điểm thi cuối kỳ |
| TBM | `average` | Làm tròn 1 chữ số |

**Cột nhận xét** — từ `publishedReport.subjectReviews[]`:

| Cột | Nguồn | Ghi chú |
|-----|-------|---------|
| Nhận xét GV | `SubjectPeriodicReview.comment` | Match theo `subjectName` hoặc `subjectId` |

**Mapping**: Join 2 danh sách theo tên môn. Mỗi dòng = 1 môn, cột cuối = nhận xét GV bộ môn cho môn đó.

**Xử lý đặc biệt**:
- Môn `PASS_FAIL`: Hiển thị "Đ" (Đạt) hoặc "CĐ" (Chưa đạt)
- Môn `COMMENT`: Hiển thị "—" ở cột điểm
- Cột "Nhận xét GV" nếu text dài → truncate + nhấn để xem đầy đủ (dialog/bottom sheet)
- Nếu chưa có nhận xét: hiện "—"

## Phân hệ Web Giáo viên (Teacher Web Portal)

Phân hệ Web dành riêng cho Giáo viên được xây dựng chung trong mã nguồn dự án `admin-web` để chia sẻ tài nguyên (components, styles, api client) nhưng có phân quyền và giao diện hoạt động độc lập.

### 1. Đường dẫn & Đăng nhập (Routing & Authentication)
- **Đăng nhập riêng**: URL `/teacher/login` dành riêng cho Giáo viên (giao diện đăng nhập khác với Admin).
- **Dashboard Giáo viên**: URL `/teacher/dashboard` (chỉ truy cập được với tài khoản có role `TEACHER`).
- **Layout riêng**: Sidebar hiển thị các menu chức năng dành riêng cho Giáo viên thay vì danh mục quản trị của Admin.

### 2. Các chức năng trên Web Giáo viên (Đầy đủ như App Mobile)

- **Lớp giảng dạy & Nhập điểm**:
  - Xem danh sách các lớp & môn được phân công dạy.
  - Nhập điểm trực tiếp trên lưới dữ liệu (data grid) hoặc upload bảng điểm từ file Excel/CSV.
- **Nhập nhận xét định kỳ**:
  - **GVBM**: Nhập nhận xét môn học (Nhận xét, điểm mạnh, cần cải thiện) cho các lớp mình dạy và gửi đi.
  - **GVCN**: Nhập nhận xét chung của GVCN + Quản lý danh sách vi phạm của học sinh (thêm/sửa/xóa lỗi vi phạm trực tiếp). Nhấn **Submit** để gửi toàn bộ dữ liệu lên hệ thống (Không chia tab, không có dropdown chọn Hạnh kiểm, không hiển thị/quản lý nhận xét của GVBM).
- **Quản lý thông báo lớp**:
  - **Thông báo đã nhận**: Xem các thông báo từ Nhà trường/Admin gửi đến giáo viên (Giao diện inbox giống PH/HS).
  - **Gửi thông báo mới**: Soạn thông báo gửi đến các lớp mình dạy/chủ nhiệm (Loại bỏ checkbox yêu cầu xác nhận/phản hồi).
  - **Lịch sử đã gửi**: Hiển thị danh sách thông báo chính giáo viên đó đã gửi và trạng thái phê duyệt của Admin ngay bên dưới form tạo thông báo.
- **Hồ sơ lớp chủ nhiệm (GVCN)**:
  - Xem thống kê chuyên cần trung bình, GPA trung bình của lớp.
  - **❌ KHÔNG hiển thị thống kê tỷ lệ phụ huynh đã đọc thông báo**.
  - Xem danh sách học sinh của lớp, kết quả học tập chi tiết từng học sinh và xếp hạng.
- **Duyệt đơn xin nghỉ (GVCN)**:
  - Xem danh sách đơn xin nghỉ học của học sinh lớp chủ nhiệm, xem lý do phép và duyệt hoặc từ chối.
- **Tin nhắn / Chat**:
  - Giao diện chat trực tiếp với Phụ huynh và Học sinh trong lớp chủ nhiệm hoặc lớp giảng dạy.

## Phân hệ Quản trị Admin — Tab Quản lý Kết quả (Admin Unified Results Management)

Trên giao diện Admin Web, thay vì trang Quản lý điểm đơn lẻ, Admin sẽ có một phân hệ tích hợp mang tên **Quản lý kết quả (Result Management)** để tổng hợp và công bố toàn bộ dữ liệu học tập, nề nếp của học sinh.

### 1. Cấu trúc Tab "Quản lý kết quả" của Admin

Phân hệ này được chia làm các tiểu mục (Sub-tab/Section) để Admin dễ dàng nhập liệu và kiểm duyệt:

- **Mục 1: Quản lý Điểm & Nhận xét môn (Grades & Subject Comments)**:
  - Hiển thị bảng điểm chi tiết của từng lớp, từng môn (miệng, 15p, 1 tiết, học kỳ).
  - Tích hợp hiển thị **Nhận xét của GVBM** tương ứng ở mỗi môn học ngay bên cạnh điểm thi để Admin tiện theo dõi.
  - Cho phép Admin công bố điểm **theo ngày / linh hoạt**:
    - Khi GVBM nhập điểm đến đâu, Admin có thể nhấn công bố điểm thành phần đó ngay lập tức (không bắt buộc phải đợi cuối kỳ).
    - Phụ huynh và học sinh sẽ cập nhật được điểm thành phần trên App Mobile theo thời gian thực để theo dõi.
- **Mục 2: Quản lý Vi phạm (Violations Management)**:
  - Admin xem danh sách các lỗi vi phạm của học sinh do GVCN gửi lên.
  - Cho phép Admin thêm, sửa, xóa lỗi vi phạm trực tiếp để đảm bảo tính chính xác trước khi tính hạnh kiểm.
- **Mục 3: Chuyên cần (Attendance)**:
  - Tổng hợp số buổi nghỉ có phép, nghỉ không phép, và đi muộn của từng học sinh trong học kỳ (dùng để phục vụ xét hạnh kiểm và học lực).

### 2. Bảng tổng kết học kỳ của Admin (Semester Overall Summary)

Sau khi tính toán xong tất cả điểm số, vi phạm và chuyên cần, hệ thống sẽ cung cấp cho Admin một **Bảng tổng kết của cả lớp** hiển thị chi tiết các thông số của từng học sinh:

| Học sinh | Điểm TB (GPA) | Vi phạm | Nghỉ học (P / KP) | Học lực gợi ý | Hạnh kiểm gợi ý | Học lực cuối | Hạnh kiểm cuối | Danh hiệu | Trạng thái |
|----------|---------------|---------|--------------------|---------------|-----------------|--------------|----------------|-----------|------------|
| Nguyễn A | 8.5           | 2       | 0 phép / 0 KP      | Giỏi          | Tốt             | [Dropdown]   | [Dropdown]     | [Dropdown]| Chờ CB     |
| Trần B   | 7.2           | 3       | 2 phép / 1 KP      | Khá           | Khá             | [Dropdown]   | [Dropdown]     | [Dropdown]| Đang CB    |

- **Cơ chế tính tự động**:
  - GPA và Xếp hạng được tự động tính từ bảng điểm.
  - Học lực được gợi ý dựa trên GPA môn và điều kiện khống chế.
  - Hạnh kiểm được hệ thống tự động tính từ số lỗi vi phạm (cứ 3 lỗi hạ 1 bậc hạnh kiểm, kết hợp với chỉ số chuyên cần nghỉ học quá buổi quy định).
- **Quyền quyết định của Admin**:
  - Admin có quyền thay đổi (override) Học lực cuối, Hạnh kiểm cuối, và Danh hiệu của từng học sinh trực tiếp trên bảng tổng kết này.
- **Nút CÔNG BỐ chung (Publish Summary Results)**:
  - Khi Admin nhấn nút **"Công bố kết quả học kỳ"** trên bảng tổng kết, toàn bộ thông tin tổng kết (GPA, học lực, hạnh kiểm, danh hiệu, nhận xét chung GVCN) sẽ chính thức được công bố lên App cho phụ huynh và học sinh xem (Tab 1 Bảng tổng kết của PH/HS).

---

## Files thay đổi

### 1. `lib/vn/edu/fpt/view/screens/grades_screen.dart` — VIẾT LẠI

Thay đổi toàn bộ giao diện:

- **Bỏ**: ExpansionTile từng môn, stat cards, card công thức tính điểm
- **Thêm params**: `authService`, `backendApiClient`
- **Thêm API calls**: `getStudentSemesterResult` + `getPublishedReport` (gọi song song với `getTranscript`)
- **Layout mới** (1 ListView cuộn dọc):
  1. Dropdown năm học · học kỳ
  2. Widget `_SemesterSummaryTable` — bảng tổng kết dọc (semester_results + NX GVCN)
  3. Widget `_AllSubjectsGradeTable` — bảng điểm tất cả môn + cột NX GV (cuộn ngang)

### 2. `lib/vn/edu/fpt/view/screens/home_screen_phuhuynh.dart` — MODIFY

- Xóa nút "Nhận xét học kỳ" (line 621–637)
- Xóa nút "Lịch họp & tuyên dương" (line 638–657)
- Cập nhật nút "Bảng điểm": truyền thêm `authService`, `backendApiClient`

### 3. `lib/vn/edu/fpt/view/screens/home_screen_hocsinh.dart` — MODIFY

- Xóa nút "Nhận xét học kỳ" (line 452–467)
- Xóa nút "Khen thưởng & vi phạm" (line 468–485)
- Cập nhật nút "Bảng điểm": truyền thêm `authService`, `backendApiClient`

### 4. `lib/vn/edu/fpt/view/screens/periodic_reviews_screen.dart` — MODIFY

Thay đổi giao diện của GVCN:
- Loại bỏ nút "Trả lại" nhận xét môn học.
- Đổi màn hình chi tiết HS của GVCN thành 2 tab: "Quản lý nhận xét GVBM" (chỉ xem) và "Nhận xét" (nhập nhận xét chung + ghi nhận số vi phạm).
- Xóa bỏ dropdown chọn Hạnh kiểm.
- Đổi nút "Công bố" và "Công bố cả lớp" thành "Submit" và "Submit cả lớp".

### 5. `lib/vn/edu/fpt/view/screens/home_screen_giaovien.dart` — MODIFY
- Xóa hoàn toàn nút **"Theo dõi học sinh"** (chỉ hiển thị khi `_classId != null`).
- Xóa hoàn toàn nút **"QL Học phí"** (chỉ hiển thị khi `_classId != null`).

### 6. `lib/vn/edu/fpt/view/screens/homeroom_monitoring_screen.dart` — [DELETE]
- Xóa tận gốc file này cùng toàn bộ UI theo dõi học sinh, cảnh báo nguy cơ, báo cáo lớp chủ nhiệm.

### 7. `lib/vn/edu/fpt/view/screens/student_engagement_screen.dart` — [DELETE]
- Xóa tận gốc file này do phần KT/VP của học sinh và lịch họp của phụ huynh đã bị bãi bỏ/tích hợp sang trang khác.

### 8. `lib/vn/edu/fpt/view/screens/teacher_tuition_screen.dart` — [DELETE]
- Xóa tận gốc file này do nghiệp vụ quản lý học phí của GVCN đã được tự động hóa bằng cron job của Admin.

### 9. `lib/vn/edu/fpt/view/screens/announcements_create_screen.dart` — MODIFY
- Loại bỏ SwitchListTile `requiresReply` và hardcode `_requiresReply = false` khi lưu/sửa.
- Danh sách thông báo đã gửi vẫn giữ nguyên tại màn hình này.

### 10. `lib/vn/edu/fpt/view/screens/announcement_inbox_screen.dart` — MODIFY
- Cập nhật luồng hiển thị của giáo viên: Khi giáo viên truy cập, trang này sẽ hiển thị **thông báo đã nhận** (nhận từ Admin/Nhà trường) thay vì "thông báo đã gửi" của chính giáo viên đó.

### 11. `lib/vn/edu/fpt/view/screens/teacher_stats_screen.dart` — MODIFY
- Xóa bỏ hiển thị chỉ số **"Tương tác phụ huynh (đã đọc)"** (`parentReadRate`) khỏi UI Hồ sơ lớp chủ nhiệm.

### 12. Phân hệ Web Giáo viên & Quản trị — `admin-web` — [NEW / MODIFY]
- **`admin-web/src/App.tsx` (MODIFY)**: Bổ sung định tuyến cho phân hệ Giáo viên bao gồm: `/teacher/login`, `/teacher/dashboard`, và các menu con.
- **`admin-web/src/pages/GradesManagementPage.tsx` (MODIFY)**: Thiết kế lại trang quản lý điểm của Admin thành Unified Portal 3 tab (Bảng điểm, Nhận xét & Vi phạm, Tổng kết học kỳ) và bổ sung nút "Công bố" đồng thời cho toàn bộ dữ liệu học tập.
- **`admin-web/src/pages/teacher/` (NEW)**: Tạo thư mục chứa các trang nghiệp vụ Giáo viên trên Web:
  - `TeacherLoginPage.tsx`: Giao diện đăng nhập riêng biệt.
  - `TeacherDashboardPage.tsx`: Trang chủ/Tổng quan của Giáo viên.
  - `TeacherGradesPage.tsx`: Xem lớp dạy, nhập điểm thủ công, upload CSV/Excel.
  - `TeacherPeriodicReviewsPage.tsx`: GVBM nhập nhận xét môn học. GVCN nhập nhận xét chung, thêm/sửa/xóa vi phạm của học sinh, nhấn **Submit** (giao diện đơn giản không chia tab).
  - `TeacherAnnouncementsPage.tsx`: Gửi thông báo lớp (bỏ requiresReply), xem danh sách đã gửi, xem hòm thư thông báo đã nhận.
  - `TeacherHomeroomProfilePage.tsx`: Hồ sơ lớp chủ nhiệm, xem danh sách học sinh và xếp hạng (không có tỷ lệ đọc thông báo).
  - `TeacherLeaveRequestsPage.tsx`: Duyệt/từ chối đơn nghỉ học.
  - `TeacherChatPage.tsx`: Nhắn tin với học sinh/phụ huynh.

### 13. Files KHÔNG thay đổi

| File | Lý do |
|------|-------|
| Model + API files | Không thay đổi, chỉ reuse |

---


## API cần gọi (trên 1 trang, song song)

| # | API | Dữ liệu | Hiển thị ở |
|---|-----|---------|-----------|
| 1 | `getTranscript(studentId, academicYearId, semesterId)` | Bảng điểm chi tiết các môn | Phần ② (cột điểm) |
| 2 | `getStudentSemesterResult(studentId, semesterId)` | GPA, hạnh kiểm, học lực, danh hiệu, xếp hạng | Phần ① |
| 3 | `getPublishedReport(studentId, academicYearId, semesterId)` | NX GVCN + NX từng môn | Phần ① (hàng NX GVCN) + Phần ② (cột NX GV) |

---

## So sánh trước/sau

| Hạng mục | Trước | Sau |
|----------|-------|-----|
| Bảng điểm | Xem từng môn (ExpansionTile) | 1 bảng tất cả môn + cột NX GV |
| Tổng kết HK | Không hiển thị ở PH/HS | Bảng tổng kết đầu trang (GPA, hạnh kiểm, học lực, danh hiệu, xếp hạng) |
| NX GVCN | Màn hình riêng | Hàng trong bảng tổng kết |
| NX GV bộ môn | Màn hình riêng | Cột cuối trong bảng điểm |
| Vi phạm | PH/HS thấy danh sách vi phạm | ❌ Bỏ hẳn — GVCN nhập vi phạm trực tiếp trong màn Nhận xét định kỳ để Submit |
| Khen thưởng | Màn hình StudentEvent riêng | Danh hiệu nằm trong bảng tổng kết |
| Lịch họp | Màn hình riêng | ❌ Bỏ → chuyển sang Thông báo |
| Quyền GVCN duyệt/trả lại | Có nút Trả lại nhận xét môn học | ❌ Bỏ duyệt/trả lại. GVBM gửi thẳng, GVCN tự Submit phần của mình độc lập |
| Quyền GVCN chọn hạnh kiểm | Có dropdown chọn Hạnh kiểm | ❌ Bỏ chọn, Admin tự động tính theo công thức vi phạm |
| Quyền GVCN công bố | Có nút Công bố nhận xét | ❌ Thay bằng nút "Submit", hệ thống tự gom, Admin duyệt và công bố |
| Cổng quản trị Admin | Trang Quản lý điểm riêng biệt | ✅ Cổng quản lý kết quả tích hợp (Mục Điểm & Nhận xét môn, Mục Vi phạm, Mục Chuyên cần) và Bảng tổng kết học kỳ để Admin tính toán, xét học lực/hạnh kiểm và công bố điểm theo ngày linh hoạt |
| Theo dõi học sinh (GVCN) | Màn hình Dashboard 3 tab | ❌ Bỏ tận gốc (Xóa file screen và nút trên Home GV) |
| Quản lý học phí (GVCN) | Có nút QL Học phí, màn hình theo dõi | ❌ Bỏ tận gốc — Tự động hóa bằng cron job của Admin nhắc đóng phí |
| Gửi thông báo (GV) | Có tùy chọn xác nhận/phản hồi | ❌ Bỏ tùy chọn này (luôn là false) |
| Xem thông báo đã nhận (GV) | Hiện "Thông báo đã gửi" của chính mình | ✅ Hiện thông báo đã nhận từ Admin/Trường giống như PH/HS |
| Xem thông báo đã gửi (GV) | Hiển thị ở màn hình Thông báo chính | Di chuyển vào mục **"Gửi thông báo lớp"** (`AnnouncementsCreateScreen`) |
| Thống kê Phụ huynh đã đọc (GV) | Hiển thị tỷ lệ phụ huynh đã đọc | ❌ Bỏ thống kê này ở cả Mobile và Web |
| Web Giáo viên | Chưa có phân hệ Web | ✅ Xây dựng phân hệ Web Giáo viên tích hợp trong `admin-web` (login `/teacher/login`, đầy đủ tính năng trừ QL Học phí) |
| Số nút Home PH | 3 nút | 1 nút (Bảng điểm) |
| Số nút Home HS | 3 nút | 1 nút (Bảng điểm) |
| Số nút Home GV | 4 nút (Phân công, Nhận xét, Theo dõi, QL Học phí) | 2 nút (Phân công, Nhận xét định kỳ) |

---

## Verification

### Build & Test
```bash
flutter analyze
flutter test test/grade_assessment_flow_test.dart
flutter test test/periodic_review_flow_test.dart
# Chạy build Web kiểm tra không lỗi
npm --prefix admin-web run build
```

### Manual Check
- [ ] PH: Nhấn "Bảng điểm" → thấy bảng tổng kết + bảng điểm tất cả môn
- [ ] Bảng tổng kết: danh hiệu, ĐTB, hạnh kiểm, học lực, xếp hạng, **NX GVCN**
- [ ] Bảng điểm: tất cả môn + cuộn ngang + **cột Nhận xét GV** cuối
- [ ] Nhấn vào ô NX dài → xem đầy đủ trong dialog
- [ ] Môn Đạt/Chưa đạt hiện "Đ" thay vì số
- [ ] Home PH: không còn nút "Nhận xét HK", "Lịch họp"
- [ ] Home HS: không còn nút "Nhận xét HK", "KT & VP"
- [ ] PH/HS: **KHÔNG** thấy vi phạm ở bất kỳ đâu
- [ ] Home GV (Mobile): **KHÔNG** còn nút "Theo dõi học sinh" và **KHÔNG** còn nút "QL Học phí"
- [ ] GV (Mobile & Web): **KHÔNG** hiển thị tỷ lệ phụ huynh đã đọc thông báo trong Hồ sơ lớp chủ nhiệm.
- [ ] GV: Gửi thông báo không còn ô check "Yêu cầu xác nhận hoặc phản hồi"
- [ ] GV: Màn hình Thông báo ở AppShell hiển thị thông báo đã nhận (thay vì đã gửi)
- [ ] GV: Danh sách thông báo đã gửi được hiển thị bên dưới form tạo thông báo ở màn hình "Gửi thông báo lớp"
- [ ] Admin: Trang Quản lý kết quả hoạt động với đầy đủ các mục (Quản lý điểm & nhận xét môn, Vi phạm, Chuyên cần).
- [ ] Admin: Cho phép công bố điểm môn học linh hoạt theo ngày (phụ huynh/học sinh xem được điểm thành phần ngay).
- [ ] Admin: Bảng tổng kết học kỳ tự động tính GPA, xếp loại, gợi ý hạnh kiểm dựa trên vi phạm/chuyên cần và cho phép override trước khi công bố kết quả cuối kỳ.
- [ ] Web Giáo viên: Truy cập `/teacher/login` đăng nhập được bằng tài khoản GV, vào Dashboard và sử dụng đầy đủ các tính năng (Nhập điểm, Nhận xét định kỳ, Gửi thông báo, QL Đơn nghỉ, Chat - không có trang QL Học phí).
- [ ] Dự án: Đã xóa hoàn toàn các file `homeroom_monitoring_screen.dart`, `student_engagement_screen.dart`, và `teacher_tuition_screen.dart`
- [ ] GV: luồng nhập nhận xét bộ môn & Submit nhận xét chủ nhiệm + nhập vi phạm vẫn hoạt động bình thường
