# Spec: Đồng bộ hóa luồng Chat cho các vai trò PARENT, STUDENT, TEACHER

* **Tác giả:** Senior Flutter Developer
* **Ngày tạo:** 2026-07-07
* **Trạng thái:** Chờ duyệt

---

## 1. Phân tích luồng hoạt động hiện tại (PARENT làm chuẩn)

Hệ thống Chat thời gian thực hiện tại hoạt động dựa trên các thành phần sau:

### **Tầng Dữ liệu & Xử lý (Shared across all roles):**
1. **API Client (`ChatApiClient`):** Thực hiện các cuộc gọi REST HTTP lên Backend qua các endpoint như `/api/conversations` (danh sách hội thoại), `/api/conversations/{id}` (danh sách tin nhắn), `/api/conversations/search-users` (tìm kiếm người dùng). Không phân biệt vai trò ở Client, Backend tự nhận diện vai trò qua JWT Authorization Token.
2. **Repository (`ChatRepository`):** Chuyển đổi dữ liệu DTO từ API Client thành Model dạng Domain.
3. **Socket Service (`ChatSocketService`):** Quản lý kết nối STOMP WebSocket để gửi nhận tin nhắn thời gian thực.
4. **State Management (`ChatService`):** Kế thừa `ChangeNotifier`, lưu trữ danh sách hội thoại, tin nhắn theo ID cuộc hội thoại, và đồng bộ hóa trạng thái gửi/nhận/đọc tin nhắn.

### **Luồng điều hướng và giao diện PARENT (Chuẩn):**
1. **Đăng nhập (`LoginScreen`):** Thành công sẽ gọi `chatService.start(session)` để mở kết nối WebSocket và tải trước danh sách cuộc hội thoại ban đầu, sau đó chuyển sang `AppShell`.
2. **Màn hình chính (`AppShell`):** Khởi tạo Nested Navigator cho từng Tab. Tab 1 (Tin nhắn) gọi `ConversationsScreen` và truyền `chatService` từ `AppShell`.
3. **Danh sách hội thoại (`_ServiceConversationsScreen`):** Lắng nghe trạng thái từ `ChatService` thông qua `ListenableBuilder` để cập nhật danh sách cuộc trò chuyện động.
4. **Mở Chat (`ChatDetailScreen`):** Khi chọn một hội thoại hoặc tạo mới qua màn hình tìm kiếm (`UserSearchScreen`), ứng dụng đẩy `ChatDetailScreen` vào Nested Navigator.
5. **Gửi/Nhận tin nhắn:** Xử lý qua STOMP WebSocket của `ChatService` với cơ chế cập nhật trạng thái (Sending -> Sent -> Delivered -> Read).

---

## 2. Điểm khác biệt hiện tại của STUDENT và TEACHER

### **STUDENT:**
* **Giao diện & Logic:** Đã tự động sử dụng chung `_ServiceConversationsScreen` khi `chatService != null` trong `ConversationsScreen`. Luồng chạy hoàn toàn tương tự PARENT.

### **TEACHER:**
* **Giao diện & Logic:** Bị chặn cứng ở `ConversationsScreen` và chuyển sang `TeacherInboxScreen()` sử dụng dữ liệu tĩnh (Mock Data):
  ```dart
  if (actor == AppActor.teacher) {
    return const TeacherInboxScreen();
  }
  ```
  Do đó, khi chuyển sang vai trò TEACHER, người dùng chỉ thấy các tin nhắn giả lập và không thể gửi/nhận hay đồng bộ dữ liệu thật với Backend.

---

## 3. Đề xuất Thay đổi & Giải pháp Đồng bộ

Chúng ta sẽ thống nhất cả 3 vai trò sử dụng chung luồng Chat thật (`_ServiceConversationsScreen`) khi có dịch vụ `chatService` hoạt động, và chỉ giữ lại giao diện cũ/mock làm fallback dự phòng.

### **Chi tiết Thay đổi trong `lib/vn/edu/fpt/view/screens/messages_screen.dart`:**

#### **Bước 1: Cập nhật điều hướng trong `ConversationsScreen`**
Cho phép TEACHER đi qua luồng dữ liệu thật nếu `chatService` có sẵn, chỉ chuyển hướng sang `TeacherInboxScreen` khi không có dịch vụ.
```dart
  @override
  Widget build(BuildContext context) {
    final service = chatService;
    if (service != null) {
      return _ServiceConversationsScreen(chatService: service, actor: actor);
    }

    if (actor == AppActor.teacher) {
      return const TeacherInboxScreen();
    }
    // ... Fallback Mock của Parent/Student
```

#### **Bước 2: Cập nhật `_ServiceConversationsScreen`**
1. Nhận thêm tham số `actor` để xác định vai trò hiện tại.
2. Điều chỉnh tiêu đề AppBar tương thích theo vai trò:
   * Vai trò `AppActor.teacher`: hiển thị tiêu đề **"Tin nhắn phụ huynh"** (như thiết kế mock cũ của Teacher).
   * Các vai trò còn lại (`parent`, `student`): hiển thị tiêu đề **"Tin nhắn liên lạc"**.

```dart
class _ServiceConversationsScreen extends StatelessWidget {
  const _ServiceConversationsScreen({required this.chatService, this.actor = AppActor.parent});

  final ChatService chatService;
  final AppActor actor;

  @override
  Widget build(BuildContext context) {
    // ...
    appBar: OrangeTopBar(
      title: actor == AppActor.teacher ? 'Tin nhắn phụ huynh' : 'Tin nhắn liên lạc',
      actions: [ ... ]
    ),
  }
}
```

---

## 4. Kế hoạch Kiểm thử & Xác minh (Verification Plan)

### **Kiểm thử tự động (Automated Tests):**
* Chạy lại các Widget Tests hiện tại để đảm bảo không bị ảnh hưởng hành vi cũ của `ConversationsScreen` đối với PARENT.

### **Kiểm thử thủ công (Manual Verification):**
1. **Kiểm tra vai trò TEACHER:**
   * Đăng nhập bằng tài khoản TEACHER.
   * Truy cập Tab Tin nhắn: Xác nhận tiêu đề là *"Tin nhắn phụ huynh"*.
   * Xác nhận danh sách hội thoại được tải từ Backend thật.
   * Thử tìm kiếm phụ huynh mới và mở phòng chat.
   * Thử gửi tin nhắn và xác nhận trạng thái tin nhắn cập nhật đúng thời gian thực.
2. **Kiểm tra vai trò STUDENT:**
   * Đăng nhập bằng tài khoản STUDENT.
   * Truy cập Tab Tin nhắn: Xác nhận tiêu đề là *"Tin nhắn liên lạc"*.
   * Xác nhận danh sách tải bình thường từ Backend thật, gửi nhận tin nhắn thành công.
3. **Kiểm tra vai trò PARENT:**
   * Đảm bảo tính năng nhắn tin cũ vẫn hoạt động trơn tru không thay đổi.
