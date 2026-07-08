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

@Entity
@Table(name = "student_scores",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"grade_item_id", "student_id"}))
@Data
@EqualsAndHashCode(callSuper = true)
public class StudentScore extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grade_item_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_ss_gradeitem"))
    private GradeItem gradeItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_ss_student"))
    private Student student;

    @Column(precision = 5, scale = 2)
    private BigDecimal score;

    @Column(nullable = false)
    private Boolean isGraded = false;

    private String note;

    @Column(nullable = false)
    private Boolean isCommentBased = false;

    private String comment;
}