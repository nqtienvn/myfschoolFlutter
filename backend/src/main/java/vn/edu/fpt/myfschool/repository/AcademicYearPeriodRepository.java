package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.myfschool.entity.AcademicYearPeriod;
import java.util.List;

public interface AcademicYearPeriodRepository extends JpaRepository<AcademicYearPeriod, Long> {
    List<AcademicYearPeriod> findByAcademicYearId(Long academicYearId);
}
