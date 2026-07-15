package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.*;
import vn.edu.fpt.myfschool.common.enums.ParentContactType;

import java.time.LocalDateTime;

public record SaveParentContactLogRequest(
        @NotNull Long academicYearId,
        @NotNull Long semesterId,
        @NotNull Long classId,
        @NotNull ParentContactType contactType,
        @NotBlank @Size(max = 200) String subject,
        @NotBlank @Size(max = 2000) String summary,
        @Size(max = 1000) String result,
        @NotNull LocalDateTime contactedAt,
        LocalDateTime nextActionAt
) {}
