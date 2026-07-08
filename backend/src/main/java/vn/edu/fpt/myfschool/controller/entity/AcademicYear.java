package vn.edu.fpt.myfschool.controller.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import vn.edu.fpt.myfschool.common.enums.AcademicYearStatus;

import java.time.LocalDate;

@Entity
@Table(name = "academic_years", uniqueConstraints = @UniqueConstraint(columnNames = "name"))
@Data
@EqualsAndHashCode(callSuper = true)
public class AcademicYear extends BaseEntity {

    @Column(nullable = false, unique = true, length = 20)
    private String name;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AcademicYearStatus status = AcademicYearStatus.DRAFT;
}
