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
import java.util.Collection;

@Repository
public interface TeachingAssignmentRepository extends JpaRepository<TeachingAssignment, Long> {
    @Query("SELECT DISTINCT ta.cls.academicYear.id FROM TeachingAssignment ta WHERE ta.teacher.user.id = :userId")
    List<Long> findAcademicYearIdsByTeacherUserId(@Param("userId") Long userId);

    List<TeachingAssignment> findByClsIdAndStatus(Long classId, AssignmentStatus status);

    Optional<TeachingAssignment> findByClsIdAndSubjectId(Long classId, Long subjectId);

    List<TeachingAssignment> findByTeacherIdAndStatus(Long teacherId, AssignmentStatus status);

    boolean existsByTeacherIdAndClsIdAndStatus(Long teacherId, Long classId, AssignmentStatus status);

    @Query("SELECT DISTINCT ta.cls.id FROM TeachingAssignment ta WHERE ta.teacher.id = :teacherId AND ta.status = 'ACTIVE'")
    List<Long> findActiveClassIdsByTeacherId(@Param("teacherId") Long teacherId);

    @Query("SELECT ta FROM TeachingAssignment ta " +
           "WHERE ta.cls.academicYear.id = :academicYearId " +
           "AND ta.status = 'ACTIVE'")
    List<TeachingAssignment> findByAcademicYearId(
        @Param("academicYearId") Long academicYearId);

    boolean existsByClsIdAndSubjectIdAndIdNot(Long classId, Long subjectId, Long id);

    boolean existsByTeacherIdAndSubjectIdAndStatus(Long teacherId, Long subjectId, AssignmentStatus status);

    void deleteByClsId(Long classId);

    @Query("SELECT CASE WHEN COUNT(ta) > 0 THEN true ELSE false END FROM TeachingAssignment ta " +
           "WHERE ta.cls.academicYear.id = :academicYearId AND ta.subject.id IN :subjectIds")
    boolean existsInAcademicYearBySubjectIds(@Param("academicYearId") Long academicYearId,
                                              @Param("subjectIds") Collection<Long> subjectIds);
}
