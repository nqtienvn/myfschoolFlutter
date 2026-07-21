package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.NotNull;

public record ResultCloseRequest(
        @NotNull Long academicYearId,
        @NotNull Long semesterId
) {}
