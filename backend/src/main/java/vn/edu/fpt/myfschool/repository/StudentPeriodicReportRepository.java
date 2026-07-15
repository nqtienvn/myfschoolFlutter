package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.myfschool.common.enums.PeriodicReportStatus;
import vn.edu.fpt.myfschool.entity.StudentPeriodicReport;

import java.util.List;
import java.util.Optional;

public interface StudentPeriodicReportRepository extends JpaRepository<StudentPeriodicReport, Long> {
    Optional<StudentPeriodicReport> findByStudentIdAndSemesterId(Long studentId, Long semesterId);
    Optional<StudentPeriodicReport> findByStudentIdAndSemesterIdAndStatus(
            Long studentId, Long semesterId, PeriodicReportStatus status);
    List<StudentPeriodicReport> findByAcademicYearIdAndSemesterId(Long academicYearId, Long semesterId);
}
