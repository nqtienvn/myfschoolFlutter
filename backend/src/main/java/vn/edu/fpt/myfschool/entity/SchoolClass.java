package vn.edu.fpt.myfschool.entity;

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
@Table(name = "classes",
       uniqueConstraints = @UniqueConstraint(columnNames = {"name", "academic_year_id"}))
@Data
@EqualsAndHashCode(callSuper = true)
public class SchoolClass extends BaseEntity {

    @Column(nullable = false, length = 20)
    private String name;

    @Column(nullable = false)
    private Integer gradeLevel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_classes_academic_year"))
    private AcademicYear academicYear;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id",
                foreignKey = @ForeignKey(name = "fk_classes_school"))
    private School school;

    @Column(nullable = false, length = 200)
    private String schoolName = "FPT Schools";

    @OneToMany(mappedBy = "currentClass", fetch = FetchType.LAZY)
    private List<Student> students = new ArrayList<>();
}
