package vn.edu.fpt.myfschool.common.dto;

import java.util.List;

public record ConversationDetailDto(Long id, ParticipantDto otherParticipant, List<MessageDto> messages) {}
