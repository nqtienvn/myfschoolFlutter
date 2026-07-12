package vn.edu.fpt.myfschool.common.dto;

import vn.edu.fpt.myfschool.common.enums.Shift;

import java.time.LocalDate;

public record AdminAttendanceDayDto(
    Long classId,
    String className,
    LocalDate date,
    Shift shift,
    int scheduledPeriods,
    int totalStudents,
    boolean submitted,
    int presentCount,
    int absentWithLeaveCount,
    int absentWithoutLeaveCount
) {}
