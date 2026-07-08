package vn.edu.fpt.myfschool.controller.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "student_classes",
       uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "academic_year"}))
@Data
@EqualsAndHashCode(callSuper = true)
public class StudentClass extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_sc_student"))
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_sc_class"))
    private SchoolClass cls;

    @Column(nullable = false, length = 9)
    private String academicYear;
}
