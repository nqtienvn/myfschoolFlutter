package vn.edu.fpt.myfschool.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.entity.*;
import vn.edu.fpt.myfschool.repository.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ConversationDto> getConversations(Long userId) {
        List<Conversation> conversations = conversationRepository.findConversationsByUserId(userId);
        return conversations.stream().map(c -> {
            ConversationParticipant other = c.getParticipants().stream()
                .filter(p -> !p.getUser().getId().equals(userId)).findFirst().orElse(null);
            long unread = messageRepository.countUnread(c.getId(), userId);
            ParticipantDto otherDto = other != null ? new ParticipantDto(
                other.getUser().getId(), other.getUser().getName(),
                other.getUser().getAvatar(), other.getUser().getRole()) : null;
            return new ConversationDto(c.getId(), c.getLastMessage(), c.getLastMessageAt(),
                (int) unread, otherDto);
        }).collect(Collectors.toList());
    }

    public ConversationDto createOrFindConversation(Long userId, Long otherUserId) {
        var existing = conversationRepository.findConversationBetweenUsers(userId, otherUserId);
        if (existing.isPresent()) {
            return getConversations(userId).stream()
                .filter(c -> c.id().equals(existing.get().getId())).findFirst()
                .orElse(null);
        }

        Conversation conv = new Conversation();
        conv = conversationRepository.save(conv);

        ConversationParticipant p1 = new ConversationParticipant();
        p1.setConversation(conv);
        p1.setUser(userRepository.findById(userId).orElseThrow());
        p1.setJoinedAt(LocalDateTime.now());
        participantRepository.save(p1);

        ConversationParticipant p2 = new ConversationParticipant();
        p2.setConversation(conv);
        p2.setUser(userRepository.findById(otherUserId).orElseThrow());
        p2.setJoinedAt(LocalDateTime.now());
        participantRepository.save(p2);

        User other = userRepository.findById(otherUserId).orElseThrow();
        return new ConversationDto(conv.getId(), null, null, 0,
            new ParticipantDto(other.getId(), other.getName(), other.getAvatar(), other.getRole()));
    }

    public MessageDto sendMessage(Long conversationId, Long senderId, SendMessageRequest request) {
        Conversation conv = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", conversationId));
        User sender = userRepository.findById(senderId).orElseThrow();

        Message msg = new Message();
        msg.setConversation(conv);
        msg.setSender(sender);
        msg.setContent(request.content());
        msg = messageRepository.save(msg);

        conv.setLastMessage(request.content());
        conv.setLastMessageAt(msg.getCreatedAt());
        conversationRepository.save(conv);

        return new MessageDto(msg.getId(), sender.getId(), sender.getName(),
            request.content(), true, msg.getCreatedAt(), List.of());
    }

    public void markAsRead(Long conversationId, Long userId) {
        participantRepository.findByConversationIdAndUserId(conversationId, userId)
            .ifPresent(cp -> {
                cp.setLastReadAt(LocalDateTime.now());
                participantRepository.save(cp);
            });
    }

    @Transactional(readOnly = true)
    public int getTotalUnreadCount(Long userId) {
        List<Long> convIds = participantRepository.findConversationIdsByUserId(userId);
        return convIds.stream().mapToInt(cId -> (int) messageRepository.countUnread(cId, userId)).sum();
    }
}
