package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import vn.edu.fpt.myfschool.common.enums.Shift;

import java.time.LocalDate;
import java.util.List;

public record CreateAttendanceCorrectionRequest(
    @NotNull Long classId,
    @NotNull LocalDate date,
    @NotNull Shift shift,
    @NotNull List<AttendanceEntry> entries,
    @NotBlank @Size(max = 500) String reason
) {}
