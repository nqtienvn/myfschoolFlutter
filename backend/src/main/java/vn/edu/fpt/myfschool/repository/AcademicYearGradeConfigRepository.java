package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.myfschool.entity.AcademicYearGradeConfig;
import java.util.Optional;

public interface AcademicYearGradeConfigRepository extends JpaRepository<AcademicYearGradeConfig,Long> {
    Optional<AcademicYearGradeConfig> findByAcademicYearId(Long academicYearId);
    boolean existsByAcademicYearId(Long academicYearId);
}
