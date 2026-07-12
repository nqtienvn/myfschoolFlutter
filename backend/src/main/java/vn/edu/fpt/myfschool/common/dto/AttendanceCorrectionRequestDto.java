package vn.edu.fpt.myfschool.common.dto;

import vn.edu.fpt.myfschool.common.enums.AttendanceCorrectionStatus;
import vn.edu.fpt.myfschool.common.enums.Shift;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AttendanceCorrectionRequestDto(
    Long id, Long classId, String className, String teacherName,
    LocalDate date, Shift shift, AttendanceCorrectionStatus status,
    int presentCount, int absentWithLeaveCount, int absentWithoutLeaveCount,
    LocalDateTime createdAt
) {}
