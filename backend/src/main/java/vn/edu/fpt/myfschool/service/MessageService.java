package vn.edu.fpt.myfschool.service;

import org.springframework.data.domain.PageRequest;
import vn.edu.fpt.myfschool.common.dto.MessageDto;
import vn.edu.fpt.myfschool.entity.Message;
import vn.edu.fpt.myfschool.repository.MessageRepository;
import java.util.List;
import java.util.stream.Collectors;

public interface MessageService {
    List<MessageDto> getMessages(Long conversationId, Long userId, Long beforeMessageId, int limit);
}
