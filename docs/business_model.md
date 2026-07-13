# Tài liệu Nghiệp vụ & Thiết kế Hệ thống MyFschool

Tài liệu này mô tả chi tiết về nghiệp vụ, cấu trúc dữ liệu, các tác nhân hệ thống (actor), quy tắc nghiệp vụ và các tính năng đề xuất mở rộng cho dự án **MyFschool (FPT Schools)**.

---

## 1. Nghiệp vụ Tổng quan của Dự án (Domain & Purpose)
**MyFschool** là ứng dụng **Sổ liên lạc điện tử thông minh** và **Cổng thông tin tương tác** giữa Nhà trường và Gia đình. 
Khác với các ứng dụng truyền thống phụ thuộc nặng nề vào các tác vụ thủ công, MyFschool tập trung vào:
* **Tối ưu hóa UI/UX Client-Driven:** Toàn bộ nghiệp vụ được phản ánh và mô phỏng trực quan thông qua thiết kế giao diện động (Client-Side Simulation). Dữ liệu nghiệp vụ được tổ chức dạng Snapshot giúp phản ứng tức thì với các thao tác của người dùng.
* **Số hóa các quy trình hành chính:** Điểm danh lớp học, xin nghỉ phép, nộp đơn từ trực tuyến, nhập điểm học sinh và truyền thông nội bộ.
* **Kết nối đa luồng:** Giao tiếp trực tiếp giữa Phụ huynh, Học sinh và Giáo viên thông qua luồng nhắn tin, thông báo và hoạt động lớp học.

---

## 2. Thiết kế Kiến trúc Giao diện & Nghiệp vụ tại UI (Client-Side Business Logic)
Toàn bộ logic nghiệp vụ của ứng dụng được mô phỏng trực tiếp trên giao diện Client (Flutter Dart) bằng việc quản lý trạng thái local và mô hình dữ liệu tĩnh (`mockStudents` tại [student_models.dart](file:///c:/DevFlutter/practice/myfschoolse1913/lib/vn/edu/fpt/view/screens/student_models.dart)):
* **State Mutation (Biến đổi trạng thái nội bộ):** Các thao tác chuyển đổi học sinh (switcher) của phụ huynh, duyệt đơn nghỉ phép của giáo viên, hoặc xem chi tiết môn học đều thay đổi trạng thái UI ngay lập tức.
* **Fixed Header Layout (Cố định thanh tiêu đề):** Để tăng tính chuyên nghiệp, toàn bộ các màn hình chính (Phụ huynh, Học sinh, Giáo viên, Tác vụ) đều áp dụng cấu trúc Fixed Header (cố định `SharedHeader` hoặc Title Row phía trên) và cuộn nội dung bên dưới trong `Expanded(child: ListView(...))` để tiêu đề không bị cuộn đi mất.
* **Persistent Navigation Shell (Thanh điều hướng cố định):** Sử dụng `IndexedStack` và các `Navigator` lồng nhau (Nested Navigation) trong [app_shell.dart](file:///c:/DevFlutter/practice/myfschoolse1913/lib/vn/edu/fpt/view/screens/app_shell.dart) để giữ nguyên thanh điều hướng dưới cùng khi chuyển sang các trang con, đồng thời lưu giữ vị trí cuộn và trạng thái của từng tab.

---

## 3. Các Actor & Cấu trúc Tiện ích trên UI

### Tác nhân 1: Phụ huynh (Parent)
* **Giao diện trang chủ:** Áp dụng Fixed Header hiển thị thông tin học sinh và thông báo chưa đọc. Ngay bên dưới là thanh cuộn ngang hỗ trợ chuyển đổi nhanh (Student Switcher) giữa các con (ví dụ: `Nguyễn Minh An` và `Nguyễn Minh Bảo`).
* **Lưới tiện ích tinh gọn (2x2 Grid Layout):** Để tối ưu trải nghiệm và loại bỏ các thành phần rườm rà, lưới tiện ích của phụ huynh chỉ tập trung vào 4 tính năng cốt lõi:
  1. **Thời khóa biểu:** Xem lịch học và lịch thi hàng ngày của con.
  2. **Bảng điểm:** Xem điểm chi tiết từng môn học (Toán, Anh, Tin) kèm nhận xét và xu hướng tăng trưởng.
  3. **Chuyên cần:** Xem chi tiết lịch sử đi học (Có mặt, Vắng có phép/không phép).
  4. **Đơn từ:** Tạo đơn nghỉ phép trực tuyến cho con và theo dõi trạng thái phê duyệt.
* *(Ghi chú: Đã loại bỏ hoàn toàn các tiện ích "Sự kiện & CLB" và "Dịch vụ khác" để phụ huynh tập trung vào giám sát học tập).*

### Tác nhân 2: Học sinh (Student)
* **Giao diện trang chủ:** Fixed Header hiển thị nhận diện hệ thống và biểu tượng thông báo. Bên dưới là thẻ thông tin học sinh (Green Card) trực quan và lưới tiện ích cá nhân.
* **Lưới tiện ích học tập (2x2 Grid Layout):** Tập trung vào trải nghiệm học tập và sinh hoạt cá nhân của học sinh lớp 12:
  1. **Thời khóa biểu:** Lịch học và phòng học cụ thể.
  2. **Bảng điểm:** Theo dõi điểm số cá nhân để chủ động cải thiện.
  3. **Chuyên cần:** Tự giám sát tình trạng có mặt hoặc vắng của bản thân.
  4. **Sự kiện & CLB:** Xem và đăng ký tham gia các hoạt động ngoại khóa, câu lạc bộ trong trường.
* *(Ghi chú: Đã loại bỏ nút "Dịch vụ khác" để giao diện đạt sự cân đối tuyệt đối 2x2).*

### Tác nhân 3: Giáo viên (Teacher / GVCN)
* **Giao diện trang chủ:** Fixed Header hiển thị vai trò chủ nhiệm và luồng inbox. Thẻ cam (Orange Card) tóm tắt lớp giảng dạy và môn phụ trách.
* **Giao diện tiện ích điều hành (3x2 Grid Layout):** Hỗ trợ 6 tác vụ giảng dạy thiết thực:
  1. **Lớp được phân công:** Xem danh sách học sinh và thông tin liên hệ.
  2. **Điểm danh lớp:** Điểm danh theo từng buổi sáng/chiều có lịch học. Giáo viên chỉ điểm danh trong ngày; sửa sau khi đã lưu phải gửi Admin phê duyệt.
  3. **Duyệt đơn xin nghỉ:** Phê duyệt hoặc Từ chối đơn xin nghỉ phép gửi từ Phụ huynh.
  4. **Nhập & Upload điểm:** Nhập điểm trực tiếp hoặc import hàng loạt qua tệp Excel.
  5. **Gửi thông báo lớp:** Soạn thông báo kèm cờ bắt buộc xác nhận từ phụ huynh.
  6. **Thống kê lớp học:** Biểu đồ chuyên cần và học lực của cả lớp.
* **Hộp thoại nhắn tin hợp nhất (Unified Messaging Inbox):** 
  * Đã loại bỏ hoàn toàn nút "Tin nhắn phụ huynh" trên lưới tiện ích.
  * Tích hợp toàn bộ luồng chat và danh sách hộp thoại cần phản hồi của Phụ huynh trực tiếp vào tab **"Tin nhắn"** ở thanh điều hướng dưới cùng của Giáo viên (thông qua [TeacherInboxScreen](file:///c:/DevFlutter/practice/myfschoolse1913/lib/vn/edu/fpt/view/screens/teacher_inbox_screen.dart)).

---

## 4. Các Quy tắc Nghiệp vụ Đặc thù tại Client
* **Quy tắc liên kết đơn phép và điểm danh:** Khi giáo viên bấm **Duyệt (Approved)** đơn nghỉ phép của học sinh trên UI, trạng thái điểm danh ngày tương ứng trong phần chuyên cần sẽ tự động chuyển thành `Vắng có phép` kèm mã đơn liên kết.
* **Quy tắc phân quyền dữ liệu (RBAC) trên UI:**
  * Phụ huynh chỉ được xem dữ liệu các học sinh thuộc danh sách con đã liên kết (`mockStudents[0]` và `mockStudents[1]`).
  * Học sinh chỉ hiển thị thông tin cá nhân của chính mình (`mockStudents[0]`) và không có nút duyệt đơn phép hoặc điểm danh lớp học.
  * Giáo viên chỉ được truy cập vào giao diện quản lý lớp 12A và nhập điểm môn PRM393.
* **Quy tắc phản hồi thông báo:** Các thông báo được cấu hình `requiresReply: true` sẽ hiển thị cảnh báo đỏ nổi bật trên UI phụ huynh và chỉ mất đi sau khi người dùng bấm nút "Xác nhận tham dự/Đã đọc".

---

## 5. Brainstorming: Đề xuất các Tính năng mở rộng giá trị (Future Enhancements)

Nhằm nâng cấp MyFschool từ một Sổ liên lạc điện tử cơ bản thành một Siêu ứng dụng giáo dục thông minh (School Super App), dưới đây là các tính năng được đề xuất phát triển thêm:

### 5.1. Dành cho Phụ huynh (Parent Hub)
* **Định vị & Theo dõi Xe đưa đón (School Bus GPS Real-time Tracker):**
  * Tích hợp bản đồ GPS thời gian thực trên giao diện phụ huynh.
  * Tự động gửi thông báo đẩy (Push Notification) khi xe buýt sắp đến điểm đón (ví dụ: cách 5 phút) hoặc khi con đã quét thẻ điểm danh lên/xuống xe an toàn.
* **Thanh toán học phí trực tuyến một chạm (One-tap QR Payment Hub):**
  * Hiển thị chi tiết hóa đơn học phí, tiền ăn bán trú, tiền xe điện tử hàng tháng.
  * Tạo mã VietQR động chứa sẵn số tiền và nội dung chuyển khoản để phụ huynh quét thanh toán nhanh từ bất kỳ app ngân hàng nào, tự động cập nhật trạng thái đóng tiền trên UI.
* **Theo dõi Sức khỏe & Thực đơn Bán trú (Boarding Care & Nutrition):**
  * Cập nhật thực đơn dinh dưỡng hàng tuần của con tại trường.
  * Cho phép phụ huynh ghi chú dị ứng thực phẩm hoặc đăng ký chế độ ăn riêng.
  * Biểu đồ theo dõi chiều cao, cân nặng, chỉ số BMI và lịch sử khám sức khỏe định kỳ tại phòng y tế trường.

### 5.2. Dành cho Học sinh (Student Center)
* **Trung tâm Bài tập & Hạn nộp (Homework & Deadline Tracker):**
  * Giao diện lịch học kết hợp hiển thị các đầu bài tập về nhà được giao từ giáo viên bộ môn.
  * Hỗ trợ tải tài liệu học tập, nộp bài trực tuyến dưới dạng ảnh chụp hoặc file tài liệu, và nhận thông báo khi bài tập sắp hết hạn.
* **Hệ thống Thi đua & Tích điểm đổi quà (Gamified Student Rewards):**
  * Ghi nhận điểm số rèn luyện hàng tuần của học sinh.
  * Nhận các "Huy hiệu điện tử" (ví dụ: Dũng cảm, Chăm chỉ, Thần đồng Toán học) từ giáo viên chủ nhiệm khi có biểu hiện xuất sắc.
  * Tích lũy điểm thi đua để đổi quà lưu niệm FPT Schools hoặc voucher tại căng tin trường.
* **Thư viện số & Thẻ mượn sách tích hợp (E-Library & Reservation):**
  * Tích hợp mã vạch thẻ thư viện số ngay trên app của học sinh.
  * Hỗ trợ tìm kiếm đầu sách trực tuyến, đặt lịch giữ sách trước và nhắc nhở thời hạn trả sách bằng thông báo tự động.

### 5.3. Dành cho Giáo viên (Teacher Assistant)
* **Chấm điểm rèn luyện & Hành vi lớp học thời gian thực (Classroom Points Tracker):**
  * Công cụ điểm thi đua nhanh (tương tự ClassDojo) để giáo viên cộng/trừ điểm rèn luyện trực tiếp cho học sinh ngay trong tiết học.
  * Thống kê tự động danh sách học sinh tích cực hoặc cần nhắc nhở trong ngày/tuần.
* **Trình nhập điểm Excel ngoại tuyến thông minh (Smart Offline Grading Checker):**
  * Hỗ trợ tải tệp Excel mẫu thông minh.
  * Khi giáo viên import tệp điểm lên hệ thống, giao diện Client sẽ tự động quét, kiểm tra lỗi cú pháp (sai định dạng điểm, sai mã học sinh, trùng lặp cột) và hiển thị trực quan các dòng bị lỗi trước khi xác nhận lưu dữ liệu.
* **Đồng bộ Lịch báo giảng & Điểm danh tự động (Syllabus & Timetable Sync):**
  * Liên kết thời khóa biểu dạy học của giáo viên với khung chương trình đào tạo của Bộ/Trường.
  * Giáo viên có thể đính kèm giáo án của bài học ngay trên lịch dạy và tự động kích hoạt danh sách điểm danh tương ứng với học sinh của tiết học đó.
* **Báo cáo đọc và ký nhận thông báo (Read-receipt Tracking Dashboard):**
  * Thống kê chi tiết danh sách phụ huynh chưa đọc thông báo quan trọng.
  * Tích hợp nút gửi thông báo nhắc nhở nhanh (Nudge) đến những phụ huynh chưa xác nhận đóng tiền học hoặc tham gia họp lớp.
