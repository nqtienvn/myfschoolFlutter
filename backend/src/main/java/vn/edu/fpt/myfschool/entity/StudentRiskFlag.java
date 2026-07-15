package vn.edu.fpt.myfschool.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import vn.edu.fpt.myfschool.common.enums.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "student_risk_flags", uniqueConstraints = @UniqueConstraint(
        columnNames = {"academic_year_id", "semester_id", "class_id", "student_id", "risk_type"}))
@Data
@EqualsAndHashCode(callSuper = true)
public class StudentRiskFlag extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "class_id", nullable = false)
    private SchoolClass cls;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "student_id", nullable = false)
    private Student student;
    @Enumerated(EnumType.STRING) @Column(name = "risk_type", nullable = false, length = 40)
    private RiskType riskType;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private RiskSeverity severity;
    @Column(name = "metric_value", length = 100)
    private String metricValue;
    @Column(name = "threshold_value", length = 100)
    private String thresholdValue;
    @Column(nullable = false, length = 500)
    private String message;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private RiskStatus status = RiskStatus.OPEN;
    @Column(name = "detected_at", nullable = false)
    private LocalDateTime detectedAt;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "acknowledged_by")
    private User acknowledgedBy;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "resolved_by")
    private User resolvedBy;
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
    @Column(name = "source_snapshot_json", nullable = false, columnDefinition = "LONGTEXT")
    private String sourceSnapshotJson;
}
