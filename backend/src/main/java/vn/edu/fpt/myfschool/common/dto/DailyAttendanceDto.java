package vn.edu.fpt.myfschool.common.dto;

import vn.edu.fpt.myfschool.common.enums.Shift;
import java.time.LocalDate;
import java.util.List;

public record DailyAttendanceDto(
    Long classId, String className, LocalDate date, Shift shift,
    boolean hasSchedule, boolean submitted, boolean canEdit, boolean correctionPending,
    int scheduledPeriods,
    List<AttendanceEntryDto> students
) {}
