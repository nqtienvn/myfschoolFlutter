package vn.edu.fpt.myfschool;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import vn.edu.fpt.myfschool.common.dto.SendMessageRequest;
import vn.edu.fpt.myfschool.common.enums.MessageType;
import vn.edu.fpt.myfschool.entity.Conversation;
import vn.edu.fpt.myfschool.entity.ConversationParticipant;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
