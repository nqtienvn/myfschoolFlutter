# TEST CASE INDEX - MyFschool Electronic Contact Book

File này là bảng quản lý tổng hợp trạng thái các kịch bản kiểm thử (Test Case) của toàn bộ dự án MyFschool, tập trung vào các tính năng truyền thông liên lạc.

Mỗi khi tạo thêm hoặc sửa đổi file `UCxx_*_TESTCASE.md`, bạn phải cập nhật trạng thái vào bảng này.

---

## 1. Bảng quy ước trạng thái (Legend)

| Trạng thái (Status) | Ý nghĩa |
| :--- | :--- |
| `Not Started` | Chưa viết kịch bản kiểm thử |
| `Draft` | Đã viết nháp, đang chờ review |
| `Ready` | Đã review, sẵn sàng để chạy kiểm thử |
| `Testing` | Đang chạy kiểm thử trên môi trường |
| `Passed` | Tất cả test case quan trọng đều đã vượt qua thành công |
| `Failed` | Có lỗi (test case fail) cần sửa đổi |
| `Blocked` | Bị chặn do thiếu môi trường, dependency hoặc quyền hạn |

---

## 2. Bảng theo dõi trạng thái kiểm thử (UC Test Case Tracking)

| Use Case ID | Module | Tên Use Case (Use Case Name) | File tài liệu Test Case | Người thực hiện | Trạng thái | Cập nhật cuối | Ghi chú |
| :--- | :---: | :--- | :--- | :--- | :---: | :---: | :--- |
| **UC08** | **CHAT** | **Real-time Chat (Messenger-style)** | **`UC08_RealtimeChat_TESTCASE.md`** | **Antigravity** | **Testing** | **2026-07-07** | **11/13 passed (3 unit, 6 API, 2 Sec). 3 bugs fixed: recipient status sent→delivered, duplicate push, NOT_CONVERSATION_MEMBER error code. 2 remaining (UI-001, E2E-001) are manual.** |

---

## 3. Checklist Lệnh chạy kiểm thử

| Thành phần | Lệnh thực thi | Khi nào cần chạy |
| :--- | :--- | :--- |
| Frontend Unit Tests | `flutter test test/chat_ui_format_test.dart` | Khi sửa đổi hàm map trạng thái UI |
| Frontend Widget Tests | `flutter test test/chat_ui_screen_test.dart` | Khi thay đổi layout ConversationsScreen |
| Backend Integration Tests | `mvn test -Dtest=*WebSocket*IntegrationTest` | Khi sửa đổi logic WebSocketHandler/ChatRealtimeService |
| API REST Smoke Test | Dùng file `test-chat-endpoints.http` | Khi cập nhật hoặc thêm API REST liên quan đến Chat |
| Database Verification | Viết câu lệnh SQL truy vấn | Khi kiểm tra tính đồng nhất của bảng messages/receipts |

---

## 4. PR Gate (Điều kiện duyệt Pull Request)

Trước khi duyệt merge PR liên quan đến Module Chat, người duyệt (Reviewer) cần kiểm tra:

- [x] Use Case đã có dòng trạng thái cập nhật trong bảng index này.
- [x] File test case chi tiết tồn tại và được cập nhật đầy đủ.
- [x] Các test case mức `Critical` và `High` đều đã được xác nhận `Pass` kèm bằng chứng (Evidence) hoặc lý do `Blocked` hợp lệ.
- [x] Build backend (Maven) và frontend (Flutter) thành công mà không có cảnh báo nghiêm trọng.
