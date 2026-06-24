package vn.edu.fpt.myfschool.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.ArrayList;
import java.util.List;
import vn.edu.fpt.myfschool.common.enums.TargetRole;

@Entity
@Table(name = "announcements")
@Data
@EqualsAndHashCode(callSuper = true)
public class Announcement extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_ann_teacher"))
    private Teacher teacher;

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
