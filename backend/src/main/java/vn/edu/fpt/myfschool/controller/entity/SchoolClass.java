package vn.edu.fpt.myfschool.controller.entity;

import jakarta.persistence.*;
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

    @Column(nullable = false, length = 200)
    private String schoolName = "FPT Schools";

    @OneToMany(mappedBy = "currentClass", fetch = FetchType.LAZY)
    private List<Student> students = new ArrayList<>();

    @OneToMany(mappedBy = "cls", fetch = FetchType.LAZY)
    private List<ClassSubject> classSubjects = new ArrayList<>();
}
