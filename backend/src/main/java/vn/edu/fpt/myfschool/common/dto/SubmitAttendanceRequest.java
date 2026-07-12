package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import vn.edu.fpt.myfschool.common.enums.Shift;

public record SubmitAttendanceRequest(
    @NotNull Long classId, @NotNull LocalDate date, @NotNull Shift shift,
    @NotNull List<AttendanceEntry> entries
) {}
