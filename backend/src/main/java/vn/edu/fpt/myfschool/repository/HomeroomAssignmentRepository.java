package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.myfschool.entity.HomeroomAssignment;
import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

@Repository
public interface HomeroomAssignmentRepository extends JpaRepository<HomeroomAssignment, Long> {

    List<HomeroomAssignment> findByClsIdAndAcademicYearId(Long classId, Long academicYearId);

    List<HomeroomAssignment> findByTeacherIdAndAcademicYearId(Long teacherId, Long academicYearId);

    @Query("SELECT ha FROM HomeroomAssignment ha " +
           "WHERE ha.cls.id = :classId AND ha.academicYear.id = :academicYearId " +
           "AND (ha.effectiveTo IS NULL OR ha.effectiveTo > CURRENT_DATE)")
    Optional<HomeroomAssignment> findActiveByClassAndYear(
        @Param("classId") Long classId,
        @Param("academicYearId") Long academicYearId);

    @Query("SELECT ha FROM HomeroomAssignment ha " +
           "WHERE ha.teacher.id = :teacherId AND ha.academicYear.id = :academicYearId " +
           "AND (ha.effectiveTo IS NULL OR ha.effectiveTo > CURRENT_DATE)")
    List<HomeroomAssignment> findActiveByTeacherAndYear(
        @Param("teacherId") Long teacherId,
        @Param("academicYearId") Long academicYearId);

    @Query("SELECT ha FROM HomeroomAssignment ha " +
           "WHERE ha.teacher.id = :teacherId " +
           "AND ha.academicYear.startDate <= :date AND ha.academicYear.endDate >= :date " +
           "AND ha.effectiveFrom <= :date AND (ha.effectiveTo IS NULL OR ha.effectiveTo >= :date)")
    List<HomeroomAssignment> findActiveByTeacherAndDate(
        @Param("teacherId") Long teacherId,
        @Param("date") LocalDate date);

    @Query("SELECT COUNT(ha) > 0 FROM HomeroomAssignment ha " +
           "WHERE ha.teacher.id = :teacherId AND ha.cls.id = :classId " +
           "AND ha.academicYear.id = :academicYearId " +
           "AND ha.effectiveFrom <= :date AND (ha.effectiveTo IS NULL OR ha.effectiveTo >= :date)")
    boolean existsActiveForTeacherClassAndDate(
        @Param("teacherId") Long teacherId,
        @Param("classId") Long classId,
        @Param("academicYearId") Long academicYearId,
        @Param("date") LocalDate date);

    void deleteByClsId(Long classId);
}
