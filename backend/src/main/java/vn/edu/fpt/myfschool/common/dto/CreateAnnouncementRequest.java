package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.*;
import java.util.List;
import vn.edu.fpt.myfschool.common.enums.TargetRole;

public record CreateAnnouncementRequest(
    @NotBlank @Size(max = 500) String title, @NotBlank String body,
    @NotNull TargetRole targetRole, Boolean requiresReply, Long academicYearId,
    @NotEmpty List<Long> classIds
) {}
