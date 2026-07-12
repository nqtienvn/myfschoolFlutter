package vn.edu.fpt.myfschool.common.dto;

import java.math.BigDecimal;

public record DashboardStudentStatsDto(
    Long studentId, String studentName, String studentCode, String className,
    double attendanceRate, int presentSessions, int absentSessions,
    BigDecimal currentGpa, String academicAbility, String conduct, Integer classRank
) {}
