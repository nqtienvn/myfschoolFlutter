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

# Phase 5 — Nhận xét định kỳ và hạnh kiểm

## Mục tiêu

Hoàn thiện luồng sổ liên lạc:

```text
GVBM nhận xét môn → gửi GVCN
→ GVCN tổng hợp → nhập nhận xét chung và hạnh kiểm
→ công bố → PH/HS xem
```

## Database

Migration dự kiến `V20`.

Bảng `subject_student_reviews`:

```text
id
academic_year_id
semester_id
class_id
student_id
subject_id
subject_teacher_id
comment
strengths
improvements
status: DRAFT | SUBMITTED | RETURNED
return_reason
submitted_at
created_at
updated_at
```

Unique:

```text
student_id + subject_id + semester_id
```

Bảng `student_periodic_reports`:

```text
id
academic_year_id
semester_id
class_id
student_id
homeroom_teacher_id
general_comment
conduct
status: DRAFT | PUBLISHED
published_at
created_at
updated_at
```

Unique:

```text
student_id + semester_id
```

Bảng audit `student_review_audits`:

```text
entity_type
entity_id
old_value_json
new_value_json
changed_by
reason
changed_at
```

## Quy tắc nghiệp vụ

GVBM:

- Chỉ nhận xét học sinh thuộc lớp/môn được phân công.
- Có thể lưu nháp nhiều lần.
- Gửi GVCN khi hoàn thành.
- Sau `SUBMITTED` không được sửa nếu GVCN chưa trả lại.
- GVCN trả lại phải nhập lý do.

GVCN:

- Chỉ xem nhận xét các môn của lớp chủ nhiệm.
- Thấy môn nào chưa gửi.
- Tổng hợp nhận xét từng học sinh.
- Nhập nhận xét chung và hạnh kiểm cuối kỳ.
- Chỉ công bố khi các trường bắt buộc đã đủ.
- Có thể công bố riêng từng học sinh hoặc cả lớp.

Phụ huynh/học sinh:

- Chỉ thấy báo cáo `PUBLISHED`.
- Không thấy bản nháp hoặc nhận xét nội bộ bị trả lại.

Admin:

- Xem toàn bộ trong năm học đang chọn.
- Có thể mở lại báo cáo đã công bố, nhưng bắt buộc ghi lý do và audit.

## Tích hợp `semester_results`

- `semester_results.conduct` là hạnh kiểm chính thức sau công bố.
- Giá trị tính tự động từ chuyên cần chỉ là `suggestedConduct`.
- Khi GVCN công bố, ghi `conductSource=HOMEROOM`.
- Những lần tính lại GPA/chuyên cần không được ghi đè hạnh kiểm đã được GVCN công bố.

## API đề xuất

```text
GET  /api/subject-reviews/assignments
GET  /api/subject-reviews?classId=&subjectId=&semesterId=
PUT  /api/subject-reviews/{studentId}
POST /api/subject-reviews/submit

GET  /api/homeroom-reports?classId=&semesterId=
GET  /api/homeroom-reports/students/{studentId}
PUT  /api/homeroom-reports/students/{studentId}
PUT  /api/subject-reviews/{id}/return
POST /api/homeroom-reports/students/{studentId}/publish
POST /api/homeroom-reports/publish-class

GET  /api/periodic-reports/students/{studentId}
```

Mọi API phải nhận hoặc suy ra và kiểm tra chéo:

```text
academicYearId
semesterId
classId
studentId
subjectId
teacher assignment
homeroom assignment
```

## Flutter App

GVBM:

- Chọn lớp, môn, học kỳ.
- Danh sách học sinh và ô nhận xét.
- Lưu nháp/gửi GVCN.
- Hiển thị trạng thái và lý do bị trả lại.

GVCN:

- Dashboard tiến độ nhận xét theo môn.
- Danh sách học sinh.
- Màn tổng hợp nhận xét từng em.
- Nhập nhận xét chung, hạnh kiểm.
- Công bố một học sinh hoặc cả lớp.

Phụ huynh/học sinh:

- Mục “Nhận xét học kỳ”.
- Hiển thị nhận xét từng môn, nhận xét GVCN và hạnh kiểm.
- Chỉ dùng dữ liệu đã công bố.

## Admin Web

- Theo dõi tiến độ nhận xét theo lớp/môn.
- Xem báo cáo học sinh.
- Bộ lọc năm học, học kỳ, lớp và trạng thái.
- Chức năng mở lại báo cáo phải yêu cầu lý do.

## Test bắt buộc

- GVBM không nhận xét môn/lớp ngoài phân công.
- GVBM không sửa sau khi đã submit.
- GVCN chỉ xem lớp chủ nhiệm.
- GVCN trả lại phải có lý do.
- PH/HS không xem được bản nháp.
- Công bố cập nhật đúng `semester_results.conduct`.
- Tính lại GPA không ghi đè hạnh kiểm chính thức.
- Audit đủ old/new/actor/time/reason.
- Cô lập hai năm học và trường hợp học sinh chuyển lớp.

## Definition of Done

Toàn bộ dữ liệu đi từ GVBM đến GVCN rồi mới công bố; không dùng nhận xét mock hoặc hạnh kiểm tĩnh.

Commit gợi ý:

```text
feat: add periodic student reviews and conduct workflow
```

Sau khi hoàn thành phải dừng và chờ: `phase ok sang phase 6`.

---

# Phase 6 — Hồ sơ học sinh nâng cao và báo cáo GVCN

Phase này là phase cuối, triển khai nội bộ theo thứ tự `6A → 6B → 6C`.

## 6A. Cảnh báo học sinh có nguy cơ

Nguồn dữ liệu:

- GPA và điểm môn.
- Chuyên cần.
- Số buổi vắng không phép.
- Nhận xét định kỳ.
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
