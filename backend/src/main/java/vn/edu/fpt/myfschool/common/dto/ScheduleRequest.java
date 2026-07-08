package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.*;
import vn.edu.fpt.myfschool.common.enums.Shift;

public record ScheduleRequest(
    @NotNull Long assignmentId,
    @NotNull @Min(1) @Max(7) Integer dayOfWeek, @NotNull @Min(1) @Max(10) Integer period,
    String room, @NotNull Shift shift
) {}
