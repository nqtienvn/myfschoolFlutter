package vn.edu.fpt.myfschool.common.dto;

import vn.edu.fpt.myfschool.common.enums.AnnouncementDeliveryStatus;
import vn.edu.fpt.myfschool.common.enums.TargetRole;

import java.time.LocalDateTime;
import java.util.List;

public record AnnouncementDto(
    Long id,
    String title,
    String body,
    TargetRole targetRole,
    Long teacherId,
    String teacherName,
    List<String> classNames,
    List<Long> classIds,
    boolean isRead,
    LocalDateTime createdAt,
    Long academicYearId,
    AnnouncementDeliveryStatus deliveryStatus,
    String systemRejectionMessage,
    String senderType,
    String recipientScope,
    Long retryOfAnnouncementId,
    List<AnnouncementViolationDto> violations
) {}
