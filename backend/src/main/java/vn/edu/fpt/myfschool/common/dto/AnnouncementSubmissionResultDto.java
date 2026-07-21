package vn.edu.fpt.myfschool.common.dto;

import vn.edu.fpt.myfschool.common.enums.AnnouncementDeliveryStatus;

import java.util.List;

public record AnnouncementSubmissionResultDto(
        AnnouncementDeliveryStatus outcome,
        String message,
        AnnouncementDto announcement,
        List<AnnouncementViolationDto> violations
) {}
