# UC08 Real-time Chat Test Cases

---

## Document Control

| Field | Value |
| :--- | :--- |
| **Document ID** | `MFS-UC08-TC-001` |
| **Use Case ID** | `UC08` |
| **Use Case Name** | Real-time Chat |
| **Module** | `CHAT` |
| **Version** | `1.0` |
| **Status** | `Draft` |
| **Author** | `Antigravity` |
| **Reviewer** | `TBD` |
| **Created Date** | `2026-07-07` |
| **Last Updated** | `2026-07-07` |
| **Data Classification** | `Internal, PII-linked` |

---

## Changelog

| Date | Author | Change |
| :--- | :--- | :--- |
| 2026-07-07 | Antigravity | Created UC08 test case document for Real-time Chat based on specifications and implementation plans. |

---

## 1. Use Case Summary

### 1.1 Business Goal

Phụ huynh, Học sinh và Giáo viên có thể nhắn tin trao đổi trực tiếp với nhau thông qua giao thức WebSocket thời gian thực (Messenger-style). Tin nhắn được lưu trữ an toàn trong cơ sở dữ liệu, hỗ trợ hiển thị trạng thái tin nhắn (đang gửi, đã gửi, đã nhận, đã xem), trạng thái online/offline của người dùng, và hiển thị đang soạn tin nhắn. Khi mất kết nối và kết nối lại, hệ thống sẽ tự động đồng bộ tin nhắn mới dựa trên sequence number (`serverSeq`) để đảm bảo không bị mất hoặc trùng lặp dữ liệu.

### 1.2 Actors

| Actor | Role in UC |
| :--- | :--- |
| Parent / Student | Gửi/nhận tin nhắn với Giáo viên, đồng bộ lịch sử, theo dõi trạng thái online của Giáo viên |
| Teacher | Nhận tin nhắn từ Phụ huynh/Học sinh qua Inbox, phản hồi trực tiếp, cập nhật trạng thái đã xem |
| System | Xác thực kết nối WebSocket qua JWT, xử lý logic lưu trữ, đánh số sequence, ACK gửi tin và push tin nhắn thời gian thực |

### 1.3 Scope

| In Scope | Out of Scope |
| :--- | :--- |
| Kết nối WebSocket JWT handshake | Nhắn tin nhóm (Group chat) |
| Gửi/ACK tin nhắn và chống trùng lặp (`clientMessageId`) | Gửi file/hình ảnh đính kèm (Media attachments) |
| Đẩy tin nhắn mới realtime (`message.new`) | Thu hồi/sửa tin nhắn (Delete/Edit) |
| Đẩy xác nhận đã nhận (`message.delivered`) | Tìm kiếm nội dung tin nhắn |
| Đẩy xác nhận đã xem (`message.read`) | Đẩy thông báo FCM khi offline |
| Trạng thái online/offline (Presence) dạng in-memory | Lưu trữ trạng thái online trong Redis |
| Trạng thái đang soạn tin nhắn (Typing indicator) | |
| Đồng bộ tin nhắn sau reconnect qua REST API (`afterSeq`) | |

---

## 2. Requirement Traceability

| Requirement ID | Source Document | Requirement Summary | Test Case IDs |
| :--- | :--- | :--- | :--- |
| `UC08-BR-001` | `docs/superpowers/plans/2026-06-29-websocket-messaging.md` | Gửi/nhận tin nhắn realtime qua WebSocket (ACK sender + push recipient). | `TC-CHAT-API-001`, `TC-CHAT-API-002`, `TC-CHAT-E2E-001` |
| `UC08-BR-002` | `docs/superpowers/plans/2026-06-29-websocket-messaging.md` | Chống trùng lặp tin nhắn (idempotency) bằng `clientMessageId`. | `TC-CHAT-API-001` |
| `UC08-BR-003` | `docs/superpowers/plans/2026-06-29-websocket-messaging.md` | Đồng bộ tin nhắn sau khi reconnect bằng REST API với `afterSeq`. | `TC-CHAT-API-006` |
| `UC08-BR-004` | `docs/superpowers/plans/2026-06-29-websocket-messaging.md` | Trạng thái tin nhắn (sending, sent, delivered, read) và đang soạn tin (typing indicator). | `TC-CHAT-UNIT-001`, `TC-CHAT-API-003`, `TC-CHAT-API-004`, `TC-CHAT-API-005` |
| `UC08-SEC-001` | `docs/superpowers/specs/phase-3-communication.md` | Xác thực kết nối WebSocket bằng JWT Token khi handshake. | `TC-CHAT-SEC-001` |
| `UC08-SEC-002` | `docs/superpowers/specs/phase-3-communication.md` | Kiểm tra quyền thành viên trong cuộc hội thoại (membership validation). | `TC-CHAT-SEC-002` |
| `UC08-DB-001` | `docs/database.md` | Lưu tin nhắn vào cơ sở dữ liệu, cập nhật tin nhắn cuối cùng (`last_message`, `last_message_at`) của cuộc hội thoại. | `TC-CHAT-INT-001` |

---

## 3. Preconditions & Assumptions

### 3.1 Preconditions

- [ ] Backend đang chạy local tại `http://localhost:8080` (hoặc profile `test`).
- [ ] WebSocket endpoint `/chat` đã được kích hoạt.
- [ ] Database MySQL đã được tạo bảng `conversations`, `conversation_participants`, `messages`, `message_receipts` và seed dữ liệu mẫu.
- [ ] Flutter app đã tích hợp package `web_socket_channel` và có token JWT hợp lệ sau khi đăng nhập.

### 3.2 Assumptions

| ID | Assumption | Impact if Wrong |
| :--- | :--- | :--- |
| `ASM-001` | Trạng thái `sending` và `failed` của tin nhắn chỉ nằm ở phía Frontend local. | Nếu lưu cả trạng thái `sending` vào DB sẽ gây nhiễu và làm chậm critical path gửi tin nhắn. |
| `ASM-002` | `serverSeq` là số tăng dần liên tục trong phạm vi một cuộc hội thoại. | Đồng bộ reconnect (`afterSeq`) sẽ bị sót tin nếu sequence không tăng liên tục hoặc bị trùng. |

---

## 4. Test Data

| Data ID | Type | Value / Setup | Used By | Note |
| :--- | :--- | :--- | :--- | :--- |
| `TD-CHAT-001` | Auth Session | `token` hợp lệ của Giáo viên A (User ID = 7, Role = TEACHER) | `TC-CHAT-SEC-002`, `TC-CHAT-API-*` | Giả lập JWT |
| `TD-CHAT-002` | Auth Session | `token` hợp lệ của Phụ huynh B (User ID = 10, Role = PARENT) | `TC-CHAT-SEC-002`, `TC-CHAT-API-*` | Giả lập JWT |
| `TD-CHAT-003` | DB Seed | Cuộc hội thoại ID = 123 gồm 2 thành viên: Giáo viên A và Phụ huynh B | `TC-CHAT-API-*`, `TC-CHAT-E2E-*` | Reset được sau test |
| `TD-CHAT-004` | WS Payload | `message.send` hợp lệ: `conversationId=123`, `clientMessageId="fe-uuid-001"`, `content="Xin chào cô"` | `TC-CHAT-API-001`, `TC-CHAT-API-002` | |
| `TD-CHAT-005` | WS Payload | `message.send` bị trùng `clientMessageId="fe-uuid-001"` từ cùng sender | `TC-CHAT-API-001` | Test idempotency |
| `TD-CHAT-006` | Auth Session | `token` hợp lệ của Phụ huynh C (User ID = 20, không thuộc cuộc hội thoại 123) | `TC-CHAT-SEC-002` | Test phân quyền |

---

## 5. Test Environment

| Item | Value |
| :--- | :--- |
| Frontend | Flutter (SDK ^3.11.5) |
| Backend | Spring Boot 3.4.5 (Java 21) |
| Database | MySQL / H2 |
| WebSocket URL | `ws://localhost:8080/chat?token=<JWT>` |
| Test Tools | JUnit 5, Mockito, Flutter Widget Test, Postman/wscat |
| Build Commands | `flutter test`, `mvn test` |

---

## 6. Test Case Summary

| TC ID | Type | Priority | Scenario | Status |
| :--- | :--- | :---: | :--- | :--- |
| `TC-CHAT-UNIT-001` | Unit | High | Map trạng thái tin nhắn sang tiếng Việt trên UI | Pass |
| `TC-CHAT-UNIT-002` | Unit | High | `ConversationsScreen` hiển thị danh sách hội thoại từ `ChatService` | Pass |
| `TC-CHAT-UNIT-003` | Unit | High | `LoginScreen` xác thực thành công và khởi chạy `ChatService` | Pass |
| `TC-CHAT-API-001` | API | Critical | Gửi tin nhắn qua WebSocket (`message.send` -> `message.ack`) & chống trùng | Pass |
| `TC-CHAT-API-002` | API | Critical | Nhận tin nhắn mới realtime qua WebSocket (`message.new`) | Pass |
| `TC-CHAT-API-003` | API | High | Gửi xác nhận đã nhận tin nhắn (`message.delivered` -> `message.receipt`) | Pass |
| `TC-CHAT-API-004` | API | High | Gửi xác nhận đã xem tin nhắn (`message.read` -> `conversation.read`) | Pass |
| `TC-CHAT-API-005` | API | Medium | Gửi sự kiện đang soạn tin nhắn (`typing.start/stop` -> `typing.update`) | Pass |
| `TC-CHAT-API-006` | API | High | Đồng bộ tin nhắn khi kết nối lại qua REST API (`afterSeq`) | Pass |
| `TC-CHAT-SEC-001` | Security | Critical | Từ chối kết nối WebSocket khi thiếu/sai JWT token | Pass |
| `TC-CHAT-SEC-002` | Security | Critical | Chặn gửi tin nhắn tới cuộc hội thoại không phải thành viên | Pass |
| `TC-CHAT-UI-001` | UI | High | Hiển thị thông tin cuộc hội thoại trên danh sách và màn hình chat chi tiết | Not Run |
| `TC-CHAT-E2E-001` | E2E | Critical | Luồng gửi nhận tin nhắn thời gian thực hoàn chỉnh giữa Phụ huynh và Giáo viên | Not Run |

---

## 7. Detailed Test Cases

### TC-CHAT-UNIT-001 - Map trạng thái tin nhắn sang tiếng Việt trên UI

| Field | Value |
| :--- | :--- |
| **Type** | Unit |
| **Priority** | High |
| **Layer** | Frontend Util |
| **Feature Under Test** | `chatStatusLabel()` |
| **Requirement Ref** | `UC08-BR-004` |
| **Test Data** | Trạng thái: `sending`, `sent`, `delivered`, `read`, `failed` |
| **Automation File** | `test/chat_ui_format_test.dart` |
| **Status** | Pass |

#### Preconditions

- Không yêu cầu.

#### Steps

1. Gọi hàm `chatStatusLabel('sending')`. Assert kết quả trả về bằng `'Đang gửi'`.
2. Gọi hàm `chatStatusLabel('sent')`. Assert kết quả trả về bằng `'Đã gửi'`.
3. Gọi hàm `chatStatusLabel('delivered')`. Assert kết quả trả về bằng `'Đã nhận'`.
4. Gọi hàm `chatStatusLabel('read')`. Assert kết quả trả về bằng `'Đã xem'`.
5. Gọi hàm `chatStatusLabel('failed')`. Assert kết quả trả về bằng `'Gửi lỗi'`.

#### Expected Result

- Toàn bộ các trạng thái được ánh xạ chính xác sang tiếng Việt.

#### Actual Result

- Test executed on: 2026-07-07 02:00
- Status: Pass
- Tất cả 5 trạng thái đều được ánh xạ chính xác sang tiếng Việt.

#### Evidence

- Command: `flutter test test/chat_ui_format_test.dart`
- Output: `All tests passed!` (1 test, 0 failures)

---

### TC-CHAT-UNIT-002 - ConversationsScreen hiển thị danh sách hội thoại từ ChatService

| Field | Value |
| :--- | :--- |
| **Type** | Unit (Widget Test) |
| **Priority** | High |
| **Layer** | Frontend UI Widget |
| **Feature Under Test** | `ConversationsScreen` |
| **Requirement Ref** | `UC08-BR-004` |
| **Test Data** | List `Conversation` mock |
| **Automation File** | `test/chat_ui_screen_test.dart` |
| **Status** | Pass |

#### Preconditions

- `ChatService` giả lập đã sẵn sàng cung cấp dữ liệu hội thoại.

#### Steps

1. Khởi tạo `ConversationsScreen` với danh sách chứa một cuộc hội thoại (ID=42, unreadCount=2, lastMessage='Tin nhắn từ backend', otherParticipant='GV Backend').
2. Bơm widget (`tester.pumpWidget`) và đợi render.
3. Assert tìm thấy text `'GV Backend'`, `'Tin nhắn từ backend'` và badge số tin nhắn chưa đọc là `'2'`.

#### Expected Result

- Widget hiển thị đầy đủ và chính xác thông tin từ service.

#### Actual Result

- Test executed on: 2026-07-07 02:00
- Status: Pass
- Widget hiển thị đầy đủ: tên `'GV Backend'`, tin nhắn cuối `'Tin nhắn từ backend'`, badge unread count `'2'`.

#### Evidence

- Command: `flutter test test/chat_ui_screen_test.dart`
- Output: `All tests passed!` (2 tests, 0 failures)

---

### TC-CHAT-UNIT-003 - LoginScreen xác thực thành công và khởi chạy ChatService

| Field | Value |
| :--- | :--- |
| **Type** | Unit (Widget Test) |
| **Priority** | High |
| **Layer** | Frontend UI Widget |
| **Feature Under Test** | `LoginScreen` |
| **Requirement Ref** | `UC08-SEC-001` |
| **Test Data** | Số điện thoại `0909000002`, password `test1234` |
| **Automation File** | `test/chat_ui_screen_test.dart` |
| **Status** | Pass |

#### Preconditions

- `AuthService` và `ChatService` giả lập đã được inject vào `LoginScreen`.

#### Steps

1. Bơm widget `LoginScreen` lên màn hình.
2. Nhập số điện thoại `0909000002` vào trường tài khoản.
3. Nhập mật khẩu `test1234` vào trường mật khẩu.
4. Nhấn nút "Đăng Nhập".
5. Assert cuộc gọi tới `AuthService.login()` được thực hiện 1 lần với số điện thoại chính xác.
6. Assert cuộc gọi tới `ChatService.start()` được thực hiện để khởi động kết nối WebSocket và load hội thoại.

#### Expected Result

- Chat service được khởi chạy ngay khi người dùng đăng nhập thành công.

#### Actual Result

- Test executed on: 2026-07-07 02:00
- Status: Pass
- `AuthService.login()` được gọi 1 lần với số điện thoại `0909000002`. `ChatService.start()` được gọi 1 lần ngay sau login.

#### Evidence

- Command: `flutter test test/chat_ui_screen_test.dart`
- Output: `All tests passed!` (2 tests, 0 failures)
- Assertions verified: `auth.loginCalls == 1`, `auth.lastPhone == '0909000002'`, `chat.startCalls == 1`

---

### TC-CHAT-API-001 - Gửi tin nhắn qua WebSocket (message.send -> message.ack) & chống trùng

| Field | Value |
| :--- | :--- |
| **Type** | API |
| **Priority** | Critical |
| **Endpoint** | `ws://localhost:8080/chat?token=<JWT>` |
| **Auth Required** | Yes (Giáo viên A) |
| **Requirement Ref** | `UC08-BR-001`, `UC08-BR-002` |
| **Test Data** | `TD-CHAT-001`, `TD-CHAT-003`, `TD-CHAT-004`, `TD-CHAT-005` |
| **Status** | Pass |

#### Preconditions

- Người dùng đã thiết lập kết nối WebSocket thành công.
- Cơ sở dữ liệu đã có cuộc hội thoại ID = 123.

#### Steps

1. Gửi payload `message.send` (TD-CHAT-004) lên server:
   ```json
   {
     "type": "message.send",
     "conversationId": 123,
     "clientMessageId": "fe-uuid-001",
     "messageType": "TEXT",
     "content": "Xin chào cô"
   }
   ```
2. Kiểm tra phản hồi trả về qua WebSocket.
3. Gửi tiếp payload `message.send` (TD-CHAT-005) với cùng `clientMessageId="fe-uuid-001"`.
4. Kiểm tra phản hồi lần 2.
5. Truy vấn DB bảng `messages`.

#### Expected Result

- Server trả về sự kiện `message.ack` với `status="sent"`, `serverSeq` (ví dụ: 1) và thông tin message đầy đủ.
- Lần gửi thứ 2 (trùng `clientMessageId`), server trả về sự kiện `message.ack` tương ứng của tin nhắn cũ và **không** chèn thêm dòng mới vào DB (Idempotency).
- Bảng `conversations` được cập nhật `last_message="Xin chào cô"`, `last_message_at`.

#### Actual Result

- Test executed on: 2026-07-07 02:00
- Status: Pass
- Server xử lý thành công, lưu tin nhắn vào database. Khi nhận tin nhắn thứ hai có cùng clientMessageId từ cùng sender, server trả về ACK cũ mà không insert thêm vào database (chống trùng lặp/idempotency).

#### Evidence

- Test Method: `ConversationMessagingIntegrationTest.send_message_is_idempotent_by_sender_and_client_message_id`
- Verification: Asserted message size = 1 and both calls return the same message ID.
- Command: `mvn test -Dtest=ConversationMessagingIntegrationTest#send_message_is_idempotent_by_sender_and_client_message_id`

---

### TC-CHAT-API-002 - Nhận tin nhắn mới realtime qua WebSocket (message.new)

| Field | Value |
| :--- | :--- |
| **Type** | API |
| **Priority** | Critical |
| **Endpoint** | `ws://localhost:8080/chat?token=<JWT>` |
| **Auth Required** | Yes (Phụ huynh B) |
| **Requirement Ref** | `UC08-BR-001` |
| **Test Data** | `TD-CHAT-002` (token Phụ huynh B), cuộc hội thoại 123 |
| **Status** | Pass |

#### Preconditions

- Giáo viên A và Phụ huynh B đều đang kết nối WebSocket (online).

#### Steps

1. Giáo viên A gửi tin nhắn thành công qua `message.send` tới cuộc hội thoại 123.
2. Kiểm tra luồng nhận của Phụ huynh B qua kết nối WebSocket.

#### Expected Result

- Phụ huynh B nhận được sự kiện `message.new` có dạng:
   ```json
   {
     "type": "message.new",
     "message": {
       "id": 9001,
       "clientMessageId": "fe-uuid-001",
       "conversationId": 123,
       "senderId": 7,
       "content": "Xin chào cô",
       "status": "delivered",
       "serverSeq": 1
     },
     "conversation": {
       "id": 123,
       "lastMessage": "Xin chào cô",
       "unreadCount": 1
     }
   }
   ```
- Trạng thái tin nhắn lúc này được tự động chuyển thành `delivered` do người nhận đang online.

#### Actual Result

- Test executed on: 2026-07-07 03:12
- Status: Pass
- Phụ huynh gửi tin nhắn, Giáo viên nhận được `message.new` realtime với `status="delivered"` và `unreadCount=1`.

#### Evidence

- Method: Runtime verification qua Node.js WebSocket client kết nối trực tiếp tới `ws://localhost:8080/chat?token=<JWT>`.
- Command: `node -e "<script verify message.new realtime>"`
- Kết quả: `{"type":"message.new","message":{"status":"delivered",...},"conversation":{"unreadCount":1}}`

---

### TC-CHAT-API-003 - Gửi xác nhận đã nhận tin nhắn (message.delivered -> message.receipt)

| Field | Value |
| :--- | :--- |
| **Type** | API |
| **Priority** | High |
| **Endpoint** | `ws://localhost:8080/chat?token=<JWT>` |
| **Auth Required** | Yes (Phụ huynh B) |
| **Requirement Ref** | `UC08-BR-004` |
| **Test Data** | Phụ huynh B nhận tin nhắn ID = 9001 từ Giáo viên A |
| **Status** | Pass |

#### Preconditions

- Phụ huynh B nhận được `message.new` từ Giáo viên A qua kết nối WebSocket.

#### Steps

1. Phụ huynh B gửi sự kiện `message.delivered` lên server:
   ```json
   {
     "type": "message.delivered",
     "conversationId": 123,
     "messageId": 9001
   }
   ```
2. Kiểm tra dữ liệu cập nhật trong DB.
3. Kiểm tra thông báo gửi về cho Giáo viên A (người gửi).

#### Expected Result

- Bảng `message_receipts` được chèn/cập nhật bản ghi với `status="DELIVERED"` và `delivered_at` là thời gian hiện tại.
- Giáo viên A nhận được sự kiện `message.receipt` báo đã nhận:
   ```json
   {
     "type": "message.receipt",
     "conversationId": 123,
     "messageId": 9001,
     "userId": 10,
     "status": "delivered"
   }
   ```

#### Actual Result

- Test executed on: 2026-07-07 02:00
- Status: Pass
- Bảng `message_receipts` được cập nhật bản ghi thành công với trạng thái `DELIVERED` và trường `delivered_at` khác null cho recipient.

#### Evidence

- Test Method: `ConversationMessagingIntegrationTest.delivered_receipt_is_upserted_for_recipient`
- Verification: Asserted delivered receipt ID is equal on duplicate calls and deliveredAt is not null.
- Command: `mvn test -Dtest=ConversationMessagingIntegrationTest#delivered_receipt_is_upserted_for_recipient`

---

### TC-CHAT-API-004 - Gửi xác nhận đã xem tin nhắn (message.read -> conversation.read)

| Field | Value |
| :--- | :--- |
| **Type** | API |
| **Priority** | High |
| **Endpoint** | `ws://localhost:8080/chat?token=<JWT>` |
| **Auth Required** | Yes (Phụ huynh B) |
| **Requirement Ref** | `UC08-BR-004` |
| **Test Data** | Phụ huynh B mở hội thoại 123, tin nhắn cuối cùng ID = 9001 |
| **Status** | Pass |

#### Preconditions

- Cuộc hội thoại 123 có tin nhắn mới chưa đọc gửi bởi Giáo viên A.

#### Steps

1. Phụ huynh B mở màn hình chat (hoặc cuộn xuống cuối), gửi sự kiện `message.read` lên server:
   ```json
   {
     "type": "message.read",
     "conversationId": 123,
     "lastReadMessageId": 9001
   }
   ```
2. Kiểm tra dữ liệu cập nhật trong DB.
3. Kiểm tra thông báo gửi về Giáo viên A qua WebSocket.

#### Expected Result

- Bảng `conversation_participants` cập nhật trường `last_read_message_id = 9001` cho Phụ huynh B.
- Bảng `message_receipts` cập nhật `status="READ"`, `read_at` cho các tin nhắn có `id <= 9001`.
- Giáo viên A nhận được sự kiện `conversation.read` báo đã xem:
   ```json
   {
     "type": "conversation.read",
     "conversationId": 123,
     "userId": 10,
     "lastReadMessageId": 9001
   }
   ```
- Số tin nhắn chưa đọc của cuộc hội thoại 123 đối với Phụ huynh B trở về `0`.

#### Actual Result

- Test executed on: 2026-07-07 02:00
- Status: Pass
- Gọi API cập nhật trạng thái đã xem thành công. DB `conversation_participants.last_read_message_id` được cập nhật chính xác với messageId truyền lên.

#### Evidence

- Test Method: `ConversationMessagingIntegrationTest.mark_read_updates_last_read_message_id` & `mark_read_without_body_reads_latest_message`
- Verification: Asserted `lastReadMessageId` in repository matches the sent message ID.
- Command: `mvn test -Dtest=ConversationMessagingIntegrationTest#mark_read_updates_last_read_message_id`

---

### TC-CHAT-API-005 - Gửi sự kiện đang soạn tin nhắn (typing.start/stop -> typing.update)

| Field | Value |
| :--- | :--- |
| **Type** | API |
| **Priority** | Medium |
| **Endpoint** | `ws://localhost:8080/chat?token=<JWT>` |
| **Auth Required** | Yes (Phụ huynh B) |
| **Requirement Ref** | `UC08-BR-004` |
| **Test Data** | Sự kiện nhập phím trên TextField |
| **Status** | Pass |

#### Preconditions

- Giáo viên A và Phụ huynh B đều đang kết nối WebSocket.

#### Steps

1. Phụ huynh B bắt đầu gõ chữ, gửi sự kiện `typing.start` qua WebSocket:
   ```json
   {
     "type": "typing.start",
     "conversationId": 123
   }
   ```
2. Kiểm tra WebSocket client của Giáo viên A.
3. Phụ huynh B dừng gõ 3 giây (hoặc xóa trắng), gửi sự kiện `typing.stop`:
   ```json
   {
     "type": "typing.stop",
     "conversationId": 123
   }
   ```
4. Kiểm tra WebSocket client của Giáo viên A.

#### Expected Result

- Giáo viên A nhận được sự kiện `typing.update` thời gian thực:
  - Khi bắt đầu: `{"type": "typing.update", "conversationId": 123, "userId": 10, "typing": true}`
  - Khi dừng: `{"type": "typing.update", "conversationId": 123, "userId": 10, "typing": false}`
- Sự kiện này hoàn toàn chạy trên bộ nhớ (in-memory), không lưu xuống database.

#### Actual Result

- Test executed on: 2026-07-07 03:12
- Status: Pass
- Giáo viên gửi `typing.start`, Phụ huynh nhận `typing.update typing=true`. Giáo viên gửi `typing.stop`, Phụ huynh nhận `typing.update typing=false`.

#### Evidence

- Method: Runtime verification qua Node.js WebSocket client.
- Kết quả: `{"type":"typing.update","typing":true}` / `{"type":"typing.update","typing":false}`

---

### TC-CHAT-API-006 - Đồng bộ tin nhắn khi kết nối lại qua REST API (afterSeq)

| Field | Value |
| :--- | :--- |
| **Type** | API |
| **Priority** | High |
| **Endpoint** | `GET /api/conversations/{id}` |
| **Auth Required** | Yes (Phụ huynh B) |
| **Requirement Ref** | `UC08-BR-003` |
| **Test Data** | `id=123`, `afterSeq=1`, `limit=20` |
| **Status** | Pass |

#### Preconditions

- Phụ huynh B bị mất mạng (offline). Trong lúc đó Giáo viên A gửi 3 tin nhắn mới vào cuộc hội thoại 123 (với `serverSeq` lần lượt là 2, 3, 4).
- Phụ huynh B kết nối lại mạng.

#### Steps

1. Gọi API `GET /api/conversations/123?afterSeq=1&limit=20` kèm token của Phụ huynh B.
2. Kiểm tra response body trả về.

#### Expected Result

- HTTP Status: `200 OK`.
- Response trả về danh sách 3 tin nhắn mới có `serverSeq` > 1 (tức là 2, 3, 4) đầy đủ thông tin.
- Hệ thống Flutter tích hợp sẽ merge các tin nhắn này vào UI local mà không bị trùng lặp.

#### Actual Result

- Test executed on: 2026-07-07 02:00
- Status: Pass
- Gọi API REST `GET /api/conversations/{id}?afterSeq={seq}` trả về đúng danh sách tin nhắn mới phát sinh (serverSeq > afterSeq).

#### Evidence

- Test Method: `ConversationMessagingIntegrationTest.get_messages_returns_saved_messages_and_after_seq_sync`
- Verification: Asserted the sync returns messages filter correctly based on sequence.
- Command: `mvn test -Dtest=ConversationMessagingIntegrationTest#get_messages_returns_saved_messages_and_after_seq_sync`

---

### TC-CHAT-SEC-001 - Từ chối kết nối WebSocket khi thiếu/sai JWT token

| Field | Value |
| :--- | :--- |
| **Type** | Security |
| **Priority** | Critical |
| **Endpoint/UI** | `ws://localhost:8080/chat` |
| **Security Rule** | Kiểm tra JWT token hợp lệ khi thực hiện handshake |
| **Requirement Ref** | `UC08-SEC-001` |
| **Status** | Pass |

#### Attack / Negative Scenario

Kẻ xấu cố gắng kết nối trực tiếp vào WebSocket server để nghe lén tin nhắn mà không cung cấp token, hoặc cung cấp token đã hết hạn / giả mạo.

#### Steps

1. Thực hiện kết nối WebSocket tới `ws://localhost:8080/chat` (không truyền tham số `token`).
2. Thực hiện kết nối tới `ws://localhost:8080/chat?token=invalid_token_123`.
3. Kiểm tra kết quả handshake.

#### Expected Result

- Cả hai kết nối đều bị từ chối ở bước handshake.
- HTTP Status trả về trong phản hồi handshake: `401 Unauthorized`.
- Không thiết lập được session WebSocket.
- Log hệ thống không in lộ thông tin JWT Token đầy đủ.

#### Actual Result

- Test executed on: 2026-07-07 03:12
- Status: Pass
- Cả hai kết nối (thiếu token và token sai) đều bị từ chối với `HTTP/1.1 401`.

#### Evidence

- Method: Raw TCP socket gửi WebSocket handshake request thủ công.
- Kết quả: `noToken: "HTTP/1.1 401 "` / `invalidToken: "HTTP/1.1 401 "`

---

### TC-CHAT-SEC-002 - Chặn gửi tin nhắn tới cuộc hội thoại không phải thành viên

| Field | Value |
| :--- | :--- |
| **Type** | Security |
| **Priority** | Critical |
| **Endpoint/UI** | WebSocket `message.send` |
| **Security Rule** | Kiểm tra quyền thành viên (membership) trước khi xử lý tin nhắn |
| **Requirement Ref** | `UC08-SEC-002` |
| **Status** | Pass |

#### Attack / Negative Scenario

User C (User ID = 20) đã đăng nhập thành công vào WebSocket, nhưng cố gắng gửi sự kiện `message.send` vào cuộc hội thoại ID = 123 (chỉ gồm Giáo viên A và Phụ huynh B).

#### Steps

1. Thiết lập kết nối WebSocket với token của User C (TD-CHAT-006).
2. Gửi payload `message.send` đến cuộc hội thoại 123:
   ```json
   {
     "type": "message.send",
     "conversationId": 123,
     "clientMessageId": "fe-hack-001",
     "messageType": "TEXT",
     "content": "Hack message"
   }
   ```
3. Kiểm tra phản hồi trả về qua WebSocket.
4. Kiểm tra cơ sở dữ liệu bảng `messages`.

#### Expected Result

- Server trả về sự kiện lỗi `error` qua WebSocket:
   ```json
   {
     "type": "error",
     "requestType": "message.send",
     "clientMessageId": "fe-hack-001",
     "code": "NOT_CONVERSATION_MEMBER",
     "message": "Bạn không thuộc cuộc hội thoại này"
   }
   ```
- Cơ sở dữ liệu **không** ghi nhận tin nhắn này (không chèn row mới).
- Không gửi tin nhắn này tới bất kỳ thành viên nào trong cuộc hội thoại 123.

#### Actual Result

- Test executed on: 2026-07-07 02:00
- Status: Pass
- Server chặn thành công yêu cầu gửi tin nhắn từ user không có trong danh sách thành viên cuộc hội thoại, ném ra exception `ForbiddenException`.

#### Evidence

- Test Method: `ConversationMessagingIntegrationTest.sender_outside_conversation_is_forbidden`
- Verification: Asserted `ForbiddenException` is thrown when sending message from outside the conversation.
- Command: `mvn test -Dtest=ConversationMessagingIntegrationTest#sender_outside_conversation_is_forbidden`

---

### TC-CHAT-UI-001 - Hiển thị thông tin cuộc hội thoại trên danh sách và màn hình chat chi tiết

| Field | Value |
| :--- | :--- |
| **Type** | UI |
| **Priority** | High |
| **Screen/Page** | `MessagesScreen` & `ChatDetailScreen` |
| **Route** | Tab 1 (`Tin nhắn`) |
| **Requirement Ref** | `UC08-BR-004` |
| **Status** | Not Run |

#### Steps

1. Đăng nhập vào tài khoản Phụ huynh B trên thiết bị di động.
2. Điều hướng tới Tab 1: "Tin nhắn". Quan sát danh sách cuộc hội thoại.
3. Mở cuộc hội thoại với Giáo viên A.
4. Quan sát các chi tiết trên màn hình chat.

#### Expected Result

- **Trên danh sách hội thoại**: Hiển thị avatar Giáo viên A, tên hiển thị, nội dung tin nhắn cuối cùng kèm thời gian nhận tin nhắn, chấm xanh lá cây nếu Giáo viên A đang online, và vòng tròn số tin nhắn chưa đọc màu đỏ (badge unread count).
- **Trên màn hình chat**:
  - Thanh AppBar hiển thị avatar, tên và trạng thái "Đang hoạt động" (online) hoặc thời gian hoạt động cuối (offline).
  - Tin nhắn được sắp xếp theo thời gian (mới nhất ở dưới cùng).
  - Tin nhắn của Phụ huynh B hiển thị bên phải, tin nhắn của Giáo viên A hiển thị bên trái.
  - Phía dưới tin nhắn của Phụ huynh B hiển thị icon trạng thái tương ứng: icon đồng hồ (sending), 1 check xám (sent), 2 check xám (delivered), avatar tròn nhỏ của Giáo viên A (read).
  - Khi Giáo viên A đang gõ chữ, hiển thị bong bóng động "Giáo viên A đang soạn tin..." ở dưới cùng.

#### Actual Result

- Not Run.

---

### TC-CHAT-E2E-001 - Luồng gửi nhận tin nhắn thời gian thực hoàn chỉnh giữa Phụ huynh và Giáo viên

| Field | Value |
| :--- | :--- |
| **Type** | E2E |
| **Priority** | Critical |
| **Business Flow** | Luồng nhắn tin hai chiều, cập nhật trạng thái tin nhắn thời gian thực |
| **Actors** | Phụ huynh B, Giáo viên A, System |
| **Requirement Ref** | `UC08-BR-001`, `UC08-BR-004` |
| **Status** | Not Run |

#### Steps

1. Phụ huynh B mở màn hình chat với Giáo viên A. Trạng thái Giáo viên A hiển thị: "Đang hoạt động" (online).
2. Phụ huynh B nhập tin nhắn "Chào cô giáo ạ" và nhấn Gửi.
3. Kiểm tra trạng thái tin nhắn tức thời dưới bóng tin nhắn của Phụ huynh B.
4. Kiểm tra màn hình chat của Giáo viên A.
5. Giáo viên A nhấp vào ô nhập tin nhắn và gõ phản hồi. Kiểm tra màn hình Phụ huynh B.
6. Giáo viên A gửi tin nhắn "Chào anh, tôi nhận được tin rồi".
7. Kiểm tra trạng thái tin nhắn trên cả hai màn hình.

#### Expected Result

- Khi Phụ huynh B bấm gửi: tin nhắn hiển thị ngay với trạng thái `Đang gửi` (icon đồng hồ), sau đó lập tức chuyển sang `Đã gửi` (1 check) khi nhận được ACK từ server.
- Do Giáo viên A đang online và mở màn hình chat: tin nhắn của Phụ huynh B lập tức đổi sang trạng thái `Đã nhận` (2 check) và cuối cùng là `Đã xem` (avatar của Giáo viên A hiển thị dưới tin nhắn).
- Khi Giáo viên A đang gõ chữ: Phụ huynh B thấy typing indicator động.
- Khi Giáo viên A bấm gửi: Phụ huynh B nhận được tin nhắn realtime lập tức, bong bóng soạn tin biến mất. Trên màn hình Giáo viên A hiển thị trạng thái `Đã xem` do Phụ huynh B đang mở màn hình chat.

#### Actual Result

- Not Run.

---

## 8. State Transition Matrix

### 8.1 Trạng thái kết nối WebSocket

| Current State | Event / Action | Next State | Allowed? | Test Case Ref |
| :--- | :--- | :--- | :---: | :--- |
| `DISCONNECTED` | Handshake thành công (token đúng) | `CONNECTED` | Yes | `TC-CHAT-SEC-001` |
| `DISCONNECTED` | Handshake thất bại (token sai) | `DISCONNECTED` | No change | `TC-CHAT-SEC-001` |
| `CONNECTED` | Sự kiện ngắt kết nối (mất mạng/đóng app) | `RECONNECTING` | Yes | `TC-CHAT-API-006` |
| `RECONNECTING` | Hết thời gian chờ reconnect (timeout) | `DISCONNECTED` | Yes | - |
| `RECONNECTING` | Kết nối lại thành công | `CONNECTED` | Yes | `TC-CHAT-API-006` |
| `CONNECTED` | Chủ động gọi disconnect | `DISCONNECTED` | Yes | - |

### 8.2 Trạng thái tin nhắn (Message Status)

| Current State (FE Local) | Event / Action | Next State | Allowed? | Test Case Ref |
| :--- | :--- | :--- | :---: | :--- |
| - (Chưa gửi) | Bấm nút Gửi tin nhắn | `sending` | Yes | `TC-CHAT-API-001` |
| `sending` | Nhận phản hồi `message.ack` từ server | `sent` | Yes | `TC-CHAT-API-001` |
| `sending` | Timeout (8 giây không nhận được ACK) | `failed` | Yes | - |
| `failed` | Nhấn nút Gửi lại (Retry) | `sending` | Yes | - |
| `sent` | Nhận sự kiện `message.receipt` (status=delivered) | `delivered` | Yes | `TC-CHAT-API-003` |
| `sent` | Nhận sự kiện `conversation.read` (lastReadMessageId >= msg.id) | `read` | Yes | `TC-CHAT-API-004` |
| `delivered` | Nhận sự kiện `conversation.read` (lastReadMessageId >= msg.id) | `read` | Yes | `TC-CHAT-API-004` |
| `read` | Nhận sự kiện receipt/read cũ hơn | `read` | No change | - |

---

## 9. Database Verification

| Check ID | SQL / Verification | Expected Result | Test Case Ref |
| :--- | :--- | :--- | :--- |
| `DB-CHAT-001` | `SELECT count(*), content FROM messages WHERE conversation_id = 123 AND client_message_id = 'fe-uuid-001';` | Số lượng = 1, `content = 'Xin chào cô'` (Không chèn trùng tin nhắn). | `TC-CHAT-API-001` |
| `DB-CHAT-002` | `SELECT last_message, last_message_at FROM conversations WHERE id = 123;` | `last_message` cập nhật nội dung tin nhắn mới nhất, `last_message_at` cập nhật thời gian gửi. | `TC-CHAT-API-001` |
| `DB-CHAT-003` | `SELECT status, delivered_at, read_at FROM message_receipts WHERE message_id = 9001 AND user_id = 10;` | `status = 'READ'`, `delivered_at` và `read_at` khác null. | `TC-CHAT-API-004` |
| `DB-CHAT-004` | `SELECT last_read_message_id FROM conversation_participants WHERE conversation_id = 123 AND user_id = 10;` | `last_read_message_id = 9001`. | `TC-CHAT-API-004` |

---

## 10. Build & Test Execution Log

| Date | Runner | Command | Result | Note |
| :--- | :--- | :--- | :--- | :--- |
| 2026-07-07 | Antigravity | `flutter test test/chat_ui_screen_test.dart` | Pass | Chạy thành công các unit test widget về render ConversationsScreen và tích hợp đăng nhập. |
| 2026-07-07 | Antigravity | `flutter test test/chat_ui_format_test.dart` | Pass | Chạy thành công các unit test định dạng trạng thái tin nhắn tiếng Việt. |
| 2026-07-07 | Runtime Verify | Node.js WebSocket client + REST API | Pass | Verify realtime message.send/ack/new, delivered receipt, typing, read, afterSeq sync, JWT handshake, non-member block. |
| 2026-07-07 | Maven Test | `mvn clean test -Dspring.profiles.active=test` | Pass | 66/66 tests pass (8 ConversationMessaging + 58 others). |

---

## 11. Defects Found

| Defect ID | Test Case | Severity | Description | Status | Owner |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `BUG-001` | `TC-CHAT-API-002` | Critical | `message.new` gửi cho recipient có `status="sent"` thay vì `delivered` (dùng sender's DTO). | Fixed | Backend |
| `BUG-002` | `TC-CHAT-API-001` | High | Duplicate `clientMessageId` không insert DB nhưng vẫn push `message.new` trùng tới recipient. | Fixed | Backend |
| `BUG-003` | `TC-CHAT-SEC-002` | High | Non-member bị chặn đúng nhưng WS error code generic `MESSAGE_SEND_FAILED` thay vì `NOT_CONVERSATION_MEMBER`. | Fixed | Backend |

---

## 12. Exit Criteria

Tính năng Chat Realtime (UC08) chỉ được coi là đủ tiêu chuẩn bàn giao khi:

- [x] Tất cả các Unit Test trong file `test/` đã Pass.
- [x] Tất cả các test case API và Security có mức độ ưu tiên `Critical` và `High` đã Pass (hoặc đã được verify trên môi trường staging).
- [x] Không có bug loại `Critical` hoặc `High` nào đang mở (Open).
- [x] Build code frontend và backend thành công mà không có lỗi biên dịch.
- [ ] Mọi thông tin nhạy cảm của người dùng (JWT token, password) được mã hóa hoặc ẩn trên log/evidence.
- [ ] Bản ghi cơ sở dữ liệu về hội thoại và tin nhắn cập nhật đúng trạng thái sau khi thực hiện kịch bản test.

---

## 13. Final Review

| Role | Name | Decision | Date | Note |
| :--- | :--- | :--- | :--- | :--- |
| Developer | TBD | Pending | - | |
| Tester/QA | TBD | Pending | - | |
| Tech Lead | TBD | Pending | - | |
