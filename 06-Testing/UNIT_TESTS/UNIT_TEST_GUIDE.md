# 🧪 UNIT TESTS – MyFschool Chat Realtime

> **Thư mục:** `06-Testing/UNIT_TESTS/`  
> **Mục đích:** Tập hợp tài liệu, quy chuẩn và ví dụ kiểm thử đơn vị (Unit Test) và Widget Test cho Module Chat Realtime của ứng dụng MyFschool.  
> **Framework:** Flutter Test SDK (Dart) | JUnit 5 + Mockito (Spring Boot Backend)

---

## 1. Tổng quan Unit Test & Widget Test Coverage

| Module / Component | Target Coverage | Trạng thái | Ghi chú |
| :--- | :---: | :--- | :--- |
| **Chat Utils (`chatStatusLabel`)** | 100% | 🟢 PASS | Ánh xạ trạng thái tin nhắn sang tiếng Việt trên UI |
| **Chat UI Components (`ConversationsScreen`)** | ≥ 80% | 🟢 PASS | Render danh sách và xử lý phản hồi từ ChatService |
| **Chat Lifecycle & Auth (`LoginScreen` integration)** | ≥ 80% | 🟢 PASS | Khởi tạo kết nối ChatService sau khi login thành công |
| **Chat Service (`ChatService` state)** | ≥ 75% | 🟡 In Progress | Logic xử lý state, timer ACK, retry và merge tin nhắn |
| **Chat WebSocket Client (`ChatSocketService`)** | ≥ 70% | 🟡 In Progress | Giao tiếp dòng lệnh WebSocket và tự động reconnect |

---

## 2. Hướng dẫn kiểm thử đơn vị & Giao diện (Flutter/Dart)

### 2.1 Unit Test định dạng trạng thái (Utility Tests)
Kiểm thử các hàm helper thuần Dart, không có dependency UI hay asynchronous socket.

#### `test/chat_ui_format_test.dart`
```dart
import 'package:flutter_test/flutter_test.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/chat_detail_screen.dart';

void main() {
  test('chat status label maps delivery states to Vietnamese UI text', () {
    // Assert các trạng thái ánh xạ hiển thị đúng trên bong bóng tin nhắn
    expect(chatStatusLabel('sending'), 'Đang gửi');
    expect(chatStatusLabel('sent'), 'Đã gửi');
    expect(chatStatusLabel('delivered'), 'Đã nhận');
    expect(chatStatusLabel('read'), 'Đã xem');
    expect(chatStatusLabel('failed'), 'Gửi lỗi');
  });
}
```

### 2.2 Widget Test giao diện (UI Widget & Mock Service Tests)
Kiểm thử render giao diện và tương tác người dùng bằng cách giả lập (mock) `ChatService`, tránh việc kết nối WebSocket thật khi chạy test.

#### `test/chat_ui_screen_test.dart`
```dart
import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/models/conversation.dart';
import 'package:myfschoolse1913/vn/edu/fpt/src/services/chat_service.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/actor_models.dart';
import 'package:myfschoolse1913/vn/edu/fpt/view/screens/messages_screen.dart';

void main() {
  testWidgets('ConversationsScreen renders conversations from ChatService', (tester) async {
    // 1. Arrange: Chuẩn bị ChatService fake chứa 1 cuộc hội thoại giả lập
    final service = _FakeChatService([
      const Conversation(
        id: 42,
        unreadCount: 2,
        lastMessage: 'Tin nhắn từ backend',
        otherParticipant: ChatParticipant(userId: 7, name: 'GV Backend', role: 'TEACHER'),
      ),
    ]);

    // 2. Act: Bơm widget ConversationsScreen vào cây widget test
    await tester.pumpWidget(
      MaterialApp(home: ConversationsScreen(actor: AppActor.parent, chatService: service)),
    );
    await tester.pump(); // Kích hoạt render lại nếu có stream/future

    // 3. Assert: Kiểm tra giao diện hiển thị đúng thông tin của cuộc hội thoại
    expect(find.text('GV Backend'), findsOneWidget);
    expect(find.text('Tin nhắn từ backend'), findsOneWidget);
    expect(find.text('2'), findsOneWidget); // badge số tin nhắn chưa đọc
  });
}

// Lớp giả lập ChatService dùng cho Widget Test
class _FakeChatService extends ChatService {
  _FakeChatService([this.items = const []]) : super(repository: _FakeChatRepository(), socketService: _FakeSocketService());

  final List<Conversation> items;
  int startCalls = 0;

  @override
  List<Conversation> get conversations => items;

  @override
  Future<void> start(AuthSession session) async {
    startCalls++;
  }

  @override
  Future<void> stop() async {}
}
```

---

## 3. Hướng dẫn chạy và xuất báo cáo kiểm thử

### 3.1 Chạy toàn bộ Unit/Widget Tests trên Flutter
```bash
# Lệnh cơ bản
flutter test

# Chạy cụ thể một file test
flutter test test/chat_ui_screen_test.dart

# Xuất báo cáo coverage (lệnh sinh file lcov.info)
flutter test --coverage
```

### 3.2 Đọc kết quả Coverage
Sau khi sinh file `coverage/lcov.info`, bạn có thể dùng các extension trong IDE (như Flutter Coverage trong VS Code) hoặc dùng công cụ `lcov` để chuyển đổi sang trang HTML trực quan:
```bash
# Cài đặt lcov (trên macOS/Linux) hoặc dùng chocolatey trên Windows
genhtml coverage/lcov.info -o coverage/html
# Mở coverage/html/index.html bằng trình duyệt để xem báo cáo chi tiết
```

---

## 4. Checklist trước khi submit code test mới

- [ ] Đặt tên file test có hậu tố `_test.dart` (nằm trong thư mục `test/`).
- [ ] Mọi kịch bản test phải dùng dữ liệu giả lập (synthetic data), tuyệt đối không dùng tài khoản hay thông tin học sinh thật.
- [ ] Không sử dụng `sleep()` hoặc block thread trong test – luôn sử dụng async/await và `tester.pump()` / `tester.pumpAndSettle()`.
- [ ] Mock hoặc Fake các tầng kết nối ngoài (Socket, HTTP Client) để đảm bảo bộ unit test có thể chạy offline mà không phụ thuộc vào trạng thái server.
- [ ] Đạt tỷ lệ bao phủ (Coverage) tối thiểu 70% đối với các file logic nghiệp vụ mới viết.
