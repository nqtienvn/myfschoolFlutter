package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SavePeriodicReportRequest(
    @NotNull Long academicYearId,
    @NotNull Long semesterId,
    @NotNull Long classId,
    @Size(max = 2000) String generalComment
) {}
