package vn.edu.fpt.myfschool.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import vn.edu.fpt.myfschool.common.enums.Gender;

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

    // ponytail: denormalized shortcut kept for old dashboard paths; remove after all class membership queries use Enrollment.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id",
                foreignKey = @ForeignKey(name = "fk_students_class"))
    private SchoolClass currentClass;

    @Column(columnDefinition = "DATE")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    @Column(length = 500)
    private String address;

    @OneToMany(mappedBy = "student", fetch = FetchType.LAZY)
    private List<StudentGuardian> studentGuardians = new ArrayList<>();

    @OneToMany(mappedBy = "student", fetch = FetchType.LAZY)
    private List<Enrollment> enrollments = new ArrayList<>();
}
