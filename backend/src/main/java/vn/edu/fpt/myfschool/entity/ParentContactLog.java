package vn.edu.fpt.myfschool.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import vn.edu.fpt.myfschool.common.enums.ParentContactType;

import java.time.LocalDateTime;

@Entity
@Table(name = "parent_contact_logs")
@Data
@EqualsAndHashCode(callSuper = true)
public class ParentContactLog extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "student_id", nullable = false)
    private Student student;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "class_id", nullable = false)
    private SchoolClass cls;
    @Enumerated(EnumType.STRING) @Column(name = "contact_type", nullable = false, length = 20)
    private ParentContactType contactType;
    @Column(nullable = false, length = 200)
    private String subject;
    @Column(nullable = false, length = 2000)
    private String summary;
    @Column(length = 1000)
    private String result;
    @Column(name = "contacted_at", nullable = false)
    private LocalDateTime contactedAt;
    @Column(name = "next_action_at")
    private LocalDateTime nextActionAt;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;
}
