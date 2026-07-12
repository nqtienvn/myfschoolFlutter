package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import vn.edu.fpt.myfschool.common.enums.Shift;

import java.time.LocalDate;

public record AdminAttendanceAdjustmentRequest(
    @NotNull Long academicYearId,
    @NotNull Long classId,
    @NotNull LocalDate date,
    @NotNull Shift shift,
    @Min(0) int presentCount,
    @Min(0) int absentWithLeaveCount,
    @Min(0) int absentWithoutLeaveCount
) {}
