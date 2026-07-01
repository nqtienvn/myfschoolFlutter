package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.common.dto.ConversationDto;
import vn.edu.fpt.myfschool.common.dto.MessageDto;
import vn.edu.fpt.myfschool.common.dto.ParticipantDto;
import vn.edu.fpt.myfschool.common.dto.SendMessageRequest;
import vn.edu.fpt.myfschool.common.enums.MessageType;
import vn.edu.fpt.myfschool.common.exception.BadRequestException;
import vn.edu.fpt.myfschool.common.exception.ConflictException;
import vn.edu.fpt.myfschool.common.exception.ForbiddenException;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.entity.Conversation;
import vn.edu.fpt.myfschool.entity.ConversationParticipant;
import vn.edu.fpt.myfschool.entity.Message;
import vn.edu.fpt.myfschool.entity.User;
import vn.edu.fpt.myfschool.repository.ConversationParticipantRepository;
import vn.edu.fpt.myfschool.repository.ConversationRepository;
import vn.edu.fpt.myfschool.repository.MessageRepository;
import vn.edu.fpt.myfschool.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public interface ConversationService {
    List<ConversationDto> getConversations(Long userId);

    ConversationDto createOrFindConversation(Long userId, Long otherUserId);

    MessageDto sendMessage(Long conversationId, Long senderId, SendMessageRequest request);

    void markAsRead(Long conversationId, Long userId);

    int getTotalUnreadCount(Long userId);
}
