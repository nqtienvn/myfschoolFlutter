package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.NotNull;

public record AcademicYearResultRequest(
        @NotNull Long academicYearId,
        @NotNull Long classId
) {}
