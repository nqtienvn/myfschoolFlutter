package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.NotNull;

public record ViolationScopeRequest(
        @NotNull Long academicYearId,
        @NotNull Long semesterId,
        @NotNull Long classId
) {}
