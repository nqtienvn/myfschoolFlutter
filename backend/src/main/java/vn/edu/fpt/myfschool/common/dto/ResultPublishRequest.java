package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.NotNull;

public record ResultPublishRequest(
        @NotNull Long academicYearId,
        @NotNull Long semesterId,
        @NotNull Long classId
) {}
