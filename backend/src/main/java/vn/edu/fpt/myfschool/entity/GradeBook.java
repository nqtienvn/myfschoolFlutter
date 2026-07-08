package vn.edu.fpt.myfschool.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "grade_books",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"class_id", "subject_id", "semester_id"}))
@Data
@EqualsAndHashCode(callSuper = true)
public class GradeBook extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_gb_class"))
    private SchoolClass cls;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_gb_subject"))
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_gb_semester"))
    private Semester semester;

    @Column(nullable = false)
    private Boolean isFinalized = false;

    @OneToMany(mappedBy = "gradeBook", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<GradeItem> items = new ArrayList<>();
}