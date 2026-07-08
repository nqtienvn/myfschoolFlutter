package vn.edu.fpt.myfschool.controller.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "announcement_classes",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"announcement_id", "class_id"}))
@Data
@EqualsAndHashCode(callSuper = true)
public class AnnouncementClass extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "announcement_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_ac_announcement"))
    private Announcement announcement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_ac_class"))
    private SchoolClass cls;
}
