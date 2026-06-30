package vn.edu.fpt.myfschool.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.MessageDto;
import vn.edu.fpt.myfschool.entity.Message;
import vn.edu.fpt.myfschool.repository.MessageRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MessageService {

    private final MessageRepository messageRepository;

    public List<MessageDto> getMessages(Long conversationId, Long userId, Long beforeMessageId, int limit) {
        List<Message> messages;
        if (beforeMessageId != null) {
            messages = messageRepository.findMessagesBefore(conversationId, beforeMessageId, PageRequest.of(0, limit));
        } else {
            messages = messageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, PageRequest.of(0, limit));
        }
        return messages.stream().map(m -> new MessageDto(
                m.getId(),
                m.getClientMessageId(),
                m.getConversation().getId(),
                m.getSender().getId(),
                m.getSender().getName(),
                m.getMessageType(),
                m.getContent(),
                m.getServerSeq(),
                m.getSender().getId().equals(userId),
                m.getCreatedAt(),
                List.of()
        )).collect(Collectors.toList());
    }
}
