package vn.edu.fpt.myfschool.controller.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "class_subjects",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"class_id", "subject_id", "academic_year"}))
@Data
@EqualsAndHashCode(callSuper = true)
public class ClassSubject extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_cs_class"))
    private SchoolClass cls;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_cs_subject"))
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_cs_teacher"))
    private Teacher teacher;

    @Column(nullable = false)
    private Boolean isHomeroom = false;

    @Column(nullable = false, length = 9)
    private String academicYear;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id",
                foreignKey = @ForeignKey(name = "fk_cs_semester"))
    private Semester semester;
}
