package vn.edu.fpt.myfschool.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import vn.edu.fpt.myfschool.common.enums.StudentEventStatus;
import vn.edu.fpt.myfschool.common.enums.StudentEventType;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "student_events")
@Data
@EqualsAndHashCode(callSuper = true)
public class StudentEvent extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "student_id", nullable = false)
    private Student student;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "class_id", nullable = false)
    private SchoolClass cls;
    @Enumerated(EnumType.STRING) @Column(name = "event_type", nullable = false, length = 20)
    private StudentEventType eventType;
    @Column(length = 100)
    private String category;
    @Column(nullable = false, length = 200)
    private String title;
    @Column(length = 2000)
    private String description;
    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private StudentEventStatus status = StudentEventStatus.DRAFT;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;
    @Column(name = "published_at")
    private LocalDateTime publishedAt;
}
