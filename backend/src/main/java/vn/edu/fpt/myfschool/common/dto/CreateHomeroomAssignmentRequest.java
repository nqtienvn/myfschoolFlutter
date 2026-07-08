package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateHomeroomAssignmentRequest(
    @NotNull Long classId,
    @NotNull Long teacherId,
    @NotNull Long academicYearId,
    @NotNull LocalDate effectiveFrom
) {}