package vn.edu.fpt.myfschool.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.edu.fpt.myfschool.common.enums.SubjectReviewStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "subject_student_reviews", uniqueConstraints = @UniqueConstraint(
        name = "uq_ssr_student_subject_semester", columnNames = {"student_id", "subject_id", "semester_id"}))
@Getter
@Setter
@NoArgsConstructor
public class SubjectStudentReview extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "class_id", nullable = false)
    private SchoolClass cls;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "student_id", nullable = false)
    private Student student;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "subject_teacher_id", nullable = false)
    private Teacher subjectTeacher;
    @Column(length = 2000) private String comment;
    @Column(length = 1000) private String strengths;
    @Column(length = 1000) private String improvements;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private SubjectReviewStatus status = SubjectReviewStatus.DRAFT;
    @Column(name = "return_reason", length = 500) private String returnReason;
    @Column(name = "submitted_at") private LocalDateTime submittedAt;
}
