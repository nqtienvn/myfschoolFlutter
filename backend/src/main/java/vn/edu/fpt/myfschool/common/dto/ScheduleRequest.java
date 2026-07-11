package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.*;
public record ScheduleRequest(
    @NotNull Long timetableId,
    @NotNull Long assignmentId,
    @NotNull @Min(1) @Max(7) Integer dayOfWeek,
    @NotNull Long periodId
) {}
