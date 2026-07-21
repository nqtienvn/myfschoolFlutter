package vn.edu.fpt.myfschool.common.dto;

import vn.edu.fpt.myfschool.common.enums.AttendanceCorrectionStatus;
import vn.edu.fpt.myfschool.common.enums.Shift;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AttendanceCorrectionRequestDto(
    Long id, Long classId, String className, Long teacherId, String teacherName,
    LocalDate date, Shift shift, AttendanceCorrectionStatus status,
    int originalPresentCount, int originalAbsentWithLeaveCount,
    int originalAbsentWithoutLeaveCount,
    int presentCount, int absentWithLeaveCount, int absentWithoutLeaveCount,
    String reason, List<AttendanceCorrectionEntryDto> changes,
    LocalDateTime createdAt, String reviewedByName, LocalDateTime reviewedAt
) {}
