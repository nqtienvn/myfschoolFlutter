# Luồng quản lý điểm động

## Nguồn cấu hình

Năm học chỉ được tạo khi request có `gradeConfigTemplateId` hoặc danh sách `gradeConfigItems` hợp lệ. Cấu hình được sao chép thành snapshot trong `academic_year_grade_configs`; thay đổi mẫu sau này không làm đổi dữ liệu lịch sử.

Mỗi đầu điểm có mã, tên, hệ số, số lượng, loại đánh giá, thứ tự, tính bắt buộc và quyền nhập. Mặc định nghiệp vụ là điểm thường xuyên hệ số 1 do giáo viên bộ môn nhập; giữa kỳ hệ số 2 và cuối kỳ hệ số 3 do admin nhập.

## Phân quyền và phạm vi

- Mọi GradeBook thuộc đúng một lớp, môn và học kỳ. Backend xác thực lớp, môn và học kỳ cùng năm học.
- Giáo viên phải có phân công `ACTIVE` đúng lớp/môn và chỉ nhập đầu điểm `SUBJECT_TEACHER` hoặc `SUBJECT_TEACHER_AND_ADMIN`.
- Admin chỉ nhập đầu điểm `ADMIN` hoặc `SUBJECT_TEACHER_AND_ADMIN`.
- Học sinh chỉ xem chính mình; phụ huynh chỉ xem học sinh có liên kết giám hộ.
- Phụ huynh/học sinh chỉ nhận bảng điểm `PUBLISHED` hoặc `LOCKED`.

## Tính điểm và trạng thái

Điểm trung bình môn học kỳ được tính bằng `sum(score * weight) / sum(weight)`, làm tròn `HALF_UP` một chữ số ở kết quả. Điểm trống không được coi là 0. Không thể công bố hoặc khóa khi thiếu đầu điểm bắt buộc.

Trạng thái GradeBook: `DRAFT -> SUBMITTED -> PUBLISHED -> LOCKED`. Mọi thay đổi điểm được ghi vào `student_score_audits` với người sửa, điểm cũ, điểm mới, lý do và thời gian.

## API chính

- `GET/POST /api/grade-configurations/templates`
- `GET /api/grade-configurations/academic-years/{yearId}`
- `GET /api/grade-books?classId=&subjectId=&semesterId=`
- `PUT /api/grade-books/scores`
- `POST /api/grade-books/{id}/status/{status}`
- `GET /api/transcripts/students/{studentId}?academicYearId=&semesterId=`

Migration thay thế dữ liệu cố định: `V14__replace_legacy_grades_with_configured_gradebooks.sql`.
