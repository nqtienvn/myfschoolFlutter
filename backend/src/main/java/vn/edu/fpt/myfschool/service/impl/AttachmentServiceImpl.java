package vn.edu.fpt.myfschool.service.impl;

import vn.edu.fpt.myfschool.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.AttachmentDto;
import vn.edu.fpt.myfschool.entity.Attachment;
import vn.edu.fpt.myfschool.repository.AttachmentRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service("attachmentService")
@RequiredArgsConstructor
@Transactional
public class AttachmentServiceImpl implements AttachmentService {

    private final AttachmentRepository attachmentRepository;

    @Override
    public AttachmentDto createAttachment(String fileUrl, String fileName, Long fileSize, String mimeType) {
        Attachment att = new Attachment();
        att.setFileUrl(fileUrl);
        att.setFileName(fileName);
        att.setFileSize(fileSize.intValue());
        att.setMimeType(mimeType);
        att = attachmentRepository.save(att);
        return toDto(att);
    }

    @Transactional(readOnly = true)
    @Override
    public List<AttachmentDto> getByLeaveRequest(Long leaveRequestId) {
        return attachmentRepository.findByLeaveRequestId(leaveRequestId)
            .stream().map(this::toDto).collect(Collectors.toList());
    }

    private AttachmentDto toDto(Attachment a) {
        return new AttachmentDto(a.getId(), a.getFileUrl(), a.getFileName(),
            Long.valueOf(a.getFileSize()), a.getMimeType());
    }
}
