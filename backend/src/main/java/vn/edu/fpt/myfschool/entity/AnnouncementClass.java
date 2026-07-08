package vn.edu.fpt.myfschool.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
