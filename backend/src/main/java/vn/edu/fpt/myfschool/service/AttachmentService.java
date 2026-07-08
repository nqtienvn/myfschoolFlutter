package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.common.dto.AttachmentDto;

import java.util.List;

public interface AttachmentService {
    AttachmentDto createAttachment(String fileUrl, String fileName, Long fileSize, String mimeType);

    List<AttachmentDto> getByLeaveRequest(Long leaveRequestId);
}
