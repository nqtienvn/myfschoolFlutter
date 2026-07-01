package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.common.dto.AttachmentDto;
import vn.edu.fpt.myfschool.entity.Attachment;
import vn.edu.fpt.myfschool.repository.AttachmentRepository;
import java.util.List;
import java.util.stream.Collectors;

public interface AttachmentService {
    AttachmentDto createAttachment(String fileUrl, String fileName, Long fileSize, String mimeType);

    List<AttachmentDto> getByLeaveRequest(Long leaveRequestId);
}
