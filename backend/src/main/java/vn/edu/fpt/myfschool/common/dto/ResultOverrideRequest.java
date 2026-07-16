package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ResultOverrideRequest(
        @NotNull Long academicYearId,
        @NotNull Long semesterId,
        @NotNull Long classId,
        @NotBlank @Size(max = 50) String academicAbility,
        @NotBlank @Size(max = 50) String conduct,
        @NotBlank @Size(max = 50) String honor
) {}
