package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.myfschool.common.enums.EnrollmentStatus;
import vn.edu.fpt.myfschool.controller.entity.Enrollment;
import vn.edu.fpt.myfschool.controller.entity.Student;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    List<Enrollment> findByStudentId(Long studentId);
    Optional<Enrollment> findByStudentIdAndAcademicYearIdAndStatus(Long studentId, Long academicYearId, EnrollmentStatus status);
    List<Enrollment> findByClsIdAndAcademicYearIdAndStatus(Long classId, Long academicYearId, EnrollmentStatus status);

    @Query("SELECT e.student FROM Enrollment e WHERE e.cls.id = :classId AND e.academicYear.id = :academicYearId AND e.status = 'ACTIVE'")
    List<Student> findActiveStudentsByClassAndYear(@Param("classId") Long classId, @Param("academicYearId") Long academicYearId);
}
