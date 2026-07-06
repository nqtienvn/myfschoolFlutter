package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.common.dto.ConversationDto;
import vn.edu.fpt.myfschool.common.dto.MessageDto;
import vn.edu.fpt.myfschool.common.dto.SearchResultDto;
import vn.edu.fpt.myfschool.common.dto.SendMessageRequest;
import vn.edu.fpt.myfschool.entity.ConversationParticipant;
import vn.edu.fpt.myfschool.entity.MessageReceipt;

import java.util.List;

public interface ConversationService {
    List<ConversationDto> getConversations(Long userId);

    ConversationDto createOrFindConversation(Long userId, Long otherUserId);

    MessageDto sendMessage(Long conversationId, Long senderId, SendMessageRequest request);

    void markAsRead(Long conversationId, Long userId);

    ConversationParticipant markAsRead(Long conversationId, Long userId, Long lastReadMessageId);

    MessageReceipt markAsDelivered(Long conversationId, Long messageId, Long userId);

    List<Long> getOtherParticipantIds(Long conversationId, Long userId);

    List<Long> getRelatedUserIds(Long userId);

    void updateLastSeen(Long userId, java.time.LocalDateTime lastSeenAt);

    int getTotalUnreadCount(Long userId);

    List<SearchResultDto> searchUsers(Long userId, String keyword);

    boolean hasExistingMessage(Long conversationId, Long senderId, String clientMessageId);
}
