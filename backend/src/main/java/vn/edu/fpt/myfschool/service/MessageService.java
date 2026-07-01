package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.common.dto.MessageDto;

import java.util.List;

public interface MessageService {
    List<MessageDto> getMessages(Long conversationId, Long userId, Long beforeMessageId, Long afterSeq, int limit);
}
