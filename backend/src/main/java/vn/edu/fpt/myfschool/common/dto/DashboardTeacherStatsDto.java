package vn.edu.fpt.myfschool.common.dto;

import java.math.BigDecimal;

public record DashboardTeacherStatsDto(
    Long classId,
    String className,
    Long academicYearId,
    String academicYearName,
    Long semesterId,
    String semesterName,
    Double attendanceRate,
    BigDecimal averageGpa,
    Double parentReadRate
) {}
