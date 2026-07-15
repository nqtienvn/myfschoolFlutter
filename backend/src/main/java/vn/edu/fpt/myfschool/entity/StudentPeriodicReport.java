package vn.edu.fpt.myfschool.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.edu.fpt.myfschool.common.enums.PeriodicReportStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "student_periodic_reports", uniqueConstraints = @UniqueConstraint(
        name = "uq_spr_student_semester", columnNames = {"student_id", "semester_id"}))
@Getter
@Setter
@NoArgsConstructor
public class StudentPeriodicReport extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "class_id", nullable = false)
    private SchoolClass cls;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "student_id", nullable = false)
    private Student student;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "homeroom_teacher_id", nullable = false)
    private Teacher homeroomTeacher;
    @Column(name = "general_comment", length = 2000) private String generalComment;
    @Column(length = 50) private String conduct;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private PeriodicReportStatus status = PeriodicReportStatus.DRAFT;
    @Column(name = "published_at") private LocalDateTime publishedAt;
}
