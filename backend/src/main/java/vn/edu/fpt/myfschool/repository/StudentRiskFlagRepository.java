package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.myfschool.common.enums.RiskStatus;
import vn.edu.fpt.myfschool.common.enums.RiskType;
import vn.edu.fpt.myfschool.entity.StudentRiskFlag;

import java.util.List;
import java.util.Optional;

public interface StudentRiskFlagRepository extends JpaRepository<StudentRiskFlag, Long> {
    Optional<StudentRiskFlag> findByAcademicYearIdAndSemesterIdAndClsIdAndStudentIdAndRiskType(
            Long academicYearId, Long semesterId, Long classId, Long studentId, RiskType riskType);
    List<StudentRiskFlag> findByAcademicYearIdAndSemesterIdAndClsIdOrderBySeverityDescDetectedAtDesc(
            Long academicYearId, Long semesterId, Long classId);
    List<StudentRiskFlag> findByAcademicYearIdAndSemesterIdAndClsIdAndStatusOrderBySeverityDescDetectedAtDesc(
            Long academicYearId, Long semesterId, Long classId, RiskStatus status);
}
