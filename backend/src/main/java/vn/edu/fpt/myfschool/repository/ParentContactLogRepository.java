package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.myfschool.entity.ParentContactLog;

import java.util.List;

public interface ParentContactLogRepository extends JpaRepository<ParentContactLog, Long> {
    List<ParentContactLog> findByStudentIdAndAcademicYearIdAndSemesterIdOrderByContactedAtDesc(
            Long studentId, Long academicYearId, Long semesterId);
    List<ParentContactLog> findByClsIdAndSemesterId(Long classId, Long semesterId);
}
