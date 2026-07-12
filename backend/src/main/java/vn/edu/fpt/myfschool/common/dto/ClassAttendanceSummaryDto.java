package vn.edu.fpt.myfschool.common.dto;

public record ClassAttendanceSummaryDto(
    Long studentId,
    String studentCode,
    String studentName,
    int presentCount,
    int absentCount,
    int absentWithLeaveCount,
    int absentWithoutLeaveCount,
    double attendanceRate,
    String suggestedConduct
) {}
