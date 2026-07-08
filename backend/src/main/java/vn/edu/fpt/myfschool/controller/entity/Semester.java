package vn.edu.fpt.myfschool.controller.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import java.time.LocalDate;

@Entity
@Table(name = "semesters",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"name", "academic_year_id"}),
           @UniqueConstraint(columnNames = {"academic_year_id", "semester_order"})
       })
@SQLDelete(sql = "UPDATE semesters SET deleted = true WHERE id = ?")
@Where(clause = "deleted = false")
@Data
@EqualsAndHashCode(callSuper = true)
public class Semester extends BaseEntity {

    @Column(nullable = false)
    private Boolean deleted = false;

    @Column(nullable = false, length = 50)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_semesters_academic_year"))
    private AcademicYear academicYear;

    @Column(name = "semester_order", nullable = false)
    private Integer order;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private Boolean isCurrent = false;
}
