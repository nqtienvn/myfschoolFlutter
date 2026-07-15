package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.myfschool.entity.StudentRiskConfig;

import java.util.Optional;

public interface StudentRiskConfigRepository extends JpaRepository<StudentRiskConfig, Long> {
    Optional<StudentRiskConfig> findByAcademicYearId(Long academicYearId);
}
