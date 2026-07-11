package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateTimetableRequest(
    @NotNull Long classId,
    @NotNull Long semesterId,
    @NotNull LocalDate effectiveFrom,
    Long copyFromTimetableId
) {}
