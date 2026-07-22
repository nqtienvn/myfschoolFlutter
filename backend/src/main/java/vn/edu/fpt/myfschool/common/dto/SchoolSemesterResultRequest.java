package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.NotNull;

/** Scope for semester-result actions that apply to every active class in a school year. */
public record SchoolSemesterResultRequest(
        @NotNull Long academicYearId,
        @NotNull Long semesterId
) {}
