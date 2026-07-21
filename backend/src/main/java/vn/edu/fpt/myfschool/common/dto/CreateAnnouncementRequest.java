package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import vn.edu.fpt.myfschool.common.enums.TargetRole;

import java.util.List;

public record CreateAnnouncementRequest(
    @NotBlank @Size(max = 500) String title,
    @NotBlank @Size(max = 10000) String body,
    @NotNull TargetRole targetRole,
    Long academicYearId,
    @NotEmpty List<Long> classIds,
    Long retryOfAnnouncementId
) {}
