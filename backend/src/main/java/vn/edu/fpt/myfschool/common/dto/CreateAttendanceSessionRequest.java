package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.NotNull;
import vn.edu.fpt.myfschool.common.enums.Shift;
import java.time.LocalDate;

public record CreateAttendanceSessionRequest(
    @NotNull Long classId,
    @NotNull Long teacherId,
    @NotNull LocalDate date,
    @NotNull Shift shift,
    Long scheduleId
) {}