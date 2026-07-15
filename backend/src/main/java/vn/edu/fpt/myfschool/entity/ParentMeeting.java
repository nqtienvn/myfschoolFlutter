package vn.edu.fpt.myfschool.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import vn.edu.fpt.myfschool.common.enums.ParentMeetingStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "parent_meetings")
@Data
@EqualsAndHashCode(callSuper = true)
public class ParentMeeting extends BaseEntity {
    @Column(nullable = false, length = 200)
    private String title;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "class_id", nullable = false)
    private SchoolClass cls;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "student_id")
    private Student student;
    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;
    @Column(length = 300)
    private String location;
    @Column(length = 2000)
    private String agenda;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private ParentMeetingStatus status = ParentMeetingStatus.SCHEDULED;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;
    @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ParentMeetingParticipant> participants = new ArrayList<>();
}
