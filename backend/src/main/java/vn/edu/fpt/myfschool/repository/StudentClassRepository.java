package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.myfschool.entity.StudentClass;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentClassRepository extends JpaRepository<StudentClass, Long> {
    List<StudentClass> findByStudentId(Long studentId);
    Optional<StudentClass> findByStudentIdAndAcademicYear(Long studentId, String academicYear);

    @Query("SELECT sc FROM StudentClass sc WHERE sc.cls.id = :classId AND sc.academicYear = :academicYear")
    List<StudentClass> findByClsIdAndAcademicYear(@Param("classId") Long classId, @Param("academicYear") String academicYear);
}
