package vn.edu.fpt.myfschool.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "semester_results",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"student_id", "semester_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SemesterResult extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_sr_student"))
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_sr_semester"))
    private Semester semester; //kì học

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_sr_class"))
    private SchoolClass cls;

    @Column(precision = 5, scale = 2)
    private BigDecimal gpa;

    @Column(name = "class_rank")
    private Integer rank;

    @Column(length = 50)
    private String honor;

    @Column(length = 50)
    private String conduct;

    @Column(name = "academic_ability", length = 50)
    private String academicAbility;
}
