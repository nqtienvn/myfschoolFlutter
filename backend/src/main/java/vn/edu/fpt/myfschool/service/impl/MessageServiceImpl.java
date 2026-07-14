package vn.edu.fpt.myfschool.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.MessageDto;
import vn.edu.fpt.myfschool.common.exception.ForbiddenException;
import vn.edu.fpt.myfschool.entity.Message;
import vn.edu.fpt.myfschool.entity.MessageReceipt;
import vn.edu.fpt.myfschool.common.enums.MessageReceiptStatus;
import vn.edu.fpt.myfschool.repository.ConversationParticipantRepository;
import vn.edu.fpt.myfschool.repository.MessageReceiptRepository;
import vn.edu.fpt.myfschool.repository.MessageRepository;
import vn.edu.fpt.myfschool.service.MessageService;

import java.util.List;
import java.util.stream.Collectors;

@Service("messageService")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final MessageReceiptRepository messageReceiptRepository;
    private final ConversationParticipantRepository participantRepository;

    @Override
    public List<MessageDto> getMessages(Long conversationId, Long userId, Long beforeMessageId, Long afterSeq, int limit) {
        if (!participantRepository.existsByConversationIdAndUserId(conversationId, userId)) {
            throw new ForbiddenException("Bạn không thuộc cuộc hội thoại này");
        }

        int pageSize = Math.max(1, Math.min(limit, 100));
        List<Message> messages;
        if (afterSeq != null) {
            messages = messageRepository.findByConversationIdAndServerSeqGreaterThanOrderByServerSeqAsc(
                    conversationId, afterSeq, PageRequest.of(0, pageSize));
        } else if (beforeMessageId != null) {
            messages = messageRepository.findMessagesBefore(conversationId, beforeMessageId, PageRequest.of(0, pageSize));
        } else {
            messages = messageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, PageRequest.of(0, pageSize));
        }
        return messages.stream().map(m -> toDto(m, userId)).collect(Collectors.toList());
    }

    private MessageDto toDto(Message m, Long userId) {
        boolean isMine = m.getSender().getId().equals(userId);
        return new MessageDto(
                m.getId(),
                m.getClientMessageId(),
                m.getConversation().getId(),
                m.getSender().getId(),
                m.getSender().getName(),
                m.getMessageType(),
                m.getContent(),
                m.getServerSeq(),
                isMine,
                isMine ? resolveReceiptStatus(m.getId()) : "delivered",
                m.getCreatedAt(),
                List.of()
        );
    }

    private String resolveReceiptStatus(Long messageId) {
        List<MessageReceipt> receipts = messageReceiptRepository.findByMessageId(messageId);
        if (receipts.stream().anyMatch(r -> r.getStatus() == MessageReceiptStatus.READ)) {
            return "read";
        }
        return receipts.isEmpty() ? "sent" : "delivered";
    }
}
