package vn.edu.fpt.myfschool.controller.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import vn.edu.fpt.myfschool.common.enums.Shift;
import java.time.LocalDate;

@Entity
@Table(name = "attendance_sessions",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"class_id", "date", "shift", "schedule_id"}))
@Data
@EqualsAndHashCode(callSuper = true)
public class AttendanceSession extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_as_class"))
    private SchoolClass cls;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_as_teacher"))
    private Teacher teacher;

    @Column(nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Shift shift;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id",
                foreignKey = @ForeignKey(name = "fk_as_schedule"))
    private Schedule schedule;

    @Column(nullable = false)
    private Integer total = 0;

    @Column(nullable = false)
    private Integer present = 0;

    @Column(nullable = false)
    private Integer late = 0;

    @Column(nullable = false)
    private Integer absent = 0;

    @Column(nullable = false)
    private Boolean isClosed = false;
}