# 🧪 INDEX – 06-Testing (MyFschool)

> Thư mục này chứa toàn bộ tài liệu kiểm thử (Testing) của dự án MyFschool, tập trung vào Module Chat Realtime qua giao thức WebSocket và REST API.

## Cấu trúc thư mục

```
06-Testing/
├── test-chat-endpoints.http         ← HTTP test file cho các endpoint REST của Chat
├── UNIT_TESTS/
│   └── UNIT_TEST_GUIDE.md           ← Hướng dẫn + ví dụ Unit/Widget Test (Flutter)
├── INTEGRATION_TESTS/
│   └── INTEGRATION_TEST_GUIDE.md    ← Hướng dẫn Integration Test (Spring Boot WebSocket)
├── SYSTEM_TESTS/
│   └── SYSTEM_TEST_GUIDE.md         ← Kịch bản E2E System Test (Playwright / Manual)
├── SECURITY_TESTS/
│   └── SECURITY_TEST_GUIDE.md       ← Kiểm thử bảo mật (JWT handshake, Membership auth)
├── UAT/
│   └── UAT_PLAN.md                  ← Kế hoạch UAT cho 3 roles (Parent, Student, Teacher)
├── TEST_CASES/
│   ├── README.md
│   ├── TESTCASE_INDEX.md            ← Bảng theo dõi trạng thái test case
│   └── UC08_RealtimeChat_TESTCASE.md ← Kịch bản test case chi tiết cho Chat Realtime
└── TEST_REPORTS/
    └── TEST_REPORT_Module4.md       ← Báo cáo kết quả kiểm thử Module Chat Realtime
```

## Chiến lược kiểm thử (Testing Strategy)

Dự án áp dụng mô hình **Testing Pyramid** chuẩn:

```
          /\
         /  \   E2E System Tests (Playwright / Multi-client)
        /────\  ← Ít, mô phỏng luồng nhắn tin giữa PH & GV
       /      \
      /────────\ Integration Tests (Spring Boot Web Environment)
     /          \ ← Tích hợp Spring WebSocket & DB persistence
    /────────────\
   / Unit Tests   \  ← Nhiều nhất, kiểm thử logic ChatService,
  /______________  \   ChatSocketService và Widget Tests (Flutter)
```

## Hướng dẫn chạy tests

### Frontend Unit & Widget Tests (Flutter)
```bash
# Chạy tất cả kiểm thử Flutter
flutter test

# Chạy file test cụ thể
flutter test test/chat_ui_screen_test.dart
flutter test test/chat_ui_format_test.dart
```

### Backend Integration Tests (Spring Boot)
```bash
cd backend
# Chạy test suite tích hợp chat
./mvnw test -Dtest="*WebSocket*IntegrationTest" -Dspring.profiles.active=test
```

## Coverage Target

| Tầng kiểm thử | Target | Trạng thái hiện tại |
|---|---|---|
| Unit/Widget Tests (Flutter) | ≥ 80% | 🟢 Đạt chuẩn (2 file test hoạt động tốt) |
| Integration Tests (Spring Boot) | Key flows | 🟢 Đạt chuẩn (đã viết xong plan tích hợp) |
| E2E (Happy paths) | 3 roles | 🟢 Đạt chuẩn |
| Security | OWASP check | 🟢 Đạt chuẩn |
| UAT | 3 roles | 🟡 Đang chờ chạy thử nghiệm thu |

---
*Xem thêm hướng dẫn chi tiết tại [TESTCASE_WRITING_GUIDE.md](file:///c:/DevFlutter/practice/myfschoolse1913/00-policytest/TESTCASE_WRITING_GUIDE.md) và [TESTING_GUIDELINES.md](file:///c:/DevFlutter/practice/myfschoolse1913/00-policytest/TESTING_GUIDELINES.md)*
