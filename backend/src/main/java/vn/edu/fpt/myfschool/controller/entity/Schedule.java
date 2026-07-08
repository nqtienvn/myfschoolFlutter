package vn.edu.fpt.myfschool.controller.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import vn.edu.fpt.myfschool.common.enums.Shift;

@Entity
@Table(name = "schedules",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"assignment_id", "day_of_week", "period"}))
@Data
@EqualsAndHashCode(callSuper = true)
public class Schedule extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_sch_assignment"))
    private TeachingAssignment assignment;

    @Column(name = "day_of_week", nullable = false)
    private Integer dayOfWeek;

    @Column(nullable = false)
    private Integer period;

    @Column(length = 20)
    private String room;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Shift shift = Shift.MORNING;
}
