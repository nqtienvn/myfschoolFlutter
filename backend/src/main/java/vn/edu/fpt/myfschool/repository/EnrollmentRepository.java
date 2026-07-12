package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.myfschool.common.enums.EnrollmentStatus;
import vn.edu.fpt.myfschool.entity.Enrollment;
import vn.edu.fpt.myfschool.entity.Student;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    @Query("SELECT DISTINCT e.academicYear.id FROM Enrollment e WHERE e.student.user.id = :userId")
    List<Long> findAcademicYearIdsByStudentUserId(@Param("userId") Long userId);

    @Query("SELECT DISTINCT e.academicYear.id FROM Enrollment e, StudentGuardian sg " +
           "WHERE sg.student = e.student AND sg.guardian.user.id = :userId")
    List<Long> findAcademicYearIdsByParentUserId(@Param("userId") Long userId);
    List<Enrollment> findByStudentId(Long studentId);
    Optional<Enrollment> findByStudentIdAndAcademicYearIdAndStatus(Long studentId, Long academicYearId, EnrollmentStatus status);
    List<Enrollment> findByClsIdAndAcademicYearIdAndStatus(Long classId, Long academicYearId, EnrollmentStatus status);

    @Query("SELECT e.student FROM Enrollment e WHERE e.cls.id = :classId AND e.academicYear.id = :academicYearId AND e.status = 'ACTIVE'")
    List<Student> findActiveStudentsByClassAndYear(@Param("classId") Long classId, @Param("academicYearId") Long academicYearId);

    @Query("SELECT e FROM Enrollment e WHERE e.student.id = :studentId " +
           "AND e.academicYear.startDate <= :dateFrom AND e.academicYear.endDate >= :dateTo " +
           "AND e.status = 'ACTIVE'")
    List<Enrollment> findActiveForStudentAndDateRange(
        @Param("studentId") Long studentId,
        @Param("dateFrom") LocalDate dateFrom,
        @Param("dateTo") LocalDate dateTo);
}
