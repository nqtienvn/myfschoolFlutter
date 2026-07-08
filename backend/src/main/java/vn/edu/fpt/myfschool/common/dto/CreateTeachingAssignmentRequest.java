package vn.edu.fpt.myfschool.common.dto;

import vn.edu.fpt.myfschool.common.enums.AssignmentStatus;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateTeachingAssignmentRequest(
    @NotNull Long classId,
    @NotNull Long subjectId,
    @NotNull Long teacherId,
    @NotNull Long semesterId,
    @NotNull LocalDate effectiveFrom
) {}