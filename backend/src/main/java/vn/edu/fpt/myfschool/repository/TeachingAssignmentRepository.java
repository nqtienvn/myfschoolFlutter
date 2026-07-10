package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.myfschool.common.enums.AssignmentStatus;
import vn.edu.fpt.myfschool.entity.TeachingAssignment;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TeachingAssignmentRepository extends JpaRepository<TeachingAssignment, Long> {

    List<TeachingAssignment> findByClsIdAndSemesterIdAndStatus(
        Long classId, Long semesterId, AssignmentStatus status);

    List<TeachingAssignment> findByClsIdAndStatus(Long classId, AssignmentStatus status);

    List<TeachingAssignment> findByClsIdAndSubjectId(Long classId, Long subjectId);

    List<TeachingAssignment> findByTeacherIdAndSemesterIdAndStatus(
        Long teacherId, Long semesterId, AssignmentStatus status);

    List<TeachingAssignment> findByTeacherIdAndStatus(Long teacherId, AssignmentStatus status);

    boolean existsByTeacherIdAndClsIdAndStatus(Long teacherId, Long classId, AssignmentStatus status);

    @Query("SELECT DISTINCT ta.cls.id FROM TeachingAssignment ta WHERE ta.teacher.id = :teacherId AND ta.status = 'ACTIVE'")
    List<Long> findActiveClassIdsByTeacherId(@Param("teacherId") Long teacherId);

    List<TeachingAssignment> findBySemesterIdAndStatus(
        Long semesterId, AssignmentStatus status);

    @Query("SELECT ta FROM TeachingAssignment ta " +
           "WHERE ta.cls.id = :classId AND ta.subject.id = :subjectId " +
           "AND ta.semester.id = :semesterId AND ta.status = 'ACTIVE' " +
           "AND (ta.effectiveTo IS NULL OR ta.effectiveTo >= CURRENT_DATE)")
    List<TeachingAssignment> findActiveByClassSubjectSemester(
        @Param("classId") Long classId,
        @Param("subjectId") Long subjectId,
        @Param("semesterId") Long semesterId);

    @Query("SELECT ta FROM TeachingAssignment ta " +
           "WHERE ta.cls.academicYear.id = :academicYearId " +
           "AND ta.status = 'ACTIVE'")
    List<TeachingAssignment> findByAcademicYearId(
        @Param("academicYearId") Long academicYearId);

    boolean existsByClsIdAndSubjectIdAndSemesterIdAndEffectiveFrom(
        Long classId, Long subjectId, Long semesterId, LocalDate effectiveFrom);

    Optional<TeachingAssignment> findByClsIdAndSubjectIdAndSemesterId(Long classId, Long subjectId, Long semesterId);

    boolean existsByClsIdAndSubjectIdAndSemesterIdAndIdNot(Long classId, Long subjectId, Long semesterId, Long id);

    boolean existsByTeacherIdAndSubjectIdAndStatus(Long teacherId, Long subjectId, AssignmentStatus status);

    void deleteByClsId(Long classId);
}
