# BÁO CÁO LỖ HỔNG BAO PHỦ KIỂM THỬ — Module Chat Realtime (UC08)

* **Ngày tạo**: 2026-07-07
* **Cập nhật lần cuối**: 2026-07-07 (sau khi chạy backend integration tests)
* **Tài liệu tham chiếu**: `UC08_RealtimeChat_TESTCASE.md`, `TEST_REPORT_Module4.md`, `ConversationMessagingIntegrationTest.java`

---

## 1. Tổng quan tình trạng hiện tại

| Chỉ số | Giá trị |
|:---|:---|
| Tổng số test case trong UC08 | 13 |
| Đã chạy & PASS | **8 (61.5%)** |
| Chưa chạy (Not Run) | 5 (38.5%) |
| Backend test file | `ConversationMessagingIntegrationTest.java` — 6 test methods |
| Flutter unit tests | 2 files, 3 test cases |

---

## 2. Trạng thái từng test case

### ✅ Đã PASS — 8 test cases

| TC ID | Loại | Evidence |
|:---|:---|:---|
| TC-CHAT-UNIT-001 | Unit | `flutter test test/chat_ui_format_test.dart` |
| TC-CHAT-UNIT-002 | Unit | `flutter test test/chat_ui_screen_test.dart` |
| TC-CHAT-UNIT-003 | Unit | `flutter test test/chat_ui_screen_test.dart` |
| TC-CHAT-API-001 | API | `ConversationMessagingIntegrationTest#send_message_is_idempotent_by_sender_and_client_message_id` |
| TC-CHAT-API-003 | API | `ConversationMessagingIntegrationTest#delivered_receipt_is_upserted_for_recipient` |
| TC-CHAT-API-004 | API | `ConversationMessagingIntegrationTest#mark_read_updates_last_read_message_id` + `#mark_read_without_body_reads_latest_message` |
| TC-CHAT-API-006 | API | `ConversationMessagingIntegrationTest#get_messages_returns_saved_messages_and_after_seq_sync` |
| TC-CHAT-SEC-002 | Security | `ConversationMessagingIntegrationTest#sender_outside_conversation_is_forbidden` |

### ❌ Chưa chạy — 5 test cases

| TC ID | Priority | Lý do | Có thể chạy không? |
|:---|:---:|:---|:---:|
| **TC-CHAT-API-002** | Critical | message.new push — cần 2 WebSocket client đồng thời | Cần WS environment |
| **TC-CHAT-API-005** | Medium | typing.start/stop — cần WS realtime 2 chiều | Cần WS environment |
| **TC-CHAT-SEC-001** | Critical | JWT handshake reject — cần WS server thật test handshake | Cần WS environment |
| **TC-CHAT-UI-001** | High | UI manual — cần thiết bị thật verify presence/status icons | Manual |
| **TC-CHAT-E2E-001** | Critical | Full flow 2 chiều — cần 2 thiết bị + WebSocket | Manual |

---

## 3. Lỗ hổng còn lại

### 3.1 WebSocket realtime tests chưa implement

4/5 test còn lại đều liên quan đến WebSocket 2 chiều — cần môi trường có WS server chạy thật. Đây là hạn chế của môi trường sandbox hiện tại. Giải pháp:
- Dùng Spring Boot `@SpringBootTest(webEnvironment = RANDOM_PORT)` với WebSocket client thật
- Hoặc dùng `ChatWebSocketHandler` trong test với `StandardWebSocketClient`
- Kế hoạch: implement khi có staging environment

### 3.2 UI test còn thiếu (manual)

`TC-CHAT-UI-001` yêu cầu verify bằng tay trên thiết bị: presence, status icons, typing indicator, badge unread count. Cần chạy thủ công trước UAT.

### 3.3 Backend unit test thuần (không integration)

`ConversationMessagingIntegrationTest` là integration test (cần DB, Spring context). Chưa có unit test thuần cho:
- `ChatRealtimeService.handleConnected()` — presence
- `ChatRealtimeService.handleDisconnected()` — presence cleanup
- `ChatRealtimeService.handle()` — WS event routing
- `JwtTokenProvider.validateToken()` — JWT validation edge cases

**Mức độ ưu tiên**: Thấp — vì integration test đã cover các flow chính. Có thể bổ sung sau.

---

## 4. Kết luận

**Tiến triển tốt**: 8/13 test cases PASS (từ 23% lên 61.5%). Backend test file `ConversationMessagingIntegrationTest.java` đã implement 6 test methods cover idempotency, read/delivered receipt, afterSeq sync, membership security.

**Còn 5 test cases chưa chạy** — chủ yếu do giới hạn môi trường (cần WebSocket 2 chiều và thiết bị thật). Phù hợp để chạy trên staging trước UAT.

**Báo cáo hiện tại đã đúng** — `TEST_REPORT_Module4.md` ghi "ĐANG TIẾN HÀNH KIỂM THỬ", `TESTCASE_INDEX.md` ghi trạng thái "Testing".
