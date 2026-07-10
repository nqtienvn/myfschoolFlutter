package vn.edu.fpt.myfschool.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "teachers")
@Data
@EqualsAndHashCode(callSuper = true)
public class Teacher extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true,
                foreignKey = @ForeignKey(name = "fk_teachers_user"))
    private User user;

    @Column(nullable = false, unique = true, length = 20)
    private String employeeCode;

    @ManyToMany
    @JoinTable(
            name = "teacher_subjects",
            joinColumns = @JoinColumn(name = "teacher_id", foreignKey = @ForeignKey(name = "fk_teacher_subjects_teacher")),
            inverseJoinColumns = @JoinColumn(name = "subject_id", foreignKey = @ForeignKey(name = "fk_teacher_subjects_subject")),
            uniqueConstraints = @UniqueConstraint(columnNames = {"teacher_id", "subject_id"}))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Subject> subjects = new HashSet<>();
}
