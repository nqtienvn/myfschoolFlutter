package vn.edu.fpt.myfschool.controller.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "students")
@Data
@EqualsAndHashCode(callSuper = true)
public class Student extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true,
                foreignKey = @ForeignKey(name = "fk_students_user"))
    private User user;

    @Column(nullable = false, unique = true, length = 20)
    private String studentCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id",
                foreignKey = @ForeignKey(name = "fk_students_class"))
    private SchoolClass currentClass;

    @Column(columnDefinition = "DATE")
    private LocalDate dateOfBirth;

    @OneToMany(mappedBy = "student", fetch = FetchType.LAZY)
    private List<StudentGuardian> studentGuardians = new ArrayList<>();

    @OneToMany(mappedBy = "student", fetch = FetchType.LAZY)
    private List<StudentClass> studentClasses = new ArrayList<>();
}
