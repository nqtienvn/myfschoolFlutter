package vn.edu.fpt.myfschool.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import vn.edu.fpt.myfschool.common.enums.Relationship;

@Entity
@Table(name = "student_guardians",
       uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "guardian_id"}))
@Data
@EqualsAndHashCode(callSuper = true)
public class StudentGuardian extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_sg_student"))
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guardian_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_sg_guardian"))
    private Parent guardian;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Relationship relationship;
}
