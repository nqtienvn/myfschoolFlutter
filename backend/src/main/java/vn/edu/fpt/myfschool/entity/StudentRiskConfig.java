package vn.edu.fpt.myfschool.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import vn.edu.fpt.myfschool.common.enums.RiskSeverity;

import java.math.BigDecimal;

@Entity
@Table(name = "student_risk_configs", uniqueConstraints = @UniqueConstraint(columnNames = "academic_year_id"))
@Data
@EqualsAndHashCode(callSuper = true)
public class StudentRiskConfig extends BaseEntity {
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @Column(name = "min_gpa", precision = 4, scale = 2)
    private BigDecimal minGpa;

    @Column(name = "min_attendance_rate", precision = 5, scale = 2)
    private BigDecimal minAttendanceRate;

    @Column(name = "max_unexcused_absences")
    private Integer maxUnexcusedAbsences;

    @Column(name = "conduct_risk_values", length = 200)
    private String conductRiskValues;

    @Column(name = "include_overdue_tuition", nullable = false)
    private Boolean includeOverdueTuition = false;

    @Column(name = "overdue_tuition_days", nullable = false)
    private Integer overdueTuitionDays = 0;

    @Enumerated(EnumType.STRING) @Column(name = "gpa_severity", nullable = false)
    private RiskSeverity gpaSeverity = RiskSeverity.HIGH;
    @Enumerated(EnumType.STRING) @Column(name = "attendance_severity", nullable = false)
    private RiskSeverity attendanceSeverity = RiskSeverity.HIGH;
    @Enumerated(EnumType.STRING) @Column(name = "absence_severity", nullable = false)
    private RiskSeverity absenceSeverity = RiskSeverity.HIGH;
    @Enumerated(EnumType.STRING) @Column(name = "conduct_severity", nullable = false)
    private RiskSeverity conductSeverity = RiskSeverity.MEDIUM;
    @Enumerated(EnumType.STRING) @Column(name = "tuition_severity", nullable = false)
    private RiskSeverity tuitionSeverity = RiskSeverity.MEDIUM;
}
