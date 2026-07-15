package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.*;
import vn.edu.fpt.myfschool.common.enums.RiskSeverity;

import java.math.BigDecimal;

public record UpdateStudentRiskConfigRequest(
        @NotNull Long academicYearId,
        @DecimalMin("0.00") @DecimalMax("10.00") BigDecimal minGpa,
        @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal minAttendanceRate,
        @Min(0) Integer maxUnexcusedAbsences,
        @Size(max = 200) String conductRiskValues,
        @NotNull Boolean includeOverdueTuition,
        @NotNull @Min(0) Integer overdueTuitionDays,
        @NotNull RiskSeverity gpaSeverity,
        @NotNull RiskSeverity attendanceSeverity,
        @NotNull RiskSeverity absenceSeverity,
        @NotNull RiskSeverity conductSeverity,
        @NotNull RiskSeverity tuitionSeverity
) {}
