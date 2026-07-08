package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CalculateSemesterResultRequest(
    @NotNull Long classId,
    @NotNull Long semesterId
) {}