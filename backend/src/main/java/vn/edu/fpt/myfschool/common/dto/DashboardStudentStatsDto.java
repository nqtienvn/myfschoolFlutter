package vn.edu.fpt.myfschool.common.dto;

import java.math.BigDecimal;

public record DashboardStudentStatsDto(
    Long studentId, String studentName, String studentCode,
    Long classId, String className, String schoolName,
    Long academicYearId, String academicYearName,
    Long semesterId, String semesterName,
    double attendanceRate, int presentSessions, int absentSessions,
    BigDecimal currentGpa, String academicAbility, String conduct, Integer classRank,
    String homeroomTeacherName, String homeroomTeacherPhone
) {}
