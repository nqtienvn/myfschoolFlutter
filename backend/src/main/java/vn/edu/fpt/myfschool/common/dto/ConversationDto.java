package vn.edu.fpt.myfschool.common.dto;

import java.time.LocalDateTime;

public record ConversationDto(
    Long id, String lastMessage, LocalDateTime lastMessageAt,
    int unreadCount, ParticipantDto otherParticipant
) {}
