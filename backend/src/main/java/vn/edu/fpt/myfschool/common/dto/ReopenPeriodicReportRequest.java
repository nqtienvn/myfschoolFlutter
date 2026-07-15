package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReopenPeriodicReportRequest(
    @NotNull Long academicYearId,
    @NotBlank @Size(max = 500) String reason
) {}
