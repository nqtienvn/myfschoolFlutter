package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateTeachingAssignmentRequest(
    @NotNull Long classId,
    @NotNull Long subjectId,
    @NotNull Long teacherId,
    Long semesterId,
    LocalDate effectiveFrom
) {}
