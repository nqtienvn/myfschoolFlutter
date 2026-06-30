# WebSocket Messaging Implementation Plan

**Goal:** Hoàn thiện module chat realtime kiểu Facebook/Messenger cho MyFschool, bắt đầu từ backend WebSocket đã có sẵn, sau đó nối Flutter app theo từng phase có thể test độc lập.

**Architecture:** Backend dùng Spring Boot WebSocket native endpoint `/chat`, JWT xác thực khi handshake, service transaction ngắn để lưu message vào MySQL, rồi ACK/push realtime qua `WebSocketSessionManager`. Flutter app giữ trạng thái `sending` local, nhận `message.ack` để đổi sang `sent`, nhận receipt events để đổi sang `delivered/read`, và sync lại bằng REST khi reconnect.

**Tech Stack:** Spring Boot 3.4.5, Java 21, Spring WebSocket, Spring Security JWT, Spring Data JPA, MySQL/H2 test, Flutter/Dart SDK ^3.11.5.

## Global Constraints

- Flutter không kết nối trực tiếp MySQL; Flutter chỉ gọi Backend API/WebSocket.
- UI text/comment dùng tiếng Việt; code identifiers dùng tiếng Anh.
- Trạng thái `sending` chỉ nằm ở FE, không lưu DB.
- Backend chỉ trả `sent` sau khi DB transaction commit thành công.
- Online realtime không update MySQL liên tục; phase đầu dùng memory session, phase tối ưu dùng Redis TTL.
- Critical path gửi tin nhắn phải ngắn: validate membership -> insert DB -> update conversation -> commit -> ACK sender -> push recipient.
- Không đưa push notification, search indexing, analytics, thumbnail/file processing vào critical path <200ms.

---

## 0. Hiện trạng code đã có

### Backend đã có

- `backend/src/main/java/vn/edu/fpt/myfschool/config/WebSocketConfig.java`
  - Đăng ký endpoint `/chat`.
  - Đang tự `new WebSocketSessionManager()` trong `chatHandler()`, nên dễ lệch bean singleton.
- `backend/src/main/java/vn/edu/fpt/myfschool/websocket/ChatWebSocketHandler.java`
  - Đã add/remove session khi connect/disconnect.
  - `handleTextMessage` còn trống.
- `backend/src/main/java/vn/edu/fpt/myfschool/websocket/WebSocketSessionManager.java`
  - Đã map `userId -> Set<WebSocketSession>`.
  - Có `sendToUser()` và `isOnline()`.
- `backend/src/main/java/vn/edu/fpt/myfschool/entity/Conversation.java`
  - Có `lastMessage`, `lastMessageAt`, `participants`, `messages`.
- `backend/src/main/java/vn/edu/fpt/myfschool/entity/ConversationParticipant.java`
  - Có `joinedAt`, `lastReadAt`.
- `backend/src/main/java/vn/edu/fpt/myfschool/entity/Message.java`
  - Có `conversation`, `sender`, `content`, `attachments`.
- `backend/src/main/java/vn/edu/fpt/myfschool/service/ConversationService.java`
  - Có `sendMessage()` nhưng chưa dùng trong WebSocket.
  - Chưa check sender có thuộc conversation không trong `sendMessage()`.
  - `markAsRead()` đang dùng `lastReadAt` theo thời gian.
- `backend/src/main/java/vn/edu/fpt/myfschool/controller/ConversationController.java`
  - `GET /api/conversations/{id}` hiện đang trả list rỗng nếu conversation tồn tại, chưa gọi `MessageService.getMessages()`.

### Flutter hiện tại

- `lib/vn/edu/fpt/view/screens/messages_screen.dart`
  - Conversation list đang mock static.
- `lib/vn/edu/fpt/view/screens/parent_messages_screen.dart`
  - Popup trả lời nhanh mock.
- `pubspec.yaml`
  - Chưa có dependency WebSocket/HTTP client riêng.

---

## 1. Event contract thống nhất BE <-> FE

### Client -> Server

```json
{
  "type": "message.send",
  "conversationId": 123,
  "clientMessageId": "fe-uuid-001",
  "messageType": "TEXT",
  "content": "Xin chào cô"
}
```

```json
{
  "type": "message.delivered",
  "conversationId": 123,
  "messageId": 9001
}
```

```json
{
  "type": "message.read",
  "conversationId": 123,
  "lastReadMessageId": 9001
}
```

```json
{
  "type": "typing.start",
  "conversationId": 123
}
```

```json
{
  "type": "typing.stop",
  "conversationId": 123
}
```

```json
{
  "type": "presence.heartbeat"
}
```

### Server -> Client

```json
{
  "type": "message.ack",
  "clientMessageId": "fe-uuid-001",
  "status": "sent",
  "message": {
    "id": 9001,
    "clientMessageId": "fe-uuid-001",
    "conversationId": 123,
    "senderId": 10,
    "senderName": "PH Test",
    "senderAvatar": null,
    "messageType": "TEXT",
    "content": "Xin chào cô",
    "serverSeq": 152,
    "createdAt": "2026-06-29T10:00:00",
    "status": "sent"
  }
}
```

```json
{
  "type": "message.new",
  "message": {
    "id": 9001,
    "clientMessageId": "fe-uuid-001",
    "conversationId": 123,
    "senderId": 10,
    "senderName": "PH Test",
    "senderAvatar": null,
    "messageType": "TEXT",
    "content": "Xin chào cô",
    "serverSeq": 152,
    "createdAt": "2026-06-29T10:00:00",
    "status": "delivered"
  },
  "conversation": {
    "id": 123,
    "lastMessage": "Xin chào cô",
    "lastMessageAt": "2026-06-29T10:00:00",
    "unreadCount": 1
  }
}
```

```json
{
  "type": "message.receipt",
  "conversationId": 123,
  "messageId": 9001,
  "userId": 20,
  "status": "delivered",
  "deliveredAt": "2026-06-29T10:00:01"
}
```

```json
{
  "type": "conversation.read",
  "conversationId": 123,
  "userId": 20,
  "lastReadMessageId": 9001,
  "readAt": "2026-06-29T10:00:05"
}
```

```json
{
  "type": "typing.update",
  "conversationId": 123,
  "userId": 20,
  "typing": true
}
```

```json
{
  "type": "presence.update",
  "userId": 20,
  "online": true,
  "lastSeenAt": null
}
```

```json
{
  "type": "error",
  "requestType": "message.send",
  "clientMessageId": "fe-uuid-001",
  "code": "NOT_CONVERSATION_MEMBER",
  "message": "Bạn không thuộc cuộc hội thoại này"
}
```

---

## 2. Status model

| Status | Nằm ở đâu | Khi nào set | Có lưu DB không |
|---|---|---|---|
| `sending` | Flutter local | User bấm gửi, trước khi BE ACK | Không |
| `sent` | BE + Flutter | DB commit message thành công, sender nhận `message.ack` | Có thể suy ra từ `messages.created_at`; không cần receipt riêng cho sender |
| `delivered` | BE + Flutter | Recipient online nhận `message.new` và gửi `message.delivered` | Có, trong `message_receipts.delivered_at` |
| `read` | BE + Flutter | Recipient mở conversation và gửi `message.read` | Có, ưu tiên `conversation_participants.last_read_message_id`; optional `message_receipts.read_at` |
| `failed` | Flutter local | Gửi WS lỗi/timeout ACK | Không |

Rule quan trọng:

```txt
message.send nhận từ FE
  -> validate
  -> insert messages
  -> update conversations
  -> commit
  -> send message.ack status=sent cho sender
  -> push message.new cho recipients online
```

---

## 3. File structure dự kiến

### Backend — create

- `backend/src/main/java/vn/edu/fpt/myfschool/common/enums/MessageType.java`
  - Enum `TEXT`, `IMAGE`, `FILE`, `SYSTEM`.
- `backend/src/main/java/vn/edu/fpt/myfschool/common/enums/MessageReceiptStatus.java`
  - Enum `DELIVERED`, `READ`.
- `backend/src/main/java/vn/edu/fpt/myfschool/entity/MessageReceipt.java`
  - Receipt theo từng message/recipient.
- `backend/src/main/java/vn/edu/fpt/myfschool/repository/MessageReceiptRepository.java`
  - Query/update receipt.
- `backend/src/main/java/vn/edu/fpt/myfschool/websocket/ChatHandshakeInterceptor.java`
  - Lấy JWT từ query/header, validate, set `userId` vào WebSocket session attributes.
- `backend/src/main/java/vn/edu/fpt/myfschool/websocket/dto/ClientWsMessage.java`
  - DTO parse envelope từ FE.
- `backend/src/main/java/vn/edu/fpt/myfschool/websocket/dto/ServerWsMessage.java`
  - DTO envelope trả về FE.
- `backend/src/main/java/vn/edu/fpt/myfschool/websocket/dto/MessageSendPayload.java`
  - Payload cho `message.send`.
- `backend/src/main/java/vn/edu/fpt/myfschool/websocket/dto/MessageDeliveredPayload.java`
  - Payload cho `message.delivered`.
- `backend/src/main/java/vn/edu/fpt/myfschool/websocket/dto/MessageReadPayload.java`
  - Payload cho `message.read`.
- `backend/src/main/java/vn/edu/fpt/myfschool/websocket/dto/TypingPayload.java`
  - Payload cho `typing.start/typing.stop`.
- `backend/src/main/java/vn/edu/fpt/myfschool/service/ChatRealtimeService.java`
  - Điều phối event WebSocket, gọi `ConversationService`, push tới users.
- `backend/src/test/java/vn/edu/fpt/myfschool/ConversationMessagingIntegrationTest.java`
  - Test service/REST chính.
- `backend/src/test/java/vn/edu/fpt/myfschool/WebSocketMessagingIntegrationTest.java`
  - Test WebSocket event chính.

### Backend — modify

- `backend/src/main/java/vn/edu/fpt/myfschool/config/WebSocketConfig.java`
  - Dùng bean injection, add handshake interceptor.
- `backend/src/main/java/vn/edu/fpt/myfschool/websocket/ChatWebSocketHandler.java`
  - Parse message JSON, route theo `type`.
- `backend/src/main/java/vn/edu/fpt/myfschool/websocket/WebSocketSessionManager.java`
  - Thread-safe `computeIfAbsent`, cleanup closed sessions, broadcast helper.
- `backend/src/main/java/vn/edu/fpt/myfschool/entity/Message.java`
  - Thêm `clientMessageId`, `messageType`, `serverSeq`.
- `backend/src/main/java/vn/edu/fpt/myfschool/entity/Conversation.java`
  - Thêm `lastMessageId` hoặc quan hệ `lastMessageEntity` nếu muốn query nhanh.
- `backend/src/main/java/vn/edu/fpt/myfschool/entity/ConversationParticipant.java`
  - Thêm `lastReadMessageId`, `lastSeenAt`.
- `backend/src/main/java/vn/edu/fpt/myfschool/repository/MessageRepository.java`
  - Query by `serverSeq`, idempotency by `senderId + clientMessageId`, max seq.
- `backend/src/main/java/vn/edu/fpt/myfschool/repository/ConversationParticipantRepository.java`
  - Query participants excluding sender.
- `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/MessageDto.java`
  - Trả thêm fields FE cần.
- `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/ConversationDto.java`
  - Trả thêm online/lastSeen/lastMessage object nếu cần.
- `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/SendMessageRequest.java`
  - Thêm `clientMessageId`, `messageType` nếu vẫn dùng REST send.
- `backend/src/main/java/vn/edu/fpt/myfschool/service/ConversationService.java`
  - Validate membership, idempotent send, mark read theo message id.
- `backend/src/main/java/vn/edu/fpt/myfschool/service/MessageService.java`
  - Get messages thật từ DB cho `GET /api/conversations/{id}`.
- `backend/src/main/java/vn/edu/fpt/myfschool/controller/ConversationController.java`
  - Fix endpoint lấy messages.

### Flutter — create later

- `lib/vn/edu/fpt/src/models/chat_message.dart`
- `lib/vn/edu/fpt/src/models/conversation.dart`
- `lib/vn/edu/fpt/src/api/dto/chat_message_dto.dart`
- `lib/vn/edu/fpt/src/api/dto/conversation_dto.dart`
- `lib/vn/edu/fpt/src/api/client/chat_api_client.dart`
- `lib/vn/edu/fpt/src/repositories/chat_repository.dart`
- `lib/vn/edu/fpt/src/services/chat_socket_service.dart`
- `lib/vn/edu/fpt/src/services/chat_service.dart`
- `lib/vn/edu/fpt/view/screens/chat_detail_screen.dart`

### Flutter — modify later

- `pubspec.yaml`
  - Add WebSocket dependency nếu dùng package ngoài. Gợi ý: `web_socket_channel`.
- `lib/vn/edu/fpt/view/screens/messages_screen.dart`
  - Thay mock static bằng conversation list từ service.
- `lib/vn/edu/fpt/src/models/models.dart`, `api.dart`, `repositories.dart`, `services.dart`
  - Export các class chat mới.

---

# PHASE 1 — Backend direct chat realtime cơ bản

## Task 1.1 — Fix WebSocket bean wiring và JWT handshake

**Mục tiêu:** `/chat` nhận JWT, set `userId` vào session, handler và session manager dùng đúng Spring singleton.

**Files:**

- Create: `backend/src/main/java/vn/edu/fpt/myfschool/websocket/ChatHandshakeInterceptor.java`
- Modify: `backend/src/main/java/vn/edu/fpt/myfschool/config/WebSocketConfig.java`
- Modify: `backend/src/main/java/vn/edu/fpt/myfschool/websocket/ChatWebSocketHandler.java`

**Implementation notes:**

`ChatHandshakeInterceptor` nên lấy token theo thứ tự:

1. Query string: `ws://localhost:8080/chat?token=<JWT>` — Flutter dễ dùng nhất.
2. Header: `Authorization: Bearer <JWT>` — tiện cho test/tool.

Pseudo-code:

```java
@Component
@RequiredArgsConstructor
public class ChatHandshakeInterceptor implements HandshakeInterceptor {
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = extractToken(request);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        attributes.put("userId", jwtTokenProvider.getUserIdFromToken(token));
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }
}
```

`WebSocketConfig` đổi sang constructor injection:

```java
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {
    private final ChatWebSocketHandler chatWebSocketHandler;
    private final ChatHandshakeInterceptor chatHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, "/chat")
                .addInterceptors(chatHandshakeInterceptor)
                .setAllowedOrigins("*");
    }
}
```

**Test:**

- Connect không token -> handshake reject.
- Connect token hợp lệ -> `afterConnectionEstablished()` có `userId`.

**Command:**

```bash
cd backend && mvn test -Dtest=WebSocketMessagingIntegrationTest
```

---

## Task 1.2 — Chuẩn hóa session manager

**Mục tiêu:** Quản lý nhiều thiết bị/tab/user an toàn, không leak closed session.

**Files:**

- Modify: `backend/src/main/java/vn/edu/fpt/myfschool/websocket/WebSocketSessionManager.java`

**Implementation notes:**

Thay logic `get -> if null -> put` bằng `computeIfAbsent`:

```java
public void addSession(Long userId, WebSocketSession session) {
    sessions.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(session);
}
```

`sendToUser` nên remove session đã đóng/lỗi:

```java
public void sendToUser(Long userId, String message) {
    Set<WebSocketSession> userSessions = sessions.get(userId);
    if (userSessions == null) return;

    userSessions.removeIf(session -> !session.isOpen());
    for (WebSocketSession session : userSessions) {
        try {
            session.sendMessage(new TextMessage(message));
        } catch (IOException ex) {
            userSessions.remove(session);
        }
    }
    if (userSessions.isEmpty()) sessions.remove(userId);
}
```

Add helper:

```java
public void sendToUsers(Collection<Long> userIds, String message) {
    userIds.forEach(userId -> sendToUser(userId, message));
}

public int getOpenSessionCount(Long userId) {
    Set<WebSocketSession> userSessions = sessions.get(userId);
    if (userSessions == null) return 0;
    userSessions.removeIf(session -> !session.isOpen());
    return userSessions.size();
}
```

**Test:**

- Add 2 sessions cho cùng user -> online true.
- Remove 1 session -> vẫn online.
- Remove hết -> online false.

---

## Task 1.3 — Mở rộng schema message cho idempotency và sync

**Mục tiêu:** FE retry không bị trùng message; mỗi conversation có `serverSeq` tăng dần để sync ổn định.

**Files:**

- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/enums/MessageType.java`
- Modify: `backend/src/main/java/vn/edu/fpt/myfschool/entity/Message.java`
- Modify: `backend/src/main/java/vn/edu/fpt/myfschool/repository/MessageRepository.java`
- Modify: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/MessageDto.java`
- Modify: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/SendMessageRequest.java`

**Fields cần thêm vào `Message`:**

```java
@Column(length = 80)
private String clientMessageId;

@Enumerated(EnumType.STRING)
@Column(nullable = false, length = 20)
private MessageType messageType = MessageType.TEXT;

@Column(nullable = false)
private Long serverSeq;
```

**Indexes/unique nên có trên `messages`:**

```java
@Table(
    name = "messages",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_message_sender_client", columnNames = {"sender_id", "client_message_id"}),
        @UniqueConstraint(name = "uk_message_conversation_seq", columnNames = {"conversation_id", "server_seq"})
    },
    indexes = {
        @Index(name = "idx_message_conversation_created", columnList = "conversation_id, created_at"),
        @Index(name = "idx_message_conversation_seq", columnList = "conversation_id, server_seq")
    }
)
```

**Repository methods:**

```java
Optional<Message> findBySenderIdAndClientMessageId(Long senderId, String clientMessageId);

@Query("SELECT COALESCE(MAX(m.serverSeq), 0) FROM Message m WHERE m.conversation.id = :conversationId")
Long findMaxServerSeq(@Param("conversationId") Long conversationId);

List<Message> findByConversationIdAndServerSeqGreaterThanOrderByServerSeqAsc(
    Long conversationId, Long serverSeq, Pageable pageable);
```

**Note MySQL concurrency:**

- Phase đầu có thể dùng `MAX(serverSeq) + 1` trong transaction.
- Nếu tải cao, cần lock conversation row hoặc sequence table riêng để tránh trùng seq khi 2 người gửi cùng lúc.
- Do đã có unique `(conversation_id, server_seq)`, nếu collision thì catch duplicate và retry 1 lần.

---

## Task 1.4 — Tách service lưu message transaction ngắn

**Mục tiêu:** `ConversationService.sendMessage()` lưu DB đúng điểm, validate membership, idempotent, trả DTO đủ field cho FE.

**Files:**

- Modify: `backend/src/main/java/vn/edu/fpt/myfschool/service/ConversationService.java`
- Modify: `backend/src/main/java/vn/edu/fpt/myfschool/repository/ConversationParticipantRepository.java`

**Flow trong transaction:**

```txt
1. Load conversation
2. Check sender là participant
3. Nếu clientMessageId đã tồn tại cho sender -> trả message cũ
4. Tính next serverSeq
5. Insert messages
6. Update conversations.lastMessage, lastMessageAt
7. Commit
8. WebSocket layer mới ACK/push sau commit
```

**Participant repository:**

```java
boolean existsByConversationIdAndUserId(Long conversationId, Long userId);

@Query("SELECT cp.user.id FROM ConversationParticipant cp WHERE cp.conversation.id = :conversationId AND cp.user.id <> :userId")
List<Long> findOtherUserIds(@Param("conversationId") Long conversationId, @Param("userId") Long userId);
```

**Service method signature gợi ý:**

```java
public MessageDto sendMessage(Long conversationId, Long senderId, SendMessageRequest request)
```

`SendMessageRequest` nên là:

```java
public record SendMessageRequest(
    String clientMessageId,
    String content,
    MessageType messageType
) {}
```

Validation cụ thể:

```txt
clientMessageId: required, max 80 chars
content: required nếu messageType=TEXT, trim length 1..4000
messageType: null thì default TEXT
sender không thuộc conversation -> throw AccessDeniedException
```

**DB save point nằm ở đây:**

```txt
messageRepository.save(msg)
conversationRepository.save(conv)
```

Sau khi method return thành công, DB đã commit nếu method được gọi từ ngoài transaction proxy. Nếu gọi nội bộ cùng class cần chú ý transaction proxy; WebSocket layer nên gọi service bean public method.

---

## Task 1.5 — DTO WebSocket envelope và router

**Mục tiêu:** `ChatWebSocketHandler.handleTextMessage()` parse JSON và route theo `type`.

**Files:**

- Create: `backend/src/main/java/vn/edu/fpt/myfschool/websocket/dto/ClientWsMessage.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/websocket/dto/ServerWsMessage.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/websocket/dto/MessageSendPayload.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/service/ChatRealtimeService.java`
- Modify: `backend/src/main/java/vn/edu/fpt/myfschool/websocket/ChatWebSocketHandler.java`

**DTO shape:**

```java
public record ClientWsMessage(
    String type,
    Long conversationId,
    String clientMessageId,
    String messageType,
    String content,
    Long messageId,
    Long lastReadMessageId
) {}
```

```java
public record ServerWsMessage(
    String type,
    Object data
) {}
```

Vì payload hiện nhỏ, phase đầu có thể dùng flat DTO `ClientWsMessage` thay vì nested `payload` để code nhanh. Nếu muốn chuẩn hơn thì dùng `JsonNode payload`.

**Handler:**

```java
@Override
protected void handleTextMessage(WebSocketSession session, TextMessage textMessage) {
    Long userId = (Long) session.getAttributes().get("userId");
    if (userId == null) {
        closeSession(session, CloseStatus.NOT_ACCEPTABLE.withReason("Missing userId"));
        return;
    }
    chatRealtimeService.handle(userId, session, textMessage.getPayload());
}
```

**ChatRealtimeService.handle():**

```txt
switch type:
  message.send -> handleSend
  message.delivered -> handleDelivered
  message.read -> handleRead
  typing.start -> handleTyping(true)
  typing.stop -> handleTyping(false)
  presence.heartbeat -> handleHeartbeat
  default -> send error UNKNOWN_TYPE
```

**Error rule:**

- Không throw làm chết handler nếu chỉ lỗi payload/business.
- Gửi event `error` riêng về sender.
- Chỉ close session nếu unauthenticated hoặc JSON quá sai/spam.

---

## Task 1.6 — Implement `message.send` ACK + realtime push

**Mục tiêu:** User gửi message qua WebSocket, sender nhận ACK, recipient online nhận message mới.

**Files:**

- Modify: `backend/src/main/java/vn/edu/fpt/myfschool/service/ChatRealtimeService.java`
- Modify: `backend/src/main/java/vn/edu/fpt/myfschool/service/ConversationService.java`
- Modify: `backend/src/main/java/vn/edu/fpt/myfschool/repository/ConversationParticipantRepository.java`

**Flow:**

```txt
handleSend(userId, payload):
  1. Validate payload cơ bản
  2. MessageDto saved = conversationService.sendMessage(...)
  3. sendToUser(senderId, message.ack)
  4. Load recipient userIds
  5. sendToUsers(recipientIds online, message.new)
```

**ACK payload:**

```json
{
  "type": "message.ack",
  "clientMessageId": "fe-uuid-001",
  "status": "sent",
  "message": { }
}
```

**Recipient payload:**

```json
{
  "type": "message.new",
  "message": { },
  "conversation": {
    "id": 123,
    "lastMessage": "Xin chào cô",
    "lastMessageAt": "2026-06-29T10:00:00",
    "unreadCount": 1
  }
}
```

**Performance requirement:**

- Không gọi push notification trong method này.
- Không query toàn bộ conversation list sau khi save.
- Chỉ query participant ids và unread count cần thiết.

---

## Task 1.7 — Fix REST get messages và sync cơ bản

**Mục tiêu:** App có thể load lịch sử messages và sync khi reconnect.

**Files:**

- Modify: `backend/src/main/java/vn/edu/fpt/myfschool/controller/ConversationController.java`
- Modify: `backend/src/main/java/vn/edu/fpt/myfschool/service/MessageService.java`
- Modify: `backend/src/main/java/vn/edu/fpt/myfschool/repository/MessageRepository.java`

**Endpoint hiện tại cần sửa:**

```java
@GetMapping("/{id}")
public ResponseEntity<ApiResponse<List<MessageDto>>> getMessages(
        @PathVariable Long id,
        @RequestParam(required = false) Long beforeMessageId,
        @RequestParam(required = false) Long afterSeq,
        @RequestParam(defaultValue = "20") int limit) {
    return ResponseEntity.ok(ApiResponse.success(
        messageService.getMessages(id, SecurityUtil.getCurrentUserId(), beforeMessageId, afterSeq, limit)));
}
```

**Rules:**

- Check current user là participant trước khi trả messages.
- `beforeMessageId` dùng cho load older messages.
- `afterSeq` dùng khi reconnect sync messages mới.
- `limit` clamp 1..100.

---

# PHASE 2 — Delivery status

## Task 2.1 — Add `message_receipts`

**Mục tiêu:** Lưu đã nhận theo từng recipient.

**Files:**

- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/enums/MessageReceiptStatus.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/entity/MessageReceipt.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/repository/MessageReceiptRepository.java`
- Modify: `backend/src/main/java/vn/edu/fpt/myfschool/entity/Message.java`

**Entity shape:**

```java
@Entity
@Table(
    name = "message_receipts",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_receipt_message_user",
        columnNames = {"message_id", "user_id"}
    ),
    indexes = {
        @Index(name = "idx_receipt_user_status", columnList = "user_id, status"),
        @Index(name = "idx_receipt_message", columnList = "message_id")
    }
)
@Data
@EqualsAndHashCode(callSuper = true)
public class MessageReceipt extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageReceiptStatus status = MessageReceiptStatus.DELIVERED;

    private LocalDateTime deliveredAt;

    private LocalDateTime readAt;
}
```

**Important:**

- Không cần receipt cho sender ở phase đầu.
- Khi tạo message, có thể tạo receipt placeholder cho recipients với status null/PENDING, nhưng đơn giản hơn là chỉ insert khi recipient ACK delivered.
- Nếu cần biết message chưa delivered, query không có receipt tương ứng.

---

## Task 2.2 — Implement `message.delivered`

**Mục tiêu:** Recipient xác nhận thiết bị đã nhận message, sender thấy `delivered`.

**Files:**

- Modify: `backend/src/main/java/vn/edu/fpt/myfschool/service/ChatRealtimeService.java`
- Modify: `backend/src/main/java/vn/edu/fpt/myfschool/service/ConversationService.java` hoặc tạo `MessageReceiptService.java`

**Flow:**

```txt
FE recipient nhận message.new
  -> gửi message.delivered
BE:
  1. Check user là participant
  2. Check message thuộc conversation
  3. Check user không phải sender
  4. Upsert receipt delivered_at
  5. Notify sender message.receipt
```

**Payload sender nhận:**

```json
{
  "type": "message.receipt",
  "conversationId": 123,
  "messageId": 9001,
  "userId": 20,
  "status": "delivered",
  "deliveredAt": "2026-06-29T10:00:01"
}
```

**FE rule:**

- Nếu message đang `sent`, update thành `delivered`.
- Nếu message đã `read`, bỏ qua delivered cũ.

---

# PHASE 3 — Read status

## Task 3.1 — Add `lastReadMessageId`

**Mục tiêu:** Mark read theo message id/seq thay vì chỉ theo timestamp.

**Files:**

- Modify: `backend/src/main/java/vn/edu/fpt/myfschool/entity/ConversationParticipant.java`
- Modify: `backend/src/main/java/vn/edu/fpt/myfschool/repository/MessageRepository.java`
- Modify: `backend/src/main/java/vn/edu/fpt/myfschool/repository/ConversationParticipantRepository.java`

**Field:**

```java
private Long lastReadMessageId;
```

Giữ `lastReadAt` để biết thời điểm đọc cuối.

**Unread count query mới:**

```java
@Query("SELECT COUNT(m) FROM Message m WHERE m.conversation.id = :convId " +
       "AND m.sender.id <> :userId " +
       "AND (:lastReadMessageId IS NULL OR m.id > :lastReadMessageId)")
long countUnreadAfterMessageId(@Param("convId") Long convId,
                               @Param("userId") Long userId,
                               @Param("lastReadMessageId") Long lastReadMessageId);
```

Hoặc tối ưu hơn dùng `serverSeq` nếu có `lastReadSeq`.

---

## Task 3.2 — Implement `message.read`

**Mục tiêu:** User mở conversation, sender nhận seen/read event.

**Files:**

- Modify: `backend/src/main/java/vn/edu/fpt/myfschool/service/ChatRealtimeService.java`
- Modify: `backend/src/main/java/vn/edu/fpt/myfschool/service/ConversationService.java`
- Modify: `backend/src/main/java/vn/edu/fpt/myfschool/controller/ConversationController.java`

**Flow:**

```txt
FE mở conversation hoặc scroll tới message cuối
  -> gửi message.read { conversationId, lastReadMessageId }
BE:
  1. Check participant
  2. Check lastReadMessageId thuộc conversation
  3. Nếu lastReadMessageId <= current lastReadMessageId thì bỏ qua
  4. Update conversation_participants.lastReadMessageId, lastReadAt
  5. Optional update message_receipts readAt cho messages <= id
  6. Notify other participants conversation.read
```

**Payload:**

```json
{
  "type": "conversation.read",
  "conversationId": 123,
  "userId": 20,
  "lastReadMessageId": 9001,
  "readAt": "2026-06-29T10:00:05"
}
```

**REST fallback:**

`PUT /api/conversations/{id}/read` có thể nhận body:

```json
{
  "lastReadMessageId": 9001
}
```

Nếu không có body thì mark read tới message mới nhất hiện tại.

---

# PHASE 4 — Online presence

## Task 4.1 — Presence memory phase đầu

**Mục tiêu:** Có online/offline realtime ngay, chưa cần Redis.

**Files:**

- Modify: `backend/src/main/java/vn/edu/fpt/myfschool/websocket/WebSocketSessionManager.java`
- Modify: `backend/src/main/java/vn/edu/fpt/myfschool/service/ChatRealtimeService.java`
- Modify: `backend/src/main/java/vn/edu/fpt/myfschool/websocket/ChatWebSocketHandler.java`
- Modify: `backend/src/main/java/vn/edu/fpt/myfschool/entity/ConversationParticipant.java`

**Flow:**

```txt
afterConnectionEstablished:
  - addSession
  - notify related users presence.update online=true

afterConnectionClosed / transport error:
  - removeSession
  - nếu user không còn session nào:
      update lastSeenAt
      notify related users online=false,lastSeenAt

presence.heartbeat:
  - update in-memory lastHeartbeat
  - không update MySQL
```

**Related users:**

- Phase đầu: users cùng conversation với current user.
- Không broadcast toàn hệ thống.

**Payload:**

```json
{
  "type": "presence.update",
  "userId": 10,
  "online": false,
  "lastSeenAt": "2026-06-29T10:05:00"
}
```

---

## Task 4.2 — Redis presence phase tối ưu

**Mục tiêu:** Chuẩn bị scale nhiều instance backend.

**Files:**

- Modify: `backend/pom.xml`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/service/PresenceService.java`
- Modify: `backend/src/main/resources/application.yml` hoặc profile config hiện có.

**Dependency:**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

**Redis keys:**

```txt
presence:user:{userId} = online, TTL 30s
```

**Rules:**

```txt
connect: SET key online EX 30
heartbeat mỗi 15s: EXPIRE key 30
close: DEL key nếu không còn session local
last_seen_at DB: update khi offline thật hoặc scheduled cleanup
```

**Ghi chú:**

- Phase này chỉ cần khi deploy nhiều backend instance hoặc cần online status survive instance memory.
- Nếu chạy 1 instance local, memory presence đủ nhanh hơn và ít dependency hơn.

---

# PHASE 5 — Typing indicator

## Task 5.1 — Implement typing events không lưu DB

**Mục tiêu:** FE thấy “A đang nhập...” realtime.

**Files:**

- Modify: `backend/src/main/java/vn/edu/fpt/myfschool/service/ChatRealtimeService.java`

**Flow:**

```txt
typing.start:
  - Check participant
  - Broadcast typing.update typing=true tới participants khác

typing.stop:
  - Check participant
  - Broadcast typing.update typing=false tới participants khác
```

**Payload:**

```json
{
  "type": "typing.update",
  "conversationId": 123,
  "userId": 10,
  "typing": true
}
```

**Performance rule:**

- Không lưu DB.
- FE throttle gửi `typing.start`, ví dụ tối đa 1 event / 2 giây.
- FE tự stop nếu không nhận update mới sau 3-5 giây.

---

# PHASE 6 — Flutter app integration

## Task 6.1 — Add chat models and DTO mapping

**Mục tiêu:** App có model thuần Dart cho conversation/message/status.

**Files:**

- Create: `lib/vn/edu/fpt/src/models/chat_message.dart`
- Create: `lib/vn/edu/fpt/src/models/conversation.dart`
- Create: `lib/vn/edu/fpt/src/api/dto/chat_message_dto.dart`
- Create: `lib/vn/edu/fpt/src/api/dto/conversation_dto.dart`
- Modify: `lib/vn/edu/fpt/src/models/models.dart`

**Enums:**

```dart
enum ChatMessageStatus {
  sending,
  sent,
  delivered,
  read,
  failed,
}

enum ChatMessageType {
  text,
  image,
  file,
  system,
}
```

**ChatMessage fields:**

```dart
class ChatMessage {
  const ChatMessage({
    required this.id,
    required this.clientMessageId,
    required this.conversationId,
    required this.senderId,
    required this.senderName,
    required this.content,
    required this.type,
    required this.status,
    required this.createdAt,
    required this.isMine,
    this.serverSeq,
    this.senderAvatar,
  });

  final int? id;
  final String clientMessageId;
  final int conversationId;
  final int senderId;
  final String senderName;
  final String? senderAvatar;
  final String content;
  final ChatMessageType type;
  final ChatMessageStatus status;
  final DateTime createdAt;
  final bool isMine;
  final int? serverSeq;
}
```

---

## Task 6.2 — Add REST chat client

**Mục tiêu:** Load conversation list/messages, fallback mark read.

**Files:**

- Create: `lib/vn/edu/fpt/src/api/client/chat_api_client.dart`
- Create: `lib/vn/edu/fpt/src/repositories/chat_repository.dart`
- Modify: `lib/vn/edu/fpt/src/repositories/repositories.dart`

**Endpoints dùng:**

```txt
GET /api/conversations
POST /api/conversations
GET /api/conversations/{id}?beforeMessageId=&afterSeq=&limit=20
PUT /api/conversations/{id}/read
GET /api/conversations/unread-count
```

**Note:**

- Project hiện chưa có real HTTP client layer cho backend; nếu đã có auth token storage thì reuse.
- Nếu chưa có, phase này tạo client tối giản bằng `dart:io`/`HttpClient` hoặc add `http` package.

---

## Task 6.3 — Add WebSocket service

**Mục tiêu:** App connect `/chat?token=...`, send/receive events, expose stream cho UI.

**Files:**

- Modify: `pubspec.yaml`
- Create: `lib/vn/edu/fpt/src/services/chat_socket_service.dart`
- Modify: `lib/vn/edu/fpt/src/services/services.dart`

**Dependency gợi ý:**

```yaml
dependencies:
  web_socket_channel: ^3.0.3
```

**Service responsibilities:**

```txt
connect(token)
disconnect()
sendMessage(conversationId, clientMessageId, content)
markDelivered(conversationId, messageId)
markRead(conversationId, lastReadMessageId)
sendTypingStart(conversationId)
sendTypingStop(conversationId)
heartbeat every 15s
reconnect with backoff
stream events to ChatService/UI
```

**Client send payload:**

```dart
{
  'type': 'message.send',
  'conversationId': conversationId,
  'clientMessageId': clientMessageId,
  'messageType': 'TEXT',
  'content': content,
}
```

---

## Task 6.4 — Chat service state management

**Mục tiêu:** Quản lý local state `sending/sent/delivered/read/failed`, retry và sync.

**Files:**

- Create: `lib/vn/edu/fpt/src/services/chat_service.dart`

**Rules:**

```txt
send text:
  1. Sinh clientMessageId UUID
  2. Add local ChatMessage id=null,status=sending
  3. Send WS message.send
  4. Start ACK timeout 5-8s
  5. Nếu message.ack match clientMessageId -> replace local id/status=sent/serverSeq
  6. Nếu timeout/socket error -> status=failed

message.new:
  1. Append nếu message id chưa tồn tại
  2. Send message.delivered ngay nếu message không phải của mình
  3. Nếu đang mở conversation đó, send message.read last message id

message.receipt delivered:
  - update message status sent -> delivered

conversation.read:
  - update các message mình gửi có id <= lastReadMessageId thành read
```

**UUID:**

- Có thể dùng package `uuid`.
- Nếu không thêm package, generate bằng timestamp + random; nhưng package `uuid` sạch hơn.

---

## Task 6.5 — Replace mock conversation list

**Mục tiêu:** `messages_screen.dart` hiển thị conversations thật.

**Files:**

- Modify: `lib/vn/edu/fpt/view/screens/messages_screen.dart`
- Create: `lib/vn/edu/fpt/view/screens/chat_detail_screen.dart`

**Conversation tile cần hiển thị:**

```txt
avatar
name
online green dot nếu isOnline=true
lastMessage
lastMessageAt formatted
unreadCount badge
```

**Tap behavior:**

- Mở `ChatDetailScreen(conversationId)` trong nested navigator tab message.
- Load messages page đầu.
- Connect WS nếu chưa connect.
- Gửi `message.read` khi screen mở xong và có message cuối.

---

## Task 6.6 — Chat detail screen

**Mục tiêu:** Màn hình chat có send text, status icon, typing.

**Files:**

- Modify: `lib/vn/edu/fpt/view/screens/chat_detail_screen.dart`

**UI elements:**

```txt
AppBar: avatar + tên + online/last seen
ListView reverse hoặc scroll xuống cuối
Bubble trái/phải
Status nhỏ dưới message của mình:
  sending: icon đồng hồ/spinner
  sent: 1 check
  delivered: 2 check xám
  read: avatar nhỏ hoặc 2 check xanh
  failed: icon lỗi + retry
TextField + send button
Typing indicator: "Đang nhập..."
```

**Typing FE:**

```txt
onChanged non-empty:
  send typing.start nếu lần cuối >2s
Timer 3s không gõ:
  send typing.stop
```

---

# PHASE 7 — Offline sync, retry, notification

## Task 7.1 — Reconnect sync by `serverSeq`

**Mục tiêu:** Mất mạng/reconnect không mất message.

**Backend:**

- `GET /api/conversations/{id}?afterSeq=<lastLocalSeq>` trả messages mới hơn.

**Flutter:**

```txt
on socket reconnect:
  for each active/recent conversation:
    lastSeq = max(serverSeq local)
    fetch afterSeq
    merge by message id/clientMessageId
```

**Merge rule:**

```txt
Nếu local có clientMessageId giống ACK/server message:
  replace local pending/failed bằng server message
Nếu local chưa có id đó:
  append
Không append trùng message id
```

---

## Task 7.2 — Push notification async

**Mục tiêu:** Recipient offline vẫn nhận thông báo, không làm chậm send message.

**Backend rule:**

```txt
Sau khi commit message:
  recipient offline -> enqueue push job
  không gọi FCM trực tiếp trong critical path
```

**Payload FCM:**

```json
{
  "conversationId": "123",
  "messageId": "9001",
  "type": "chat_message"
}
```

**Flutter rule:**

- Tap notification mở đúng conversation.
- Khi app resume, luôn sync `afterSeq` thay vì tin vào payload notification.

---

# 8. Performance plan <200ms

## Critical path budget

```txt
FE WS send -> BE receive: 20-50ms
JWT/session user already resolved: 0-2ms
Validate membership: 1 indexed query, 2-10ms
Insert message + update conversation: 10-40ms
Commit: 5-20ms
ACK sender: 5-20ms
Push recipient online: 5-30ms
Target total: 50-150ms bình thường, <200ms mục tiêu
```

## Indexes bắt buộc

```txt
conversation_participants(conversation_id, user_id) unique
conversation_participants(user_id)
messages(sender_id, client_message_id) unique
messages(conversation_id, server_seq) unique
messages(conversation_id, created_at)
message_receipts(message_id, user_id) unique
message_receipts(user_id, status)
```

## Không làm trong critical path

```txt
Không gửi FCM trực tiếp
Không update online vào MySQL mỗi heartbeat
Không query full conversation list sau mỗi message
Không load attachments nặng khi gửi text
Không broadcast presence toàn hệ thống
Không tính unread count bằng scan full messages nếu data lớn
```

## Khi data lớn hơn

- Dùng Redis presence.
- Dùng `lastReadSeq` thay vì `lastReadMessageId` để unread count nhanh hơn.
- Dùng queue cho push notification.
- Dùng pagination bằng `serverSeq` thay vì offset.
- Nếu nhiều backend instance, cần message broker/pubsub để route WS event giữa instances.

---

# 9. Suggested commit order

```bash
git add backend/src/main/java/vn/edu/fpt/myfschool/config/WebSocketConfig.java \
        backend/src/main/java/vn/edu/fpt/myfschool/websocket

git commit -m "feat: authenticate chat websocket connections"
```

```bash
git add backend/src/main/java/vn/edu/fpt/myfschool/entity/Message.java \
        backend/src/main/java/vn/edu/fpt/myfschool/common/enums/MessageType.java \
        backend/src/main/java/vn/edu/fpt/myfschool/repository/MessageRepository.java \
        backend/src/main/java/vn/edu/fpt/myfschool/common/dto

git commit -m "feat: add idempotent chat message persistence"
```

```bash
git add backend/src/main/java/vn/edu/fpt/myfschool/service/ConversationService.java \
        backend/src/main/java/vn/edu/fpt/myfschool/service/ChatRealtimeService.java \
        backend/src/main/java/vn/edu/fpt/myfschool/websocket/ChatWebSocketHandler.java

git commit -m "feat: send chat messages over websocket"
```

```bash
git add backend/src/main/java/vn/edu/fpt/myfschool/entity/MessageReceipt.java \
        backend/src/main/java/vn/edu/fpt/myfschool/repository/MessageReceiptRepository.java \
        backend/src/main/java/vn/edu/fpt/myfschool/service

git commit -m "feat: track chat delivery and read receipts"
```

```bash
git add lib/vn/edu/fpt/src lib/vn/edu/fpt/view/screens pubspec.yaml

git commit -m "feat: connect Flutter chat to websocket backend"
```

---

# 10. Test checklist

## Backend unit/integration

```bash
cd backend
mvn test -Dtest=ConversationMessagingIntegrationTest
mvn test -Dtest=WebSocketMessagingIntegrationTest
mvn test
```

Test cases cần có:

```txt
message.send thành công -> DB có message, sender nhận ack, recipient nhận message.new
message.send retry cùng clientMessageId -> không insert trùng
sender không thuộc conversation -> error NOT_CONVERSATION_MEMBER
GET /api/conversations/{id} -> trả messages thật, không trả list rỗng
message.delivered -> upsert receipt, sender nhận message.receipt
message.read -> update lastReadMessageId, unreadCount về 0
connect không token -> reject
connect token hợp lệ -> online true
```

## Flutter

```bash
flutter pub get
flutter analyze
flutter test
```

Manual test:

```txt
1. Login parent ở device A/browser A
2. Login teacher ở device B/browser B
3. A mở conversation với B
4. A gửi message
5. A thấy sending -> sent dưới 200ms nếu local server ổn
6. B nhận message realtime
7. A thấy delivered
8. B mở conversation
9. A thấy read
10. B tắt app/socket
11. A thấy B offline/last seen
12. B mở lại app
13. B sync không mất message
```

---

# 11. Quyết định nên làm ngay vs để sau

## Làm ngay

```txt
JWT handshake
message.send / ack / new
clientMessageId idempotency
serverSeq
REST get messages thật
membership validation
read basic bằng lastReadMessageId
presence memory
```

## Để sau khi core chạy ổn

```txt
Redis presence
FCM push notification
Group chat nhiều member
File/image upload
Message delete/edit
Search message
Message reactions
End-to-end encryption
```

---

# 12. Self-review

- Không còn placeholder kiểu TBD/TODO.
- Plan bám vào file thật đã thấy trong repo.
- Có đủ điểm lưu DB: insert message/update conversation trong `ConversationService.sendMessage()`, delivery/read trong receipt/participant service.
- Có đủ payload FE cần: ACK, message.new, receipt, read, typing, presence, error.
- Có phase riêng cho BE core, delivery, read, presence, typing, Flutter, offline sync.
- Có performance checklist cho mục tiêu <200ms.
