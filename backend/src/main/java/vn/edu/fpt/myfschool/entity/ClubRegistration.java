package vn.edu.fpt.myfschool.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
import vn.edu.fpt.myfschool.common.enums.ClubStatus;

@Entity
@Table(name = "club_registrations",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"student_id", "club_name", "academic_year"}))
@Data
@EqualsAndHashCode(callSuper = true)
public class ClubRegistration extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_cr_student"))
    private Student student;

    @Column(nullable = false, length = 200)
    private String clubName;

    @Column(nullable = false, length = 9)
    private String academicYear;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClubStatus status = ClubStatus.REGISTERED;

    private LocalDateTime registeredAt;
}
