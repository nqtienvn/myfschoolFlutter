# MyFschool — Implementation Spec từ Phase 4 đến Phase 6

Tài liệu này là bản handoff để tiếp tục phát triển MyFschool ở một task Codex khác.

## 0. Trạng thái bàn giao

- Repository: `C:\DevFlutter\practice\myfschoolse1913`
- Branch bắt buộc: `master`
- Baseline đã push: `ad1cdc5`
- Phase 1: đã hoàn thành phân quyền và cô lập dữ liệu lớp/năm học.
- Phase 2: đã hoàn thành hồ sơ lớp GVCN, roster, xếp hạng và chi tiết học sinh.
- Phase 3: đã hoàn thành `SCORE/PASS_FAIL/COMMENT` và lịch sử sửa điểm danh.
- Không làm lại Phase 1–3.

### Quy tắc triển khai bắt buộc

1. Làm theo chuỗi `Backend → DTO/API client → Admin Web/Flutter → Test`.
2. Không dựng dữ liệu hoặc số liệu tĩnh trên giao diện.
3. Mọi dữ liệu nghiệp vụ phải kiểm tra `academicYearId`, `semesterId`, lớp và quyền người dùng.
4. Flutter không truy cập database trực tiếp.
5. Admin Web phải dùng năm học đang chọn từ application context.
6. Hoàn thành một phase phải test, tự review, commit/push `master` và dừng.
7. Chỉ sang phase tiếp theo khi người dùng nhập `phase ok sang phase X`.

---

# Phase 4 — Phát hành và đọc thông báo

> Phần thiết kế xác nhận/phản hồi cũ đã bị hủy bỏ. Không triển khai lại API, field, badge hoặc UI liên quan đến xác nhận/phản hồi.

## Luồng hiện hành

```text
Giáo viên soạn theo lớp → backend chuẩn hóa và kiểm tra chính sách năm học
→ hợp lệ: `PUBLISHED`, tạo snapshot người nhận ngay → PH/HS đọc thông báo
→ vi phạm: `SYSTEM_REJECTED`, lưu quy tắc khớp và trả kết quả ngay → không tạo người nhận

Admin soạn → phát hành ngay → toàn bộ tài khoản không phải Admin đọc thông báo
```

## Quy tắc

- Thông báo giáo viên chỉ có `PUBLISHED` hoặc `SYSTEM_REJECTED`; không có hàng đợi duyệt thủ công.
- Admin cấu hình nhiều câu từ độc lập theo năm học bằng `scope` (`TITLE`, `BODY`, `ALL`) và `matchType` (`CONTAINS`, `EXACT`).
- Chuẩn hóa luôn bật: Unicode NFKC, chữ thường, bỏ ký tự ẩn, đổi dấu câu/khoảng trắng về một khoảng trắng, giữ dấu tiếng Việt và khớp theo ranh giới từ.
- Thông báo bị từ chối lưu lịch sử và có thể sửa/gửi lại thành bản ghi mới; bản cũ không bị ghi đè.
- Admin gửi trực tiếp với `recipientScope=SCHOOL`, `targetRole=ALL`.
- `announcement_reads` chỉ lưu snapshot người nhận và `read_at`.
- Người nhận chỉ có trạng thái `UNREAD` hoặc `READ`.
- Admin không có drawer/màn theo dõi danh sách người nhận.
- Giáo viên chỉ được xem đọc/chưa đọc của thông báo do chính mình gửi.
- Không có xác nhận, phản hồi hoặc hành động bắt buộc.

## API

```text
POST /api/announcements                         # Giáo viên kiểm tra và gửi ngay
GET  /api/announcements/admin                   # Admin xem danh sách phân trang
GET  /api/announcements/admin/summary           # Tổng hợp theo hai kết quả
GET  /api/announcements/admin/policy            # Đọc chính sách theo năm học
PUT  /api/announcements/admin/policy            # Thay toàn bộ cấu hình nhiều câu từ
POST /api/announcements/admin/broadcast         # Admin phát hành toàn hệ thống
PUT  /api/announcements/{id}/read               # Người nhận đánh dấu đã đọc
GET  /api/announcements/{id}/recipients         # Chỉ giáo viên gửi; UNREAD/READ
```

## Test bắt buộc

- Nội dung hợp lệ phát hành ngay và sinh đúng snapshot người nhận.
- Nội dung vi phạm được lưu `SYSTEM_REJECTED`, có chi tiết quy tắc khớp và không sinh snapshot/notification.
- Chính sách và danh sách không rò dữ liệu giữa hai năm học.
- Admin broadcast xuất hiện trong danh sách phân trang của năm học đã chọn.
- Người ngoài lớp không đọc được thông báo giáo viên.
- Giáo viên khác không xem được trạng thái người nhận.
- Admin broadcast tạo snapshot cho mọi tài khoản không phải Admin.
- Admin không truy cập API danh sách người nhận.
- Mở nhiều lần vẫn chỉ ghi nhận một lượt đọc.

---

# Phase 5 — Vi phạm học sinh và kết quả học kỳ

## Mục tiêu

Tách rõ trách nhiệm ghi nhận vi phạm và quản lý kết quả:

```text
GVCN ghi vi phạm → lưu nháp → submit
→ Admin chỉ xem và thống kê vi phạm đã submit
```

## Database

Sử dụng bảng `student_events` với `event_type = VIOLATION`:

```text
academic_year_id
semester_id
class_id
student_id
event_type: VIOLATION
category
title
description
event_date
status: DRAFT | SUBMITTED
created_by
submitted_at
created_at
updated_at
```

Migration `V29` xóa hoàn toàn ba bảng nhận xét định kỳ cũ.

## Quy tắc nghiệp vụ

GVCN:

- Chỉ ghi vi phạm cho học sinh đang thuộc lớp chủ nhiệm trong đúng năm học và học kỳ.
- Có thể thêm, sửa, xóa vi phạm khi còn `DRAFT`.
- Có thể submit theo từng học sinh hoặc cả lớp.
- Vi phạm `SUBMITTED` bị khóa, không thể sửa hoặc xóa.

Admin:

- Chỉ đọc và thống kê vi phạm `SUBMITTED`.
- Không được tạo, sửa, xóa hoặc submit thay GVCN.
- Kết quả học kỳ không phụ thuộc vào nhận xét giáo viên.

Phụ huynh/học sinh:

- Không truy cập luồng vi phạm nội bộ này.

## API

```text
GET    /api/students/{studentId}/events
POST   /api/students/{studentId}/events
PUT    /api/student-events/{id}
DELETE /api/student-events/{id}
POST   /api/students/{studentId}/violations/submit
POST   /api/student-events/violations/submit-class
```

Mọi API ghi dữ liệu phải kiểm tra chéo `academicYearId`, `semesterId`, `classId`, `studentId` và phân công chủ nhiệm.

## Flutter App

- Không có API client, model hoặc màn hình nhận xét định kỳ.
- Bảng điểm chỉ hiển thị điểm và kết quả tổng kết đã công bố.

## Teacher Web

- Chỉ GVCN dùng trang “Vi phạm học sinh”.
- Chọn học sinh để thêm/sửa/xóa bản nháp và submit.
- Không có luồng nhận xét của GVCN hoặc GVBM.

## Admin Web

- Chỉ hiển thị danh sách và số liệu vi phạm đã submit theo phạm vi năm học.
- Không có form hoặc API ghi vi phạm.
- Không có màn hình theo dõi nhận xét định kỳ.

## Test bắt buộc

- GVCN không ghi vi phạm ngoài lớp chủ nhiệm hoặc chéo năm học.
- Admin không tạo, sửa hoặc xóa vi phạm.
- Admin không nhìn thấy bản nháp.
- Submit chuyển bản nháp thành `SUBMITTED` và khóa chỉnh sửa.
- PH/HS không truy cập API vi phạm nội bộ.
- Cô lập dữ liệu giữa hai năm học.

## Definition of Done

Không còn entity, repository, service, controller, API client, route hoặc giao diện nhận xét định kỳ. GVCN là chủ thể duy nhất ghi và submit vi phạm; Admin chỉ thống kê dữ liệu đã submit.

---

# Phase 6 — Hồ sơ học sinh nâng cao và báo cáo GVCN

Phase này là phase cuối, triển khai nội bộ theo thứ tự `6A → 6B → 6C`.

## 6A. Cảnh báo học sinh có nguy cơ

Nguồn dữ liệu:

- GPA và điểm môn.
- Chuyên cần.
- Số buổi vắng không phép.
- Kết quả rèn luyện đã công bố.
- Học phí nếu được chọn làm tiêu chí.

Bảng `student_risk_flags`:

```text
academic_year_id
semester_id
class_id
student_id
risk_type
severity
metric_value
threshold_value
message
status: OPEN | ACKNOWLEDGED | RESOLVED
detected_at
acknowledged_by
resolved_by
resolved_at
source_snapshot_json
```

Yêu cầu:

- Quy tắc cảnh báo cấu hình theo năm học, không hardcode trên UI.
- Tính lại sau khi công bố điểm, sửa điểm danh và theo scheduled job.
- Job phải idempotent, không sinh cảnh báo trùng.
- GVCN chỉ thấy lớp chủ nhiệm.
- PH/HS không thấy cảnh báo nội bộ.

## 6B. Nhật ký liên hệ và họp phụ huynh

Bảng `parent_contact_logs`:

```text
student_id
academic_year_id
semester_id
class_id
contact_type: CALL | MESSAGE | MEETING | OTHER
subject
summary
result
contacted_at
next_action_at
created_by
```

Bảng `parent_meetings` và `parent_meeting_participants`:

```text
title
academic_year_id
semester_id
class_id
student_id nullable
starts_at
location
agenda
status: SCHEDULED | COMPLETED | CANCELLED
guardian_id
response: PENDING | ACCEPTED | DECLINED
attendance: UNKNOWN | ATTENDED | ABSENT
responded_at
```

Luồng:

- GVCN tạo lịch hẹn/họp.
- Phụ huynh nhận notification realtime.
- Phụ huynh xác nhận tham gia hoặc từ chối.
- GVCN ghi nhận kết quả và trạng thái tham dự.

## 6C. Khen thưởng, vi phạm và báo cáo lớp

Bảng `student_events`:

```text
student_id
academic_year_id
semester_id
class_id
event_type: REWARD | VIOLATION | NOTE
category
title
description
event_date
status: DRAFT | PUBLISHED
created_by
published_at
```

Không xây hệ thống điểm phạt phức tạp trong phiên bản này.

Báo cáo lớp phải được backend tổng hợp, gồm:

- Sĩ số.
- Tỷ lệ chuyên cần.
- Học sinh nguy cơ.
- GPA trung bình.
- Phân bố học lực.
- Phân bố hạnh kiểm.
- Tiến độ nhận xét.
- Số liên hệ phụ huynh.
- Số lịch họp và tỷ lệ tham gia.
- Khen thưởng/vi phạm.

## API đề xuất

```text
GET /api/homeroom/risks
PUT /api/homeroom/risks/{id}/acknowledge
PUT /api/homeroom/risks/{id}/resolve

GET  /api/students/{studentId}/contact-logs
POST /api/students/{studentId}/contact-logs
PUT  /api/contact-logs/{id}
DELETE /api/contact-logs/{id}

GET  /api/parent-meetings
POST /api/parent-meetings
PUT  /api/parent-meetings/{id}
PUT  /api/parent-meetings/{id}/respond
PUT  /api/parent-meetings/{id}/attendance

GET  /api/students/{studentId}/events
POST /api/students/{studentId}/events
PUT  /api/student-events/{id}
POST /api/student-events/{id}/publish

GET /api/homeroom/reports/class-summary
```

## Giao diện

GVCN App:

- Dashboard cảnh báo.
- Hồ sơ học sinh gồm các tab:
  - Tổng quan.
  - Kết quả/nhận xét.
  - Liên hệ phụ huynh.
  - Lịch hẹn.
  - Khen thưởng/vi phạm.
- Báo cáo tổng hợp lớp theo học kỳ.

Phụ huynh App:

- Xem lịch họp được mời.
- Xác nhận tham gia.
- Xem khen thưởng/vi phạm đã công bố.
- Không thấy ghi chú nội bộ hoặc risk flag.

Admin Web:

- Cấu hình ngưỡng cảnh báo theo năm học.
- Xem báo cáo tổng hợp theo khối/lớp.
- Drill-down tới học sinh.
- Xuất CSV có thể thực hiện nếu không mở rộng quá phạm vi; dữ liệu vẫn phải lấy từ backend.

## Test bắt buộc

- Không sinh risk flag trùng.
- Sửa điểm hoặc điểm danh làm cảnh báo được tính lại đúng.
- GVCN không xem hồ sơ lớp khác.
- GVBM không truy cập nhật ký liên hệ nội bộ.
- PH chỉ phản hồi lịch họp của chính mình.
- Dữ liệu năm học A không xuất hiện trong báo cáo năm học B.
- Học sinh chuyển lớp vẫn giữ lịch sử cũ đúng lớp/năm.
- Báo cáo backend khớp dữ liệu nguồn.
- Flutter responsive tại 320/360/390/430 px.
- Admin test và production build.

## Definition of Done cuối dự án

Chạy đầy đủ:

```bash
cd backend
mvn test

cd ..
flutter analyze
flutter test
flutter build web

cd admin-web
npm test
npm run build

git diff --check
```

Sau đó:

- Tự review cả Admin, GVCN, GVBM, Phụ huynh và Học sinh.
- Xác nhận không còn dữ liệu tĩnh trong các chức năng mới.
- Commit/push `master`.
- Báo cáo endpoint, migration, test và giới hạn còn lại.
- Dừng hoàn toàn.

Commit gợi ý:

```text
feat: complete homeroom student monitoring and reports
```

---

# Câu lệnh dùng để bắt đầu ở task mới

```text
Đọc file docs/phase-4-to-6-implementation-spec.md và bắt đầu Phase 4.
Kiểm tra backend và database trước, sau đó mới map lên Admin Web và Flutter.
Xong Phase 4 phải chạy test, tự review, commit/push master rồi dừng;
chưa được làm Phase 5 cho đến khi tôi nhập “phase ok sang phase 5”.
```
