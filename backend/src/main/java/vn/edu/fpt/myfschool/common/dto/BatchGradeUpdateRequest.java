package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record BatchGradeUpdateRequest(
    @NotNull Long subjectId, @NotNull Long semesterId,
    List<GradeEntry> grades
) {}
