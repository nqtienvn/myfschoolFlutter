package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.myfschool.entity.StudentEvent;

import java.util.List;

public interface StudentEventRepository extends JpaRepository<StudentEvent, Long> {
    List<StudentEvent> findByStudentIdAndAcademicYearIdAndSemesterIdOrderByEventDateDesc(
            Long studentId, Long academicYearId, Long semesterId);
    List<StudentEvent> findByClsIdAndSemesterId(Long classId, Long semesterId);
}
