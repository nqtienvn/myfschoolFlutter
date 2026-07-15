package vn.edu.fpt.myfschool.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;
import vn.edu.fpt.myfschool.common.enums.UserRole;

import java.time.LocalDateTime;

@Entity
@Table(name = "announcement_reads",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"announcement_id", "user_id"}))
@Data
@EqualsAndHashCode(callSuper = true)
public class AnnouncementRead extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "announcement_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_ar_announcement"))
    private Announcement announcement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_ar_user"))
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_role", length = 20)
    private UserRole recipientRole;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "student_names", columnDefinition = "TEXT")
    private String studentNames;

    @Column(name = "class_names", columnDefinition = "TEXT")
    private String classNames;

    @Column(name = "class_ids", columnDefinition = "TEXT")
    private String classIds;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "reply_text", length = 1000)
    private String replyText;

    @Column(name = "replied_at")
    private LocalDateTime repliedAt;
}
