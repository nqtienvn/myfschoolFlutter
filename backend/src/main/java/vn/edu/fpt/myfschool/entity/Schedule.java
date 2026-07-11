package vn.edu.fpt.myfschool.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import vn.edu.fpt.myfschool.common.enums.Shift;

@Entity
@Table(name = "schedules",
       uniqueConstraints = @UniqueConstraint(name = "uq_schedule_timetable_slot",
           columnNames = {"timetable_id", "day_of_week", "period_id"}))
@Data
@EqualsAndHashCode(callSuper = true)
public class Schedule extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "timetable_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_schedule_timetable"))
    private Timetable timetable;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_sch_assignment"))
    private TeachingAssignment assignment;

    @Column(name = "day_of_week", nullable = false)
    private Integer dayOfWeek;

    @Column(nullable = false)
    private Integer period;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "period_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_sch_period"))
    private Period periodRef;

    @Column(length = 20)
    private String room;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Shift shift = Shift.MORNING;
}
