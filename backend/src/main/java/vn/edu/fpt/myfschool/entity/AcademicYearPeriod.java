package vn.edu.fpt.myfschool.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "academic_year_periods", uniqueConstraints = @UniqueConstraint(columnNames = {"academic_year_id", "period_id"}))
@Data
@EqualsAndHashCode(callSuper = true)
public class AcademicYearPeriod extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "period_id", nullable = false)
    private Period period;
}
