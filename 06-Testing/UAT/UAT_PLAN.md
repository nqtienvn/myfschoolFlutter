# 👥 USER ACCEPTANCE TESTING (UAT) – MyFschool Chat Realtime

> **Thư mục:** `06-Testing/UAT/`  
> **Mục đích:** Kế hoạch kiểm thử chấp nhận người dùng (UAT) – xác thực module chat realtime đáp ứng đầy đủ yêu cầu nghiệp vụ thực tế từ góc nhìn của Phụ huynh, Học sinh và Giáo viên.  
> **Giai đoạn:** Nghiệm thu kỹ thuật cuối kỳ trước khi bàn giao hệ thống.

---

## 1. Thông tin chung về UAT

| Thông tin | Chi tiết |
| :--- | :--- |
| **Ngày dự kiến chạy UAT** | Tuần cuối trước khi hoàn thành dự án |
| **Môi trường** | Máy ảo kiểm thử local hoặc máy chủ staging của trường |
| **Thành phần tham gia** | Đại diện nhóm phát triển (đóng vai từng role) + Giáo viên nghiệm thu |
| **Cơ sở đánh giá** | Đặc tả kỹ thuật module liên lạc và kịch bản quy trình (Business flows) |

---

## 2. Kịch bản UAT chi tiết theo vai trò người dùng (Roles)

### 👤 Vai trò: PHỤ HUYNH / HỌC SINH (Parent / Student)

| Mã UAT | Kịch bản kiểm thử (Scenario) | Kết quả mong đợi | Kết quả | Ghi chú |
| :--- | :--- | :--- | :---: | :--- |
| **UAT-CHAT-001** | Đăng nhập và mở màn hình "Tin nhắn" (Tab 1) | Thấy danh sách các cuộc hội thoại hiện có, hiển thị tên Giáo viên và tin nhắn cuối cùng kèm thời gian. | ⬜ | |
| **UAT-CHAT-002** | Xem chấm xanh hoạt động (Presence) | Thấy chấm xanh online bên cạnh tên Giáo viên nếu Giáo viên cũng đang mở app. | ⬜ | |
| **UAT-CHAT-003** | Gửi tin nhắn mới cho Giáo viên | Tin nhắn hiển thị bong bóng bên phải. Trạng thái dưới tin nhắn chuyển nhanh: `Đang gửi` ──► `Đã gửi` (trong vòng <200ms). | ⬜ | Happy Path |
| **UAT-CHAT-004** | Theo dõi trạng thái đã xem (Read Receipt) | Khi Giáo viên mở màn hình xem, trạng thái tin nhắn đổi thành `Đã xem` (hiển thị 2 check xanh dưới bóng tin nhắn). | ⬜ | |
| **UAT-CHAT-005** | Nhận tin nhắn trả lời và mark read | Nhận tin nhắn mới lập tức (realtime) từ Giáo viên, badge unread count tự động cập nhật. Khi mở ra xem, server nhận tin báo đã xem thành công. | ⬜ | |
| **UAT-CHAT-006** | Mất mạng đột ngột và kết nối lại (Offline Sync) | Tắt wifi/mạng khi Giáo viên gửi tin nhắn. Khi bật lại mạng, app tự động sync và hiển thị đầy đủ tin nhắn đã bỏ lỡ theo đúng sequence (`serverSeq`). | ⬜ | Resilience |

---

### 👩‍🏫 Vai trò: GIÁO VIÊN (Teacher)

| Mã UAT | Kịch bản kiểm thử (Scenario) | Kết quả mong đợi | Kết quả | Ghi chú |
| :--- | :--- | :--- | :---: | :--- |
| **UAT-CHAT-010** | Đăng nhập và xem Inbox (TeacherInboxScreen) | Thấy danh sách các cuộc hội thoại với Phụ huynh. Hiển thị badge số tin nhắn chưa đọc màu đỏ nổi bật. | ⬜ | |
| **UAT-CHAT-011** | Theo dõi trạng thái đang soạn tin (Typing) | Khi Phụ huynh gõ tin nhắn, màn hình Giáo viên hiển thị bong bóng động "Phụ huynh đang soạn tin..." thời gian thực. | ⬜ | |
| **UAT-CHAT-012** | Nhận tin nhắn mới và tự động gửi biên lai delivered | Giáo viên nhận tin nhắn mới không bị trễ. Hệ thống tự gửi sự kiện `message.delivered` về cho Phụ huynh biết thiết bị Giáo viên đã nhận tin. | ⬜ | |
| **UAT-CHAT-013** | Phản hồi tin nhắn nhanh | Giáo viên gõ phản hồi nhanh từ popup hoặc TextField và gửi đi thành công, Phụ huynh nhận được ngay lập tức. | ⬜ | |

---

## 3. Danh sách tài khoản thử nghiệm chuẩn bị sẵn

| Vai trò (Role) | Số điện thoại | Mật khẩu | Tên hiển thị |
| :--- | :--- | :--- | :--- |
| **TEACHER** | `0909000001` | `password123` | Cô Nguyễn Thị Mai (Giáo viên Chủ nhiệm) |
| **PARENT** | `0909000002` | `password123` | Anh Trần Văn Hùng (Phụ huynh em Trần An) |
| **STUDENT** | `0909000003` | `password123` | Em Trần An (Học sinh lớp 10A1) |

---

## 4. Tiêu chuẩn nghiệm thu UAT (Acceptance Criteria)

> [!IMPORTANT]
> Tính năng Chat Realtime chỉ được coi là **ĐẠT CHUẨN UAT** để đưa vào sử dụng thực tế khi đáp ứng tất cả các tiêu chí sau:

1. **✅ Tính đúng đắn (Core Logic)**: 10/10 kịch bản UAT trên phải đạt kết quả **PASS**.
2. **✅ Trải nghiệm thời gian thực**: Tin nhắn phải được gửi và nhận tức thời (<300ms trên môi trường mạng thông thường).
3. **✅ Khả năng chịu lỗi (Offline resilience)**: Khi mất mạng và kết nối lại, tin nhắn tuyệt đối không bị trùng lặp (trùng `clientMessageId`) và không bị mất (sync theo `serverSeq`).
4. **✅ An toàn bảo mật**: Kịch bản chặn người ngoài xem trộm/gửi trộm tin nhắn (`NOT_CONVERSATION_MEMBER`) phải được thực thi triệt để trên backend.
5. **✅ Trạng thái chính xác**: Đồng bộ trạng thái đọc/nhận tin (đồng hồ -> 1 check -> 2 check xám -> 2 check xanh) hoạt động ổn định giữa các thiết bị.
