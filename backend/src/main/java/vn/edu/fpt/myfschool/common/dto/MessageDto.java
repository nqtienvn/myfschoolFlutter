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
        MessageType messageType,
        String content,
        Long serverSeq,
        boolean isMine,
        String status,
        LocalDateTime createdAt,
        List<AttachmentDto> attachments
) {
}
