package vn.edu.fpt.myfschool.common.dto;

import vn.edu.fpt.myfschool.common.enums.AttendanceStatus;

public record AttendanceCorrectionEntryDto(
    Long studentId,
    String studentName,
    String studentCode,
    AttendanceStatus oldStatus,
    AttendanceStatus newStatus
) {}
