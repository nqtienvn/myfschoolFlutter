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
