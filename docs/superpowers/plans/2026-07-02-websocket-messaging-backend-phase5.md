# WebSocket Messaging Backend Phase 5 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Hoàn thiện backend chat realtime qua WebSocket đến hết Phase 5: send/ack/new, delivery receipt, read receipt, memory presence, typing indicator.

**Architecture:** Spring WebSocket endpoint `/chat` xác thực JWT ở handshake, `ChatWebSocketHandler` chỉ quản lý session và delegate JSON sang `ChatRealtimeService`. DB writes nằm trong transaction ngắn ở `ConversationServiceImpl`; realtime ACK/push chạy sau khi service trả về thành công.

**Tech Stack:** Spring Boot 3.4.5, Java 21, Spring WebSocket, Spring Security JWT, Spring Data JPA, MySQL/H2 test, Jackson.

## Global Constraints

- Flutter không kết nối trực tiếp MySQL; Flutter chỉ gọi Backend API/WebSocket.
- UI text/comment dùng tiếng Việt; code identifiers dùng English.
- Backend chỉ trả `sent` sau khi DB transaction commit thành công.
- Online realtime dùng memory session; Redis Phase 4.2 bị skip theo quyết định user ngày 2026-07-02.
- Critical path gửi tin nhắn: validate membership -> insert DB -> update conversation -> commit -> ACK sender -> push recipient.
- Không thêm push notification, search indexing, analytics, thumbnail/file processing vào critical path.
- Chỉ sửa files thật sự liên quan module nhắn tin backend.
- Không chạy `git commit`/`git push` trừ khi user yêu cầu rõ. Nếu user yêu cầu commit, chỉ commit trên branch `master`.

---

## File Structure

### Create

- `backend/src/main/java/vn/edu/fpt/myfschool/common/enums/MessageReceiptStatus.java` — enum `DELIVERED`, `READ`.
- `backend/src/main/java/vn/edu/fpt/myfschool/entity/MessageReceipt.java` — per-message, per-recipient receipt row.
- `backend/src/main/java/vn/edu/fpt/myfschool/repository/MessageReceiptRepository.java` — lookup receipt by message/user.
- `backend/src/main/java/vn/edu/fpt/myfschool/service/ChatRealtimeService.java` — parse WS events, call services, send server events.
- `backend/src/main/java/vn/edu/fpt/myfschool/websocket/dto/ClientWsMessage.java` — flat client event envelope.
- `backend/src/test/java/vn/edu/fpt/myfschool/ConversationMessagingIntegrationTest.java` — REST/service integration checks for persistence, idempotency, sync, read.

### Modify

- `backend/src/main/java/vn/edu/fpt/myfschool/config/WebSocketConfig.java` — register injected handler directly.
- `backend/src/main/java/vn/edu/fpt/myfschool/websocket/ChatHandshakeInterceptor.java` — URL-decode query token.
- `backend/src/main/java/vn/edu/fpt/myfschool/websocket/ChatWebSocketHandler.java` — delegate messages and presence lifecycle.
- `backend/src/main/java/vn/edu/fpt/myfschool/websocket/WebSocketSessionManager.java` — add heartbeat/last seen helpers and safer online cleanup.
- `backend/src/main/java/vn/edu/fpt/myfschool/entity/Message.java` — add receipt relation and server seq index if missing.
- `backend/src/main/java/vn/edu/fpt/myfschool/entity/ConversationParticipant.java` — add `lastReadMessageId`, `lastSeenAt`.
- `backend/src/main/java/vn/edu/fpt/myfschool/repository/MessageRepository.java` — add afterSeq, message ownership, unread-after-message queries.
- `backend/src/main/java/vn/edu/fpt/myfschool/repository/ConversationParticipantRepository.java` — add participant/user queries for presence and realtime fanout.
- `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/MessageDto.java` — add sender avatar and status string for FE event payloads.
- `backend/src/main/java/vn/edu/fpt/myfschool/service/ConversationService.java` — add read/delivery/presence support signatures.
- `backend/src/main/java/vn/edu/fpt/myfschool/service/impl/ConversationServiceImpl.java` — receipt/read/presence implementation.
- `backend/src/main/java/vn/edu/fpt/myfschool/service/MessageService.java` — add `afterSeq` parameter.
- `backend/src/main/java/vn/edu/fpt/myfschool/service/impl/MessageServiceImpl.java` — REST sync by `afterSeq`.
- `backend/src/main/java/vn/edu/fpt/myfschool/controller/ConversationController.java` — expose `afterSeq` and `lastReadMessageId` body.

---

## Task 1: WebSocket wiring and session lifecycle

**Files:**
- Modify: `backend/src/main/java/vn/edu/fpt/myfschool/config/WebSocketConfig.java:1-30`
- Modify: `backend/src/main/java/vn/edu/fpt/myfschool/websocket/ChatHandshakeInterceptor.java:1-50`
- Modify: `backend/src/main/java/vn/edu/fpt/myfschool/websocket/WebSocketSessionManager.java:1-60`
- Modify: `backend/src/main/java/vn/edu/fpt/myfschool/websocket/ChatWebSocketHandler.java:1-44`

**Interfaces:**
- Consumes: `JwtTokenProvider.validateToken(String)`, `JwtTokenProvider.getUserIdFromToken(String)`.
- Produces:
  - `WebSocketSessionManager.addSession(Long userId, WebSocketSession session)`
  - `WebSocketSessionManager.removeSession(Long userId, WebSocketSession session)`
  - `WebSocketSessionManager.sendToUser(Long userId, String message)`
  - `WebSocketSessionManager.sendToUsers(Collection<Long> userIds, String message)`
  - `WebSocketSessionManager.getOpenSessionCount(Long userId)`
  - `WebSocketSessionManager.isOnline(Long userId)`
  - `WebSocketSessionManager.heartbeat(Long userId)`
  - `WebSocketSessionManager.getLastHeartbeat(Long userId)`

- [ ] **Step 1: Clean WebSocket config**

Replace `WebSocketConfig` with direct injected handler registration:

```java
package vn.edu.fpt.myfschool.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import vn.edu.fpt.myfschool.websocket.ChatHandshakeInterceptor;
import vn.edu.fpt.myfschool.websocket.ChatWebSocketHandler;

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

- [ ] **Step 2: URL-decode query token**

In `ChatHandshakeInterceptor.extractToken`, replace the query branch with:

```java
String query = request.getURI().getQuery();
if (query != null) {
    for (String key : query.split("&")) {
        String[] parts = key.split("=", 2);
        if (parts.length == 2 && "token".equals(parts[0])) {
            return java.net.URLDecoder.decode(parts[1], java.nio.charset.StandardCharsets.UTF_8);
        }
    }
}
```

Keep the `Authorization: Bearer` branch unchanged.

- [ ] **Step 3: Extend session manager**

Add a heartbeat map and these methods to `WebSocketSessionManager`:

```java
private final Map<Long, java.time.LocalDateTime> lastHeartbeats = new ConcurrentHashMap<>();

public void heartbeat(Long userId) {
    if (userId != null) {
        lastHeartbeats.put(userId, java.time.LocalDateTime.now());
    }
}

public java.time.LocalDateTime getLastHeartbeat(Long userId) {
    return lastHeartbeats.get(userId);
}
```

Update `addSession` to call `heartbeat(userId)` after adding the session. Update `isOnline` to return `getOpenSessionCount(userId) > 0` so closed sessions are cleaned before checking.

- [ ] **Step 4: Prepare handler delegation hooks**

Change constructor fields in `ChatWebSocketHandler` to:

```java
private final WebSocketSessionManager sessionManager;
private final vn.edu.fpt.myfschool.service.ChatRealtimeService chatRealtimeService;
```

Update methods:

```java
@Override
public void afterConnectionEstablished(WebSocketSession session) {
    Long userId = (Long) session.getAttributes().get("userId");
    if (userId != null) {
        sessionManager.addSession(userId, session);
        chatRealtimeService.handleConnected(userId);
    }
}

@Override
public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
    Long userId = (Long) session.getAttributes().get("userId");
    if (userId != null) {
        sessionManager.removeSession(userId, session);
        chatRealtimeService.handleDisconnected(userId);
    }
}

@Override
protected void handleTextMessage(WebSocketSession session, TextMessage message) {
    Long userId = (Long) session.getAttributes().get("userId");
    if (userId == null) {
        try {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Missing userId"));
        } catch (java.io.IOException ignored) {
        }
        return;
    }
    chatRealtimeService.handle(userId, session, message.getPayload());
}

@Override
public void handleTransportError(WebSocketSession session, Throwable exception) {
    Long userId = (Long) session.getAttributes().get("userId");
    if (userId != null) {
        sessionManager.removeSession(userId, session);
        chatRealtimeService.handleDisconnected(userId);
    }
}
```

This will compile after Task 4 creates `ChatRealtimeService`.

- [ ] **Step 5: Run focused compile check**

Run:

```bash
cd backend && mvn -q -DskipTests compile
```

Expected after Task 1 alone: compile can fail because `ChatRealtimeService` is not created yet. Continue to Task 4 before treating this as a failure.

---

## Task 2: Receipt/read schema and repositories

**Files:**
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/enums/MessageReceiptStatus.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/entity/MessageReceipt.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/repository/MessageReceiptRepository.java`
- Modify: `backend/src/main/java/vn/edu/fpt/myfschool/entity/Message.java:23-77`
- Modify: `backend/src/main/java/vn/edu/fpt/myfschool/entity/ConversationParticipant.java:7-28`
- Modify: `backend/src/main/java/vn/edu/fpt/myfschool/repository/MessageRepository.java:13-36`
- Modify: `backend/src/main/java/vn/edu/fpt/myfschool/repository/ConversationParticipantRepository.java:10-22`

**Interfaces:**
- Produces:
  - `MessageReceiptStatus.DELIVERED`, `MessageReceiptStatus.READ`
  - `MessageReceiptRepository.findByMessageIdAndUserId(Long, Long)`
  - `MessageRepository.existsByIdAndConversationId(Long, Long)`
  - `MessageRepository.findByConversationIdAndServerSeqGreaterThanOrderByServerSeqAsc(Long, Long, Pageable)`
  - `MessageRepository.countUnreadAfterMessageId(Long, Long, Long)`
  - `ConversationParticipant.lastReadMessageId`, `ConversationParticipant.lastSeenAt`

- [ ] **Step 1: Create receipt status enum**

```java
package vn.edu.fpt.myfschool.common.enums;

public enum MessageReceiptStatus {
    DELIVERED,
    READ
}
```

- [ ] **Step 2: Create MessageReceipt entity**

```java
package vn.edu.fpt.myfschool.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import vn.edu.fpt.myfschool.common.enums.MessageReceiptStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "message_receipts",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_receipt_message_user",
                columnNames = {"message_id", "user_id"}
        ),
        indexes = {
                @Index(name = "idx_receipt_user_status", columnList = "user_id, status"),
                @Index(name = "idx_receipt_message", columnList = "message_id")
        })
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class MessageReceipt extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_receipt_message"))
    private Message message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_receipt_user"))
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageReceiptStatus status = MessageReceiptStatus.DELIVERED;

    private LocalDateTime deliveredAt;

    private LocalDateTime readAt;
}
```

- [ ] **Step 3: Create repository**

```java
package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.myfschool.entity.MessageReceipt;

import java.util.Optional;

@Repository
public interface MessageReceiptRepository extends JpaRepository<MessageReceipt, Long> {
    Optional<MessageReceipt> findByMessageIdAndUserId(Long messageId, Long userId);
}
```

- [ ] **Step 4: Add entity fields**

In `Message`, add below `attachments`:

```java
@OneToMany(mappedBy = "message", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
private List<MessageReceipt> receipts = new ArrayList<>();
```

Add `@Index(name = "idx_message_conversation_seq", columnList = "conversation_id, server_seq")` to the existing `@Table(indexes = ...)` array.

In `ConversationParticipant`, add fields below `lastReadAt`:

```java
private Long lastReadMessageId;

private LocalDateTime lastSeenAt;
```

- [ ] **Step 5: Add repository queries**

Append to `MessageRepository`:

```java
boolean existsByIdAndConversationId(Long id, Long conversationId);

@Query("SELECT COUNT(m) FROM Message m WHERE m.conversation.id = :convId " +
        "AND m.sender.id <> :userId " +
        "AND (:lastReadMessageId IS NULL OR m.id > :lastReadMessageId)")
long countUnreadAfterMessageId(@Param("convId") Long convId,
                               @Param("userId") Long userId,
                               @Param("lastReadMessageId") Long lastReadMessageId);
```

Append to `ConversationParticipantRepository`:

```java
@Query("SELECT DISTINCT other.user.id FROM ConversationParticipant mine " +
        "JOIN ConversationParticipant other ON other.conversation.id = mine.conversation.id " +
        "WHERE mine.user.id = :userId AND other.user.id <> :userId")
List<Long> findRelatedUserIds(@Param("userId") Long userId);
```

- [ ] **Step 6: Compile schema changes**

Run:

```bash
cd backend && mvn -q -DskipTests compile
```

Expected after Task 2 alone: compile can still fail until Task 4 creates `ChatRealtimeService` and Task 3 updates DTO constructors. Continue in order.

---

## Task 3: Message DTO, REST sync, and service read APIs

**Files:**
- Modify: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/MessageDto.java:7-20`
- Modify: `backend/src/main/java/vn/edu/fpt/myfschool/service/MessageService.java:9-11`
- Modify: `backend/src/main/java/vn/edu/fpt/myfschool/service/impl/MessageServiceImpl.java:24-50`
- Modify: `backend/src/main/java/vn/edu/fpt/myfschool/service/ConversationService.java:23-33`
- Modify: `backend/src/main/java/vn/edu/fpt/myfschool/service/impl/ConversationServiceImpl.java:38-181`
- Modify: `backend/src/main/java/vn/edu/fpt/myfschool/controller/ConversationController.java:44-61`

**Interfaces:**
- Consumes: Task 2 repository methods.
- Produces:
  - `MessageDto(..., String senderAvatar, String status, ...)`
  - `MessageService.getMessages(Long conversationId, Long userId, Long beforeMessageId, Long afterSeq, int limit)`
  - `ConversationService.markAsDelivered(Long conversationId, Long messageId, Long userId)` returning `MessageReceipt`
  - `ConversationService.markAsRead(Long conversationId, Long userId, Long lastReadMessageId)` returning `ConversationParticipant`
  - `ConversationService.getOtherParticipantIds(Long conversationId, Long userId)`
  - `ConversationService.getRelatedUserIds(Long userId)`

- [ ] **Step 1: Extend MessageDto**

Replace `MessageDto` with:

```java
package vn.edu.fpt.myfschool.common.dto;

import vn.edu.fpt.myfschool.common.enums.MessageType;

import java.time.LocalDateTime;
import java.util.List;

public record MessageDto(
        Long id,
        String clientMessageId,
        Long conversationId,
        Long senderId,
        String senderName,
        String senderAvatar,
        MessageType messageType,
        String content,
        Long serverSeq,
        boolean isMine,
        String status,
        LocalDateTime createdAt,
        List<AttachmentDto> attachments
) {
}
```

- [ ] **Step 2: Update MessageService signature**

```java
package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.common.dto.MessageDto;
import java.util.List;

public interface MessageService {
    List<MessageDto> getMessages(Long conversationId, Long userId, Long beforeMessageId, Long afterSeq, int limit);
}
```

- [ ] **Step 3: Update MessageServiceImpl mapping and afterSeq**

Replace `getMessages` with:

```java
@Override
public List<MessageDto> getMessages(Long conversationId, Long userId, Long beforeMessageId, Long afterSeq, int limit) {
    if (!participantRepository.existsByConversationIdAndUserId(conversationId, userId)) {
        throw new ForbiddenException("Bạn không thuộc cuộc hội thoại này");
    }

    int pageSize = Math.max(1, Math.min(limit, 100));
    List<Message> messages;
    if (afterSeq != null) {
        messages = messageRepository.findByConversationIdAndServerSeqGreaterThanOrderByServerSeqAsc(
                conversationId, afterSeq, PageRequest.of(0, pageSize));
    } else if (beforeMessageId != null) {
        messages = messageRepository.findMessagesBefore(conversationId, beforeMessageId, PageRequest.of(0, pageSize));
    } else {
        messages = messageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, PageRequest.of(0, pageSize));
    }
    return messages.stream().map(m -> toDto(m, userId)).collect(Collectors.toList());
}

private MessageDto toDto(Message m, Long userId) {
    return new MessageDto(
            m.getId(),
            m.getClientMessageId(),
            m.getConversation().getId(),
            m.getSender().getId(),
            m.getSender().getName(),
            m.getSender().getAvatar(),
            m.getMessageType(),
            m.getContent(),
            m.getServerSeq(),
            m.getSender().getId().equals(userId),
            m.getSender().getId().equals(userId) ? "sent" : "delivered",
            m.getCreatedAt(),
            List.of()
    );
}
```

- [ ] **Step 4: Extend ConversationService interface**

Replace the read/fanout part of `ConversationService` with:

```java
MessageDto sendMessage(Long conversationId, Long senderId, SendMessageRequest request);

void markAsRead(Long conversationId, Long userId);

ConversationParticipant markAsRead(Long conversationId, Long userId, Long lastReadMessageId);

MessageReceipt markAsDelivered(Long conversationId, Long messageId, Long userId);

List<Long> getOtherParticipantIds(Long conversationId, Long userId);

List<Long> getRelatedUserIds(Long userId);

int getTotalUnreadCount(Long userId);
```

Add imports:

```java
import vn.edu.fpt.myfschool.entity.ConversationParticipant;
import vn.edu.fpt.myfschool.entity.MessageReceipt;
```

- [ ] **Step 5: Update ConversationServiceImpl fields**

Add repository fields:

```java
private final vn.edu.fpt.myfschool.repository.MessageReceiptRepository messageReceiptRepository;
```

Add imports:

```java
import vn.edu.fpt.myfschool.common.enums.MessageReceiptStatus;
import vn.edu.fpt.myfschool.entity.MessageReceipt;
```

- [ ] **Step 6: Update unread count and DTO mapping**

Change `getConversations` unread calculation to:

```java
long unread = messageRepository.countUnreadAfterMessageId(
        c.getId(), userId,
        participantRepository.findByConversationIdAndUserId(c.getId(), userId)
                .map(ConversationParticipant::getLastReadMessageId)
                .orElse(null));
```

Replace `toMessageDto` constructor with:

```java
return new MessageDto(
        msg.getId(),
        msg.getClientMessageId(),
        msg.getConversation().getId(),
        msg.getSender().getId(),
        msg.getSender().getName(),
        msg.getSender().getAvatar(),
        msg.getMessageType(),
        msg.getContent(),
        msg.getServerSeq(),
        msg.getSender().getId().equals(currentUserId),
        msg.getSender().getId().equals(currentUserId) ? "sent" : "delivered",
        msg.getCreatedAt(),
        List.of()
);
```

- [ ] **Step 7: Add delivery/read methods**

Add to `ConversationServiceImpl`:

```java
@Override
public MessageReceipt markAsDelivered(Long conversationId, Long messageId, Long userId) {
    if (!participantRepository.existsByConversationIdAndUserId(conversationId, userId)) {
        throw new ForbiddenException("Bạn không thuộc cuộc hội thoại này");
    }
    Message message = messageRepository.findById(messageId)
            .orElseThrow(() -> new ResourceNotFoundException("Message", "id", messageId));
    if (!message.getConversation().getId().equals(conversationId)) {
        throw new BadRequestException("Tin nhắn không thuộc hội thoại này");
    }
    if (message.getSender().getId().equals(userId)) {
        throw new BadRequestException("Người gửi không cần xác nhận đã nhận");
    }

    MessageReceipt receipt = messageReceiptRepository.findByMessageIdAndUserId(messageId, userId)
            .orElseGet(() -> {
                MessageReceipt created = new MessageReceipt();
                created.setMessage(message);
                created.setUser(userRepository.findById(userId).orElseThrow());
                return created;
            });
    if (receipt.getDeliveredAt() == null) {
        receipt.setDeliveredAt(LocalDateTime.now());
    }
    receipt.setStatus(receipt.getStatus() == MessageReceiptStatus.READ
            ? MessageReceiptStatus.READ
            : MessageReceiptStatus.DELIVERED);
    return messageReceiptRepository.save(receipt);
}

@Override
public ConversationParticipant markAsRead(Long conversationId, Long userId, Long lastReadMessageId) {
    ConversationParticipant participant = participantRepository.findByConversationIdAndUserId(conversationId, userId)
            .orElseThrow(() -> new ForbiddenException("Bạn không thuộc cuộc hội thoại này"));
    Long targetMessageId = lastReadMessageId;
    if (targetMessageId != null && !messageRepository.existsByIdAndConversationId(targetMessageId, conversationId)) {
        throw new BadRequestException("Tin nhắn đã đọc không thuộc hội thoại này");
    }
    Long current = participant.getLastReadMessageId();
    if (targetMessageId != null && (current == null || targetMessageId > current)) {
        participant.setLastReadMessageId(targetMessageId);
    }
    participant.setLastReadAt(LocalDateTime.now());
    return participantRepository.save(participant);
}

@Override
public List<Long> getOtherParticipantIds(Long conversationId, Long userId) {
    if (!participantRepository.existsByConversationIdAndUserId(conversationId, userId)) {
        throw new ForbiddenException("Bạn không thuộc cuộc hội thoại này");
    }
    return participantRepository.findOtherUserIds(conversationId, userId);
}

@Override
@Transactional(readOnly = true)
public List<Long> getRelatedUserIds(Long userId) {
    return participantRepository.findRelatedUserIds(userId);
}
```

Keep existing `markAsRead(Long conversationId, Long userId)` and make it delegate:

```java
@Override
public void markAsRead(Long conversationId, Long userId) {
    markAsRead(conversationId, userId, null);
}
```

- [ ] **Step 8: Update total unread count**

Replace `getTotalUnreadCount` body with:

```java
List<Long> convIds = participantRepository.findConversationIdsByUserId(userId);
return convIds.stream().mapToInt(cId -> {
    Long lastReadMessageId = participantRepository.findByConversationIdAndUserId(cId, userId)
            .map(ConversationParticipant::getLastReadMessageId)
            .orElse(null);
    return (int) messageRepository.countUnreadAfterMessageId(cId, userId, lastReadMessageId);
}).sum();
```

- [ ] **Step 9: Update REST controller**

Change `getMessages` parameters/body to:

```java
public ResponseEntity<ApiResponse<List<MessageDto>>> getMessages(
        @PathVariable Long id,
        @RequestParam(required = false) Long beforeMessageId,
        @RequestParam(required = false) Long afterSeq,
        @RequestParam(defaultValue = "20") int limit) {
    Long userId = SecurityUtil.getCurrentUserId();
    return ResponseEntity.ok(ApiResponse.success(
            messageService.getMessages(id, userId, beforeMessageId, afterSeq, limit)));
}
```

Change `markAsRead` to accept an optional body:

```java
public ResponseEntity<ApiResponse<Void>> markAsRead(
        @PathVariable Long id,
        @RequestBody(required = false) java.util.Map<String, Long> body) {
    Long lastReadMessageId = body == null ? null : body.get("lastReadMessageId");
    conversationService.markAsRead(id, SecurityUtil.getCurrentUserId(), lastReadMessageId);
    return ResponseEntity.ok(ApiResponse.success("Đã đánh dấu đọc", null));
}
```

- [ ] **Step 10: Compile**

Run:

```bash
cd backend && mvn -q -DskipTests compile
```

Expected after Task 3 alone: compile can still fail because `ChatRealtimeService` is not created yet. Continue to Task 4.

---

## Task 4: WebSocket event router and realtime payloads

**Files:**
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/websocket/dto/ClientWsMessage.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/service/ChatRealtimeService.java`
- Modify: `backend/src/main/java/vn/edu/fpt/myfschool/websocket/ChatWebSocketHandler.java:1-55`

**Interfaces:**
- Consumes: Task 1 session manager, Task 3 conversation methods.
- Produces:
  - `ChatRealtimeService.handle(Long userId, WebSocketSession session, String payload)`
  - `ChatRealtimeService.handleConnected(Long userId)`
  - `ChatRealtimeService.handleDisconnected(Long userId)`
  - WS server events: `message.ack`, `message.new`, `message.receipt`, `conversation.read`, `typing.update`, `presence.update`, `error`

- [ ] **Step 1: Create client event DTO**

```java
package vn.edu.fpt.myfschool.websocket.dto;

public record ClientWsMessage(
        String type,
        Long conversationId,
        String clientMessageId,
        String messageType,
        String content,
        Long messageId,
        Long lastReadMessageId
) {
}
```

- [ ] **Step 2: Create ChatRealtimeService skeleton**

```java
package vn.edu.fpt.myfschool.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;
import vn.edu.fpt.myfschool.common.dto.MessageDto;
import vn.edu.fpt.myfschool.common.dto.SendMessageRequest;
import vn.edu.fpt.myfschool.common.enums.MessageType;
import vn.edu.fpt.myfschool.entity.ConversationParticipant;
import vn.edu.fpt.myfschool.entity.MessageReceipt;
import vn.edu.fpt.myfschool.websocket.WebSocketSessionManager;
import vn.edu.fpt.myfschool.websocket.dto.ClientWsMessage;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChatRealtimeService {
    private final ObjectMapper objectMapper;
    private final ConversationService conversationService;
    private final WebSocketSessionManager sessionManager;

    public void handle(Long userId, WebSocketSession session, String payload) {
        ClientWsMessage event;
        try {
            event = objectMapper.readValue(payload, ClientWsMessage.class);
        } catch (Exception ex) {
            sendToSession(session, error("unknown", null, "INVALID_JSON", "Dữ liệu WebSocket không hợp lệ"));
            return;
        }
        switch (event.type() == null ? "" : event.type()) {
            case "message.send" -> handleSend(userId, event);
            case "message.delivered" -> handleDelivered(userId, event);
            case "message.read" -> handleRead(userId, event);
            case "typing.start" -> handleTyping(userId, event, true);
            case "typing.stop" -> handleTyping(userId, event, false);
            case "presence.heartbeat" -> sessionManager.heartbeat(userId);
            default -> sendToUser(userId, error(event.type(), event.clientMessageId(), "UNKNOWN_TYPE", "Loại sự kiện không hỗ trợ"));
        }
    }
```

- [ ] **Step 3: Add send handling**

Append inside `ChatRealtimeService`:

```java
    private void handleSend(Long userId, ClientWsMessage event) {
        try {
            MessageType messageType = event.messageType() == null || event.messageType().isBlank()
                    ? MessageType.TEXT
                    : MessageType.valueOf(event.messageType());
            MessageDto saved = conversationService.sendMessage(
                    event.conversationId(),
                    userId,
                    new SendMessageRequest(event.clientMessageId(), event.content(), messageType)
            );
            sendToUser(userId, Map.of(
                    "type", "message.ack",
                    "clientMessageId", saved.clientMessageId(),
                    "status", "sent",
                    "message", saved
            ));
            List<Long> recipients = conversationService.getOtherParticipantIds(saved.conversationId(), userId);
            Map<String, Object> conversation = new LinkedHashMap<>();
            conversation.put("id", saved.conversationId());
            conversation.put("lastMessage", saved.content());
            conversation.put("lastMessageAt", saved.createdAt());
            conversation.put("unreadCount", 1);
            sendToUsers(recipients, Map.of(
                    "type", "message.new",
                    "message", saved,
                    "conversation", conversation
            ));
        } catch (IllegalArgumentException ex) {
            sendToUser(userId, error("message.send", event.clientMessageId(), "INVALID_MESSAGE_TYPE", "Loại tin nhắn không hợp lệ"));
        } catch (RuntimeException ex) {
            sendToUser(userId, error("message.send", event.clientMessageId(), "MESSAGE_SEND_FAILED", ex.getMessage()));
        }
    }
```

- [ ] **Step 4: Add delivered/read/typing/presence handling**

Append inside `ChatRealtimeService`:

```java
    private void handleDelivered(Long userId, ClientWsMessage event) {
        try {
            MessageReceipt receipt = conversationService.markAsDelivered(event.conversationId(), event.messageId(), userId);
            Long senderId = receipt.getMessage().getSender().getId();
            sendToUser(senderId, Map.of(
                    "type", "message.receipt",
                    "conversationId", event.conversationId(),
                    "messageId", event.messageId(),
                    "userId", userId,
                    "status", "delivered",
                    "deliveredAt", receipt.getDeliveredAt()
            ));
        } catch (RuntimeException ex) {
            sendToUser(userId, error("message.delivered", null, "DELIVERED_FAILED", ex.getMessage()));
        }
    }

    private void handleRead(Long userId, ClientWsMessage event) {
        try {
            ConversationParticipant participant = conversationService.markAsRead(
                    event.conversationId(), userId, event.lastReadMessageId());
            sendToUsers(conversationService.getOtherParticipantIds(event.conversationId(), userId), Map.of(
                    "type", "conversation.read",
                    "conversationId", event.conversationId(),
                    "userId", userId,
                    "lastReadMessageId", participant.getLastReadMessageId(),
                    "readAt", participant.getLastReadAt()
            ));
        } catch (RuntimeException ex) {
            sendToUser(userId, error("message.read", null, "READ_FAILED", ex.getMessage()));
        }
    }

    private void handleTyping(Long userId, ClientWsMessage event, boolean typing) {
        try {
            sendToUsers(conversationService.getOtherParticipantIds(event.conversationId(), userId), Map.of(
                    "type", "typing.update",
                    "conversationId", event.conversationId(),
                    "userId", userId,
                    "typing", typing
            ));
        } catch (RuntimeException ex) {
            sendToUser(userId, error(event.type(), null, "TYPING_FAILED", ex.getMessage()));
        }
    }

    public void handleConnected(Long userId) {
        sendPresence(userId, true, null);
    }

    public void handleDisconnected(Long userId) {
        if (sessionManager.getOpenSessionCount(userId) == 0) {
            sendPresence(userId, false, LocalDateTime.now());
        }
    }

    private void sendPresence(Long userId, boolean online, LocalDateTime lastSeenAt) {
        List<Long> related = conversationService.getRelatedUserIds(userId);
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", "presence.update");
        event.put("userId", userId);
        event.put("online", online);
        event.put("lastSeenAt", lastSeenAt);
        sendToUsers(related, event);
    }
```

- [ ] **Step 5: Add JSON send helpers**

Close `ChatRealtimeService` with:

```java
    private Map<String, Object> error(String requestType, String clientMessageId, String code, String message) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", "error");
        event.put("requestType", requestType);
        event.put("clientMessageId", clientMessageId);
        event.put("code", code);
        event.put("message", message);
        return event;
    }

    private void sendToUser(Long userId, Object event) {
        try {
            sessionManager.sendToUser(userId, objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException ignored) {
        }
    }

    private void sendToUsers(List<Long> userIds, Object event) {
        try {
            sessionManager.sendToUsers(userIds, objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException ignored) {
        }
    }

    private void sendToSession(WebSocketSession session, Object event) {
        try {
            session.sendMessage(new org.springframework.web.socket.TextMessage(objectMapper.writeValueAsString(event)));
        } catch (Exception ignored) {
        }
    }
}
```

- [ ] **Step 6: Compile**

Run:

```bash
cd backend && mvn -q -DskipTests compile
```

Expected: PASS.

---

## Task 5: Backend integration tests

**Files:**
- Create: `backend/src/test/java/vn/edu/fpt/myfschool/ConversationMessagingIntegrationTest.java`
- Modify only if needed: `backend/src/test/java/vn/edu/fpt/myfschool/BaseIntegrationTest.java:30-42`

**Interfaces:**
- Consumes: REST endpoints and service methods from Tasks 2–4.
- Produces: Regression checks for idempotent send, REST history, afterSeq sync, unread/read.

- [ ] **Step 1: Add repository/service injections if missing**

If `BaseIntegrationTest` does not expose these, add:

```java
@Autowired protected ConversationRepository conversationRepository;
@Autowired protected ConversationParticipantRepository conversationParticipantRepository;
@Autowired protected MessageRepository messageRepository;
@Autowired protected vn.edu.fpt.myfschool.service.ConversationService conversationService;
@Autowired protected vn.edu.fpt.myfschool.service.MessageService messageService;
```

- [ ] **Step 2: Create integration test class**

```java
package vn.edu.fpt.myfschool;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import vn.edu.fpt.myfschool.common.dto.SendMessageRequest;
import vn.edu.fpt.myfschool.common.enums.MessageType;
import vn.edu.fpt.myfschool.entity.Conversation;
import vn.edu.fpt.myfschool.entity.ConversationParticipant;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ConversationMessagingIntegrationTest extends BaseIntegrationTest {

    @Test
    void send_message_is_idempotent_by_sender_and_client_message_id() {
        Conversation conversation = createConversation();
        Long parentUserId = testParent.getUser().getId();

        var request = new SendMessageRequest("client-1", "Xin chào cô", MessageType.TEXT);
        var first = conversationService.sendMessage(conversation.getId(), parentUserId, request);
        var second = conversationService.sendMessage(conversation.getId(), parentUserId, request);

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(messageRepository.findByConversationIdOrderByCreatedAtDesc(conversation.getId(), org.springframework.data.domain.PageRequest.of(0, 10))).hasSize(1);
    }

    @Test
    void get_messages_returns_saved_messages_and_after_seq_sync() throws Exception {
        Conversation conversation = createConversation();
        Long parentUserId = testParent.getUser().getId();
        String parentToken = loginAsParent();

        conversationService.sendMessage(conversation.getId(), parentUserId,
                new SendMessageRequest("client-2", "Tin 1", MessageType.TEXT));
        var second = conversationService.sendMessage(conversation.getId(), parentUserId,
                new SendMessageRequest("client-3", "Tin 2", MessageType.TEXT));

        mockMvc.perform(get("/api/conversations/" + conversation.getId())
                        .header("Authorization", authHeader(parentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].content").value("Tin 2"));

        mockMvc.perform(get("/api/conversations/" + conversation.getId())
                        .header("Authorization", authHeader(parentToken))
                        .param("afterSeq", String.valueOf(second.serverSeq() - 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].content").value("Tin 2"));
    }

    @Test
    void mark_read_updates_last_read_message_id() throws Exception {
        Conversation conversation = createConversation();
        Long parentUserId = testParent.getUser().getId();
        String teacherToken = loginAsTeacher();
        var message = conversationService.sendMessage(conversation.getId(), parentUserId,
                new SendMessageRequest("client-4", "Cô đọc giúp em", MessageType.TEXT));

        mockMvc.perform(put("/api/conversations/" + conversation.getId() + "/read")
                        .header("Authorization", authHeader(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lastReadMessageId\":" + message.id() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        ConversationParticipant participant = conversationParticipantRepository
                .findByConversationIdAndUserId(conversation.getId(), testTeacher.getUser().getId())
                .orElseThrow();
        assertThat(participant.getLastReadMessageId()).isEqualTo(message.id());
    }

    @Test
    void sender_outside_conversation_is_forbidden() {
        Conversation conversation = createConversation();
        Long studentUserId = testStudent1.getUser().getId();

        org.junit.jupiter.api.Assertions.assertThrows(
                vn.edu.fpt.myfschool.common.exception.ForbiddenException.class,
                () -> conversationService.sendMessage(conversation.getId(), studentUserId,
                        new SendMessageRequest("client-5", "Không hợp lệ", MessageType.TEXT))
        );
    }

    private Conversation createConversation() {
        Conversation conversation = new Conversation();
        conversation.setLastMessageAt(LocalDateTime.now());
        conversation = conversationRepository.save(conversation);

        ConversationParticipant parent = new ConversationParticipant();
        parent.setConversation(conversation);
        parent.setUser(testParent.getUser());
        parent.setJoinedAt(LocalDateTime.now());
        conversationParticipantRepository.save(parent);

        ConversationParticipant teacher = new ConversationParticipant();
        teacher.setConversation(conversation);
        teacher.setUser(testTeacher.getUser());
        teacher.setJoinedAt(LocalDateTime.now());
        conversationParticipantRepository.save(teacher);

        return conversation;
    }
}
```

- [ ] **Step 3: Run focused tests**

Run:

```bash
cd backend && mvn test -Dtest=ConversationMessagingIntegrationTest
```

Expected: PASS.

- [ ] **Step 4: Run conversation regression tests**

Run:

```bash
cd backend && mvn test -Dtest=ConversationIntegrationTest
```

Expected: PASS.

- [ ] **Step 5: Run full backend tests**

Run:

```bash
cd backend && mvn test
```

Expected: PASS. If unrelated pre-existing failures appear, capture exact failing test names and output.

---

## Task 6: Final review and authorization-gated commit

**Files:**
- Review: all files changed by Tasks 1–5.

**Interfaces:**
- Consumes: all implemented tasks.
- Produces: concise final report and optional commit if user authorizes it.

- [ ] **Step 1: Check working tree**

Run:

```bash
git status --short
```

Expected: changed backend messaging files and tests only, plus this plan file.

- [ ] **Step 2: Inspect diff scope**

Run:

```bash
git diff -- backend/src/main/java/vn/edu/fpt/myfschool backend/src/test/java/vn/edu/fpt/myfschool docs/superpowers/plans/2026-07-02-websocket-messaging-backend-phase5.md
```

Expected: no Flutter files and no Redis dependency.

- [ ] **Step 3: If user explicitly asks for commit, commit on master**

Run only after user authorization:

```bash
git add backend/src/main/java/vn/edu/fpt/myfschool/config/WebSocketConfig.java \
        backend/src/main/java/vn/edu/fpt/myfschool/websocket \
        backend/src/main/java/vn/edu/fpt/myfschool/common/enums/MessageReceiptStatus.java \
        backend/src/main/java/vn/edu/fpt/myfschool/entity/Message.java \
        backend/src/main/java/vn/edu/fpt/myfschool/entity/MessageReceipt.java \
        backend/src/main/java/vn/edu/fpt/myfschool/entity/ConversationParticipant.java \
        backend/src/main/java/vn/edu/fpt/myfschool/repository/MessageRepository.java \
        backend/src/main/java/vn/edu/fpt/myfschool/repository/MessageReceiptRepository.java \
        backend/src/main/java/vn/edu/fpt/myfschool/repository/ConversationParticipantRepository.java \
        backend/src/main/java/vn/edu/fpt/myfschool/common/dto/MessageDto.java \
        backend/src/main/java/vn/edu/fpt/myfschool/service/ConversationService.java \
        backend/src/main/java/vn/edu/fpt/myfschool/service/MessageService.java \
        backend/src/main/java/vn/edu/fpt/myfschool/service/ChatRealtimeService.java \
        backend/src/main/java/vn/edu/fpt/myfschool/service/impl/ConversationServiceImpl.java \
        backend/src/main/java/vn/edu/fpt/myfschool/service/impl/MessageServiceImpl.java \
        backend/src/main/java/vn/edu/fpt/myfschool/controller/ConversationController.java \
        backend/src/test/java/vn/edu/fpt/myfschool/ConversationMessagingIntegrationTest.java \
        docs/superpowers/plans/2026-07-02-websocket-messaging-backend-phase5.md

git commit -m "feat: implement backend websocket messaging" -m "Co-Authored-By: Claude <noreply@anthropic.com>"
```

Expected: commit succeeds only if user requested it.

---

## Self-Review

- Spec coverage: Phase 1 JWT/WS wiring covered by Task 1; Phase 2 receipts by Task 2 and Task 3; Phase 3 read status by Task 3; Phase 4 memory presence by Task 1 and Task 4; Phase 5 typing by Task 4; REST sync by Task 3; tests by Task 5.
- Scope: Redis, Flutter, push notifications, file/image upload, and search are excluded by approved lean backend-only scope.
- Placeholder scan: no placeholder steps remain; each code-changing step includes exact code or exact method body.
- Type consistency: `MessageDto`, `MessageService`, `ConversationService`, repository method names, and WS event names match across tasks.
