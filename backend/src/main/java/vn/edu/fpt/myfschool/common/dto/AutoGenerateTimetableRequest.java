package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AutoGenerateTimetableRequest(
    @NotNull Long academicYearId,
    @NotNull Long semesterId,
    List<Long> classIds,
    @NotEmpty List<Long> shiftIds,
    @NotEmpty List<Long> periodIds
) {}
