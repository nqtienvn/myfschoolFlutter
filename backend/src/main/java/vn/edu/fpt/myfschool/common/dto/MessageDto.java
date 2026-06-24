package vn.edu.fpt.myfschool.common.dto;

import java.time.LocalDateTime;
import java.util.List;

public record MessageDto(
    Long id, Long senderId, String senderName, String content,
    boolean isMine, LocalDateTime createdAt, List<AttachmentDto> attachments
) {}
