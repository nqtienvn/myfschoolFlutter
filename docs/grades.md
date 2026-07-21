# Luồng quản lý điểm động

## Nguồn cấu hình

Năm học chỉ được tạo khi request có `gradeConfigTemplateId` hoặc danh sách `gradeConfigItems` hợp lệ. Cấu hình được sao chép thành snapshot trong `academic_year_grade_configs`; thay đổi mẫu sau này không làm đổi dữ liệu lịch sử.

Mỗi đầu điểm có mã, tên, hệ số, số lượng, loại đánh giá, thứ tự, tính bắt buộc và quyền nhập. Mặc định nghiệp vụ là điểm thường xuyên hệ số 1 do giáo viên bộ môn nhập; giữa kỳ hệ số 2 và cuối kỳ hệ số 3 do admin nhập.

## Phân quyền và phạm vi

- Mọi GradeBook thuộc đúng một lớp, môn và học kỳ. Backend xác thực lớp, môn và học kỳ cùng năm học.
- Giáo viên phải có phân công `ACTIVE` đúng lớp/môn và chỉ nhập đầu điểm `SUBJECT_TEACHER` hoặc `SUBJECT_TEACHER_AND_ADMIN`.
- Admin chỉ nhập đầu điểm `ADMIN` hoặc `SUBJECT_TEACHER_AND_ADMIN`.
- Học sinh chỉ xem chính mình; phụ huynh chỉ xem học sinh có liên kết giám hộ.
- `PUT /api/grade-books/scores` là thao tác submit: điểm hợp lệ được công bố ngay cho phụ huynh/học sinh trong cùng giao dịch, không qua bước duyệt của admin.
- Admin không công bố điểm thay giáo viên; admin chỉ được nhập các đầu điểm thuộc quyền mình và khóa sổ khi dữ liệu bắt buộc đã đủ.
- Transcript luôn dựng đủ danh sách môn áp dụng và toàn bộ đầu điểm từ cấu hình năm học. Môn hoặc đầu điểm chưa có dữ liệu vẫn có trong response với giá trị trống.

## Tính điểm và trạng thái

Điểm trung bình môn học kỳ được tính bằng `sum(score * weight) / sum(weight)`, làm tròn `HALF_UP` một chữ số ở kết quả. Khi nhập trực tiếp, ô chưa nhập vẫn là dữ liệu thiếu và được hiển thị trống trên ứng dụng. Riêng luồng import Excel của admin chuẩn hóa ô điểm số để trống thành `0` theo nghiệp vụ đối soát; giao diện quản lý kết quả cũng hiển thị ô chưa có dữ liệu là `0` nhưng không âm thầm ghi dữ liệu vào cơ sở dữ liệu.

Trạng thái GradeBook: `DRAFT -> PUBLISHED -> LOCKED`. Submit đầu điểm chuyển sổ sang `PUBLISHED`; admin chỉ có thể chuyển sang `LOCKED` sau khi mọi đầu điểm bắt buộc đã đủ. Mọi thay đổi điểm được ghi vào `student_score_audits` với người sửa, điểm cũ, điểm mới, lý do và thời gian.

## API chính

- `GET/POST /api/grade-configurations/templates`
- `GET /api/grade-configurations/academic-years/{yearId}`
- `GET /api/grade-books?classId=&subjectId=&semesterId=`
- `PUT /api/grade-books/scores`
- `POST /api/grade-books/{id}/status/LOCKED`
- `GET /api/transcripts/students/{studentId}?academicYearId=&semesterId=`
- `GET /api/result-files/template?academicYearId=&semesterId=&classId=&subjectId=`
- `POST /api/result-files/import?academicYearId=&semesterId=&classId=&subjectId=`
- `GET /api/result-files/export?academicYearId=&semesterId=&classId=`
- `POST /api/semester-results/admin/close`
- `GET/POST /api/semester-results/admin/annual`
- `POST /api/semester-results/admin/annual/calculate`
- `POST /api/semester-results/admin/annual/publish`

## Quản lý kết quả học kỳ và năm học

- Template Excel được sinh động từ snapshot cấu hình đầu điểm của đúng năm học. File có metadata và dấu vân tay cấu hình; import sai năm, học kỳ, lớp, môn hoặc dùng template cũ sẽ bị từ chối toàn bộ.
- Admin chỉ import các đầu điểm có quyền `ADMIN` hoặc `SUBJECT_TEACHER_AND_ADMIN`. Import là giao dịch nguyên tử: có bất kỳ dòng không hợp lệ thì không dòng nào được lưu.
- Export tạo ba sheet: điểm thành phần, tổng kết học kỳ và tổng kết năm học.
- Xếp mức học tập dùng kết quả từng môn theo Thông tư 22/2021/TT-BGDĐT: `Tốt`, `Khá`, `Đạt`, `Chưa đạt`; GPA chỉ là chỉ số tham khảo. Kết quả cả năm của từng môn dùng `(HK1 + 2 × HK2) / 3`.
- Ngưỡng gợi ý rèn luyện nội bộ được chuẩn hóa không chồng lấn: `Tốt` khi 0 vi phạm và tối đa 2 buổi nghỉ không phép; `Khá` khi tối đa 1 vi phạm và 4 buổi; `Đạt` khi tối đa 2 vi phạm và 9 buổi; còn lại là `Chưa đạt`. Admin vẫn phải đối soát trước khi công bố.
- Chỉ được đóng học kỳ khi tất cả lớp có sổ điểm đủ môn, kết quả học kỳ đã công bố và các sổ điểm không còn ở trạng thái nháp. Đóng học kỳ sẽ khóa mọi sổ điểm và chặn sửa/import.
- Chỉ được tính tổng kết năm khi cả hai học kỳ đã `COMPLETED`. Chỉ được đóng năm học khi toàn bộ học sinh đang học đã có kết quả năm được công bố.
- Công bố kết quả tạo thông báo cho học sinh và phụ huynh có liên kết giám hộ.

Migration thay thế dữ liệu cố định: `V14__replace_legacy_grades_with_configured_gradebooks.sql`.
Migration kết quả năm học: `V27__academic_year_results.sql`.
