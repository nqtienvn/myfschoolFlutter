package vn.edu.fpt.myfschool.common.dto;

import java.math.BigDecimal;

public record DashboardStudentStatsDto(
    Long studentId, String studentName, String studentCode, String className,
    double attendanceRate, int presentDays, int absentDays, int lateDays,
    BigDecimal currentGpa, String academicAbility, String conduct, Integer classRank
) {}
