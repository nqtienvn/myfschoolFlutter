package vn.edu.fpt.myfschool.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "classes",
       uniqueConstraints = @UniqueConstraint(columnNames = {"name", "academic_year"}))
@Data
@EqualsAndHashCode(callSuper = true)
public class SchoolClass extends BaseEntity {

    @Column(nullable = false, length = 20)
    private String name;

    @Column(nullable = false)
    private Integer gradeLevel;

    @Column(nullable = false, length = 9)
    private String academicYear;

    @Column(nullable = false, length = 200)
    private String schoolName = "FPT Schools";

    @OneToMany(mappedBy = "currentClass", fetch = FetchType.LAZY)
    private List<Student> students = new ArrayList<>();

    @OneToMany(mappedBy = "cls", fetch = FetchType.LAZY)
    private List<ClassSubject> classSubjects = new ArrayList<>();
}
