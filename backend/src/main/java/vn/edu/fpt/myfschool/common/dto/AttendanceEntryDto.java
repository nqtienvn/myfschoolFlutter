package vn.edu.fpt.myfschool.common.dto;

import vn.edu.fpt.myfschool.common.enums.AttendanceStatus;

public record AttendanceEntryDto(
    Long studentId, String studentName, String studentCode,
    AttendanceStatus status, Long attendanceId
) {}
