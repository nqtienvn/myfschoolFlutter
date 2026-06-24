package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.myfschool.entity.Grade;
import java.util.List;
import java.util.Optional;

@Repository
public interface GradeRepository extends JpaRepository<Grade, Long> {
    List<Grade> findByStudentIdAndSemesterId(Long studentId, Long semesterId);
    List<Grade> findBySubjectIdAndSemesterId(Long subjectId, Long semesterId);

    @Query("SELECT g FROM Grade g JOIN g.student s " +
           "WHERE g.subject.id = :subjectId AND g.semester.id = :semesterId " +
           "AND s.currentClass.id = :classId")
    List<Grade> findBySubjectSemesterClass(@Param("subjectId") Long subjectId,
                                            @Param("semesterId") Long semesterId,
                                            @Param("classId") Long classId);

    Optional<Grade> findByStudentIdAndSubjectIdAndSemesterId(
        Long studentId, Long subjectId, Long semesterId);

    boolean existsByStudentIdAndSubjectIdAndSemesterId(
        Long studentId, Long subjectId, Long semesterId);
}
