package vn.edu.fpt.myfschool.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Deprecated
@Entity
@Table(name = "grades",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"student_id", "subject_id", "semester_id"}))
@Data
@EqualsAndHashCode(callSuper = true)
public class Grade extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_grades_student"))
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_grades_subject"))
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_grades_semester"))
    private Semester semester;

    @Column(precision = 3, scale = 2)
    private BigDecimal oral;

    @Column(name = "quiz_15m", precision = 3, scale = 2)
    private BigDecimal quiz15m;

    @Column(name = "mid_term", precision = 3, scale = 2)
    private BigDecimal midTerm;

    @Column(precision = 3, scale = 2)
    private BigDecimal finalScore;

    @Column(precision = 3, scale = 2)
    private BigDecimal average;
}
