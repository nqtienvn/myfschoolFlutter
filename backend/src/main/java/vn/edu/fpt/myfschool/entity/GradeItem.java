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
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import vn.edu.fpt.myfschool.common.enums.AssessmentType;
import vn.edu.fpt.myfschool.common.enums.GradeEntryRole;

@Entity
@Table(name = "grade_items",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"grade_book_id", "name"}))
@Data
@EqualsAndHashCode(callSuper = true)
public class GradeItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grade_book_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_gi_gradebook"))
    private GradeBook gradeBook;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private Integer weight;

    @Column(nullable = false)
    private Integer maxScore = 10;

    @Column(name = "display_order", nullable = false)
    private Integer order = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "config_item_id")
    private AcademicYearGradeConfigItem configItem;

    @Column(length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private GradeEntryRole entryRole = GradeEntryRole.ADMIN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AssessmentType assessmentType = AssessmentType.SCORE;

    @Column(nullable = false)
    private Boolean requiredEntry = true;
}
