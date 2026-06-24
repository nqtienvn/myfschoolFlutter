package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.myfschool.entity.StudentClass;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentClassRepository extends JpaRepository<StudentClass, Long> {

    List<StudentClass> findByStudentId(Long studentId);

    Optional<StudentClass> findByStudentIdAndAcademicYear(Long studentId, String academicYear);

    List<StudentClass> findByClsIdAndAcademicYear(Long classId, String academicYear);
}
