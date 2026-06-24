package vn.edu.fpt.myfschool.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Entity
@Table(name = "semester_results",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"student_id", "semester_id"}))
@Data
@EqualsAndHashCode(callSuper = true)
public class SemesterResult extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_sr_student"))
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_sr_semester"))
    private Semester semester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_sr_class"))
    private SchoolClass cls;

    @Column(precision = 3, scale = 2)
    private BigDecimal gpa;

    private Integer rank;

    @Column(length = 50)
    private String honor;

    @Column(length = 50)
    private String conduct;

    @Column(name = "academic_ability", length = 50)
    private String academicAbility;
}
