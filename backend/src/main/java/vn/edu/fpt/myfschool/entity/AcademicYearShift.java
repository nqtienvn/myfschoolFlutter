package vn.edu.fpt.myfschool.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "academic_year_shifts", uniqueConstraints = @UniqueConstraint(columnNames = {"academic_year_id", "shift_id"}))
@Data
@EqualsAndHashCode(callSuper = true)
public class AcademicYearShift extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "shift_id", nullable = false)
    private SchoolShift shift;
}
