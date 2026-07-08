package vn.edu.fpt.myfschool.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import vn.edu.fpt.myfschool.common.dto.MessageDto;
import vn.edu.fpt.myfschool.common.dto.SendMessageRequest;
import vn.edu.fpt.myfschool.common.enums.MessageType;
import vn.edu.fpt.myfschool.common.exception.ForbiddenException;
import vn.edu.fpt.myfschool.entity.ConversationParticipant;
import vn.edu.fpt.myfschool.entity.MessageReceipt;
import vn.edu.fpt.myfschool.service.ChatRealtimeService;
import vn.edu.fpt.myfschool.service.ConversationService;
import vn.edu.fpt.myfschool.websocket.WebSocketSessionManager;
import vn.edu.fpt.myfschool.websocket.dto.ClientWsMessage;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service("chatRealtimeService")
@RequiredArgsConstructor
public class ChatRealtimeServiceImpl implements ChatRealtimeService {
    private final ObjectMapper objectMapper;
    private final ConversationService conversationService;
    private final WebSocketSessionManager sessionManager;

    @Override
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

    @Override
    public void handleConnected(Long userId) {
        sendPresence(userId, true, null);
    }

    @Override
    public void handleDisconnected(Long userId) {
        if (sessionManager.getOpenSessionCount(userId) == 0) {
            LocalDateTime lastSeenAt = LocalDateTime.now();
            conversationService.updateLastSeen(userId, lastSeenAt);
            sendPresence(userId, false, lastSeenAt);
        }
    }

    private void handleSend(Long userId, ClientWsMessage event) {
        try {
            boolean isNew = !conversationService.hasExistingMessage(
                    event.conversationId(), userId, event.clientMessageId());
            MessageType messageType = event.messageType() == null || event.messageType().isBlank()
                    ? MessageType.TEXT
                    : MessageType.valueOf(event.messageType());
            MessageDto saved = conversationService.sendMessage(
                    event.conversationId(),
                    userId,
                    new SendMessageRequest(event.clientMessageId(), event.content(), messageType)
            );
            Map<String, Object> ack = new LinkedHashMap<>();
            ack.put("type", "message.ack");
            ack.put("clientMessageId", saved.clientMessageId());
            ack.put("status", "sent");
            ack.put("message", saved);
            sendToUser(userId, ack);

            if (isNew) {
                List<Long> recipients = conversationService.getOtherParticipantIds(saved.conversationId(), userId);
                Map<String, Object> conversation = new LinkedHashMap<>();
                conversation.put("id", saved.conversationId());
                conversation.put("lastMessage", saved.content());
                conversation.put("lastMessageAt", saved.createdAt());
                conversation.put("unreadCount", 1);

                Map<String, Object> recipientMsg = new LinkedHashMap<>();
                recipientMsg.put("id", saved.id());
                recipientMsg.put("clientMessageId", saved.clientMessageId());
                recipientMsg.put("conversationId", saved.conversationId());
                recipientMsg.put("senderId", saved.senderId());
                recipientMsg.put("senderName", saved.senderName());
                recipientMsg.put("senderAvatar", saved.senderAvatar());
                recipientMsg.put("messageType", saved.messageType());
                recipientMsg.put("content", saved.content());
                recipientMsg.put("serverSeq", saved.serverSeq());
                recipientMsg.put("isMine", false);
                recipientMsg.put("status", "delivered");
                recipientMsg.put("createdAt", saved.createdAt());
                recipientMsg.put("attachments", saved.attachments());

                Map<String, Object> messageNew = new LinkedHashMap<>();
                messageNew.put("type", "message.new");
                messageNew.put("message", recipientMsg);
                messageNew.put("conversation", conversation);
                sendToUsers(recipients, messageNew);
            }
        } catch (IllegalArgumentException ex) {
            sendToUser(userId, error("message.send", event.clientMessageId(), "INVALID_MESSAGE_TYPE", "Loại tin nhắn không hợp lệ"));
        } catch (ForbiddenException ex) {
            sendToUser(userId, error("message.send", event.clientMessageId(), "NOT_CONVERSATION_MEMBER", ex.getMessage()));
        } catch (RuntimeException ex) {
            sendToUser(userId, error("message.send", event.clientMessageId(), "MESSAGE_SEND_FAILED", ex.getMessage()));
        }
    }

    private void handleDelivered(Long userId, ClientWsMessage event) {
        try {
            MessageReceipt receipt = conversationService.markAsDelivered(event.conversationId(), event.messageId(), userId);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("type", "message.receipt");
            payload.put("conversationId", event.conversationId());
            payload.put("messageId", event.messageId());
            payload.put("userId", userId);
            payload.put("status", "delivered");
            payload.put("deliveredAt", receipt.getDeliveredAt());
            sendToUser(receipt.getMessage().getSender().getId(), payload);
        } catch (RuntimeException ex) {
            sendToUser(userId, error("message.delivered", null, "DELIVERED_FAILED", ex.getMessage()));
        }
    }

    private void handleRead(Long userId, ClientWsMessage event) {
        try {
            ConversationParticipant participant = conversationService.markAsRead(
                    event.conversationId(), userId, event.lastReadMessageId());
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("type", "conversation.read");
            payload.put("conversationId", event.conversationId());
            payload.put("userId", userId);
            payload.put("lastReadMessageId", participant.getLastReadMessageId());
            payload.put("readAt", participant.getLastReadAt());
            sendToUsers(conversationService.getOtherParticipantIds(event.conversationId(), userId), payload);
        } catch (RuntimeException ex) {
            sendToUser(userId, error("message.read", null, "READ_FAILED", ex.getMessage()));
        }
    }

    private void handleTyping(Long userId, ClientWsMessage event, boolean typing) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("type", "typing.update");
            payload.put("conversationId", event.conversationId());
            payload.put("userId", userId);
            payload.put("typing", typing);
            sendToUsers(conversationService.getOtherParticipantIds(event.conversationId(), userId), payload);
        } catch (RuntimeException ex) {
            sendToUser(userId, error(event.type(), null, "TYPING_FAILED", ex.getMessage()));
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
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(event)));
        } catch (Exception ignored) {
        }
    }
}
