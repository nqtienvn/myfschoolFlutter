package vn.edu.fpt.myfschool;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import vn.edu.fpt.myfschool.common.dto.SendMessageRequest;
import vn.edu.fpt.myfschool.common.enums.MessageType;
import vn.edu.fpt.myfschool.entity.Conversation;
import vn.edu.fpt.myfschool.entity.ConversationParticipant;
import vn.edu.fpt.myfschool.service.ChatRealtimeService;
import vn.edu.fpt.myfschool.websocket.WebSocketSessionManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ConversationMessagingIntegrationTest extends BaseIntegrationTest {

    @Autowired ChatRealtimeService chatRealtimeService;
    @Autowired WebSocketSessionManager sessionManager;

    @Test
    void realtime_send_marks_recipient_message_delivered_and_skips_duplicate_push() throws Exception {
        Conversation conversation = createConversation();
        Long parentUserId = testParent.getUser().getId();
        Long teacherUserId = testTeacher.getUser().getId();
        var parentSession = new StubWebSocketSession();
        var teacherSession = new StubWebSocketSession();
        sessionManager.addSession(parentUserId, parentSession);
        sessionManager.addSession(teacherUserId, teacherSession);

        String payload = """
                {"type":"message.send","conversationId":%d,"clientMessageId":"client-ws-1","messageType":"TEXT","content":"Xin chào cô"}
                """.formatted(conversation.getId());

        chatRealtimeService.handle(parentUserId, parentSession, payload);
        chatRealtimeService.handle(parentUserId, parentSession, payload);

        assertThat(events(parentSession, "message.ack")).hasSize(2);
        assertThat(events(teacherSession, "message.new")).hasSize(1);
        assertThat(events(teacherSession, "message.new").getFirst().at("/message/status").asText()).isEqualTo("delivered");
    }

    @Test
    void realtime_non_member_send_returns_membership_error_code() throws Exception {
        Conversation conversation = createConversation();
        Long outsiderUserId = testStudent1.getUser().getId();
        var outsiderSession = new StubWebSocketSession();
        sessionManager.addSession(outsiderUserId, outsiderSession);

        chatRealtimeService.handle(outsiderUserId, outsiderSession, """
                {"type":"message.send","conversationId":%d,"clientMessageId":"client-hack-1","messageType":"TEXT","content":"Hack message"}
                """.formatted(conversation.getId()));

        JsonNode error = events(outsiderSession, "error").getFirst();
        assertThat(error.get("code").asText()).isEqualTo("NOT_CONVERSATION_MEMBER");
        assertThat(error.get("clientMessageId").asText()).isEqualTo("client-hack-1");
    }

    @Test
    void send_message_is_idempotent_by_sender_and_client_message_id() {
        Conversation conversation = createConversation();
        Long parentUserId = testParent.getUser().getId();

        var request = new SendMessageRequest("client-1", "Xin chào cô", MessageType.TEXT);
        var first = conversationService.sendMessage(conversation.getId(), parentUserId, request);
        var second = conversationService.sendMessage(conversation.getId(), parentUserId, request);

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(messageRepository.findByConversationIdOrderByCreatedAtDesc(
                conversation.getId(), PageRequest.of(0, 10))).hasSize(1);
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
    void mark_read_without_body_reads_latest_message() throws Exception {
        Conversation conversation = createConversation();
        Long parentUserId = testParent.getUser().getId();
        String teacherToken = loginAsTeacher();
        var message = conversationService.sendMessage(conversation.getId(), parentUserId,
                new SendMessageRequest("client-5", "Tin mới nhất", MessageType.TEXT));

        mockMvc.perform(put("/api/conversations/" + conversation.getId() + "/read")
                        .header("Authorization", authHeader(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        ConversationParticipant participant = conversationParticipantRepository
                .findByConversationIdAndUserId(conversation.getId(), testTeacher.getUser().getId())
                .orElseThrow();
        assertThat(participant.getLastReadMessageId()).isEqualTo(message.id());
    }

    @Test
    void delivered_receipt_is_upserted_for_recipient() {
        Conversation conversation = createConversation();
        Long parentUserId = testParent.getUser().getId();
        Long teacherUserId = testTeacher.getUser().getId();
        var message = conversationService.sendMessage(conversation.getId(), parentUserId,
                new SendMessageRequest("client-6", "Xác nhận đã nhận", MessageType.TEXT));

        var first = conversationService.markAsDelivered(conversation.getId(), message.id(), teacherUserId);
        var second = conversationService.markAsDelivered(conversation.getId(), message.id(), teacherUserId);

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(second.getDeliveredAt()).isNotNull();
    }

    @Test
    void sender_outside_conversation_is_forbidden() {
        Conversation conversation = createConversation();
        Long studentUserId = testStudent1.getUser().getId();

        assertThrows(
                vn.edu.fpt.myfschool.common.exception.ForbiddenException.class,
                () -> conversationService.sendMessage(conversation.getId(), studentUserId,
                        new SendMessageRequest("client-7", "Không hợp lệ", MessageType.TEXT))
        );
    }

    private List<JsonNode> events(StubWebSocketSession session, String type) throws Exception {
        List<JsonNode> result = new ArrayList<>();
        for (String message : session.sentMessages) {
            JsonNode json = objectMapper.readTree(message);
            if (type.equals(json.get("type").asText())) {
                result.add(json);
            }
        }
        return result;
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

    private static final class StubWebSocketSession implements WebSocketSession {
        private final List<String> sentMessages = new ArrayList<>();

        @Override
        public String getId() {
            return "stub";
        }

        @Override
        public java.net.URI getUri() {
            return null;
        }

        @Override
        public org.springframework.http.HttpHeaders getHandshakeHeaders() {
            return org.springframework.http.HttpHeaders.EMPTY;
        }

        @Override
        public java.util.Map<String, Object> getAttributes() {
            return java.util.Map.of();
        }

        @Override
        public java.security.Principal getPrincipal() {
            return null;
        }

        @Override
        public java.net.InetSocketAddress getLocalAddress() {
            return null;
        }

        @Override
        public java.net.InetSocketAddress getRemoteAddress() {
            return null;
        }

        @Override
        public String getAcceptedProtocol() {
            return null;
        }

        @Override
        public void setTextMessageSizeLimit(int messageSizeLimit) {
        }

        @Override
        public int getTextMessageSizeLimit() {
            return 8192;
        }

        @Override
        public void setBinaryMessageSizeLimit(int messageSizeLimit) {
        }

        @Override
        public int getBinaryMessageSizeLimit() {
            return 8192;
        }

        @Override
        public java.util.List<org.springframework.web.socket.WebSocketExtension> getExtensions() {
            return java.util.List.of();
        }

        @Override
        public void sendMessage(org.springframework.web.socket.WebSocketMessage<?> message) {
            sentMessages.add(message.getPayload().toString());
        }

        @Override
        public boolean isOpen() {
            return true;
        }

        @Override
        public void close() {
        }

        @Override
        public void close(org.springframework.web.socket.CloseStatus status) {
        }
    }
}
