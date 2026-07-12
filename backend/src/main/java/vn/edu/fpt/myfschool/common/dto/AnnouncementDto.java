package vn.edu.fpt.myfschool.common.dto;

import vn.edu.fpt.myfschool.common.enums.TargetRole;
import java.time.LocalDateTime;
import java.util.List;

public record AnnouncementDto(
    Long id, String title, String body, TargetRole targetRole, Boolean requiresReply,
    Long teacherId, String teacherName, List<String> classNames,
    boolean isRead, int totalRecipients, int readCount, LocalDateTime createdAt,
    Long academicYearId, String approvalStatus, String rejectionReason, String senderType,
    String recipientScope, String teacherAudience, Long subjectId, String subjectName
) {
    public AnnouncementDto withIsRead(boolean isRead) {
        return new AnnouncementDto(id, title, body, targetRole, requiresReply,
            teacherId, teacherName, classNames, isRead, totalRecipients, readCount, createdAt,
            academicYearId, approvalStatus, rejectionReason, senderType,
            recipientScope, teacherAudience, subjectId, subjectName);
    }
}
