package vn.edu.fpt.myfschool.common.dto;

import vn.edu.fpt.myfschool.common.enums.TargetRole;
import java.time.LocalDateTime;
import java.util.List;

public record AnnouncementDto(
    Long id, String title, String body, TargetRole targetRole, Boolean requiresReply,
    Long teacherId, String teacherName, List<String> classNames, List<Long> classIds,
    boolean isRead, boolean acknowledged, String replyText, LocalDateTime repliedAt, String recipientStatus,
    int totalRecipients, int readCount, int acknowledgedCount, int repliedCount, LocalDateTime createdAt,
    Long academicYearId, String approvalStatus, String rejectionReason, String senderType,
    String recipientScope, String teacherAudience, Long subjectId, String subjectName
) {
    public AnnouncementDto withIsRead(boolean isRead) {
        return new AnnouncementDto(id, title, body, targetRole, requiresReply,
            teacherId, teacherName, classNames, classIds, isRead, acknowledged, replyText, repliedAt, recipientStatus,
            totalRecipients, readCount, acknowledgedCount, repliedCount, createdAt,
            academicYearId, approvalStatus, rejectionReason, senderType,
            recipientScope, teacherAudience, subjectId, subjectName);
    }
}
