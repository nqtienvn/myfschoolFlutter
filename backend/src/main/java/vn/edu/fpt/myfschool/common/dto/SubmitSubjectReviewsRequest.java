package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SubmitSubjectReviewsRequest(
    @NotNull Long academicYearId,
    @NotNull Long semesterId,
    @NotNull Long classId,
    @NotNull Long subjectId,
    @NotEmpty List<Long> studentIds
) {}
