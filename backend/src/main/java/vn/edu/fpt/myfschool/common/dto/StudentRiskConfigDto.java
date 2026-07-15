package vn.edu.fpt.myfschool.common.dto;

import vn.edu.fpt.myfschool.common.enums.RiskSeverity;

import java.math.BigDecimal;

public record StudentRiskConfigDto(
        Long id,
        Long academicYearId,
        BigDecimal minGpa,
        BigDecimal minAttendanceRate,
        Integer maxUnexcusedAbsences,
        String conductRiskValues,
        Boolean includeOverdueTuition,
        Integer overdueTuitionDays,
        RiskSeverity gpaSeverity,
        RiskSeverity attendanceSeverity,
        RiskSeverity absenceSeverity,
        RiskSeverity conductSeverity,
        RiskSeverity tuitionSeverity
) {}
