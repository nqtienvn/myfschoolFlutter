package vn.edu.fpt.myfschool.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.edu.fpt.myfschool.common.enums.SemesterStatus;

import java.time.LocalDate;

@Entity
@Table(name = "semesters",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"name", "academic_year_id"}),
           @UniqueConstraint(columnNames = {"academic_year_id", "semester_order"})
       })
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Semester extends BaseEntity {

    @Column(nullable = false)
    private Boolean deleted = false;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "semester_order", nullable = false)
    private Integer order;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private Boolean isCurrent = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SemesterStatus status = SemesterStatus.NOT_STARTED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_semesters_academic_year"))
    private AcademicYear academicYear;

}
