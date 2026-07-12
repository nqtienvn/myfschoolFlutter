package vn.edu.fpt.myfschool.common.dto;

public record AttendanceStatsDto(
    Long studentId, String studentName, Long semesterId, String semesterName,
    int totalSessions, int presentSessions, int absentWithLeave, int absentWithoutLeave,
    double attendanceRate
) {}
