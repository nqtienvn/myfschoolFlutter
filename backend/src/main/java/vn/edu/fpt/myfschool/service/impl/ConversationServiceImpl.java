package vn.edu.fpt.myfschool.service.impl;

import vn.edu.fpt.myfschool.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

@Service("conversationService")
@RequiredArgsConstructor
@Transactional
public class ConversationServiceImpl implements ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    @Override
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

    @Override
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

    @Override
    public MessageDto sendMessage(Long conversationId, Long senderId, SendMessageRequest request) {
        MessageType messageType = resolveMessageType(request);
        String clientMessageId = normalizeClientMessageId(request);
        String content = normalizeContent(request, messageType);

        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", conversationId));
        if (!participantRepository.existsByConversationIdAndUserId(conversationId, senderId)) {
            throw new ForbiddenException("Bạn không thuộc cuộc hội thoại này");
        }

        User sender = userRepository.findById(senderId).orElseThrow();
        var existingMessage = messageRepository.findBySenderIdAndClientMessageId(
                senderId,
                clientMessageId
        );
        if (existingMessage.isPresent()) {
            Message msg = existingMessage.get();
            if (!msg.getConversation().getId().equals(conversationId)) {
                throw new ConflictException("clientMessageId đã được dùng cho hội thoại khác");
            }
            return toMessageDto(msg, senderId);
        }

        Message msg = new Message();
        msg.setConversation(conv);
        msg.setSender(sender);
        msg.setContent(content);
        msg.setClientMessageId(clientMessageId);
        msg.setMessageType(messageType);
        msg.setServerSeq(messageRepository.findMaxServerSeq(conversationId) + 1);
        msg = messageRepository.save(msg);

        conv.setLastMessage(content);
        conv.setLastMessageAt(msg.getCreatedAt());
        conversationRepository.save(conv);

        return toMessageDto(msg, senderId);
    }

    private MessageType resolveMessageType(SendMessageRequest request) {
        return request.messageType() == null ? MessageType.TEXT : request.messageType();
    }

    private String normalizeClientMessageId(SendMessageRequest request) {
        String clientMessageId = request.clientMessageId() == null ? "" : request.clientMessageId().trim();
        if (clientMessageId.isEmpty()) {
            throw new BadRequestException("clientMessageId không được để trống");
        }
        if (clientMessageId.length() > 80) {
            throw new BadRequestException("clientMessageId không được vượt quá 80 ký tự");
        }
        return clientMessageId;
    }

    private String normalizeContent(SendMessageRequest request, MessageType messageType) {
        String content = request.content() == null ? "" : request.content().trim();
        if (messageType == MessageType.TEXT && content.isEmpty()) {
            throw new BadRequestException("Nội dung tin nhắn không được để trống");
        }
        if (content.length() > 4000) {
            throw new BadRequestException("Nội dung tin nhắn không được vượt quá 4000 ký tự");
        }
        return content;
    }

    private MessageDto toMessageDto(Message msg, Long currentUserId) {
        return new MessageDto(
                msg.getId(),
                msg.getClientMessageId(),
                msg.getConversation().getId(),
                msg.getSender().getId(),
                msg.getSender().getName(),
                msg.getMessageType(),
                msg.getContent(),
                msg.getServerSeq(),
                msg.getSender().getId().equals(currentUserId),
                msg.getCreatedAt(),
                List.of()
        );
    }

    @Override
    public void markAsRead(Long conversationId, Long userId) {
        participantRepository.findByConversationIdAndUserId(conversationId, userId)
                .ifPresent(cp -> {
                    cp.setLastReadAt(LocalDateTime.now());
                    participantRepository.save(cp);
                });
    }

    @Transactional(readOnly = true)
    @Override
    public int getTotalUnreadCount(Long userId) {
        List<Long> convIds = participantRepository.findConversationIdsByUserId(userId);
        return convIds.stream().mapToInt(cId -> (int) messageRepository.countUnread(cId, userId)).sum();
    }
}
