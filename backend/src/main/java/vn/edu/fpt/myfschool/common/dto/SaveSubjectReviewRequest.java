package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SaveSubjectReviewRequest(
    @NotNull Long academicYearId,
    @NotNull Long semesterId,
    @NotNull Long classId,
    @NotNull Long subjectId,
    @Size(max = 2000) String comment,
    @Size(max = 1000) String strengths,
    @Size(max = 1000) String improvements
) {}
