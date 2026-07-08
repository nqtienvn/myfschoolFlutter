package vn.edu.fpt.myfschool.controller.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
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

    private LocalDateTime readAt;
}
