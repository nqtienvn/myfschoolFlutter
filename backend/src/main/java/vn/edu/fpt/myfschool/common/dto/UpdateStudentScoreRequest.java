package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record UpdateStudentScoreRequest(
    @NotNull Long gradeItemId,
    @NotNull List<UpdateScoreEntry> entries,
    String reason
) {}
