package vn.edu.fpt.myfschool.common.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AnnouncementPolicyDto(
        Long academicYearId,
        boolean enabled,
        String rejectionMessage,
        List<AnnouncementPolicyRuleDto> rules,
        LocalDateTime updatedAt
) {}
