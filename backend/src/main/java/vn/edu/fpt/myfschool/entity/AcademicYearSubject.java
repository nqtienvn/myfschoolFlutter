package vn.edu.fpt.myfschool.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "academic_year_subjects", uniqueConstraints = @UniqueConstraint(columnNames = {"academic_year_id", "subject_id"}))
@Data
@EqualsAndHashCode(callSuper = true)
public class AcademicYearSubject extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;
}
