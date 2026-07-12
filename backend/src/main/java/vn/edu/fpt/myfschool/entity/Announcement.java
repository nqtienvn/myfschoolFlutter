package vn.edu.fpt.myfschool.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import vn.edu.fpt.myfschool.common.enums.TargetRole;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "announcements")
@Data
@EqualsAndHashCode(callSuper = true)
public class Announcement extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id",
                foreignKey = @ForeignKey(name = "fk_ann_teacher"))
    private Teacher teacher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_user_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @Column(name = "approval_status", nullable = false, length = 20)
    private String approvalStatus = "PENDING";

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    @Column(name = "sender_type", nullable = false, length = 20)
    private String senderType = "SUBJECT_TEACHER";

    @Column(name = "recipient_scope", nullable = false, length = 20)
    private String recipientScope = "CLASSES";

    @Column(name = "teacher_audience", length = 20)
    private String teacherAudience;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_subject_id")
    private Subject recipientSubject;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TargetRole targetRole;

    @Column(nullable = false)
    private Boolean requiresReply = false;

    @OneToMany(mappedBy = "announcement", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AnnouncementClass> announcementClasses = new ArrayList<>();

    @OneToMany(mappedBy = "announcement", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AnnouncementRead> reads = new ArrayList<>();
}
