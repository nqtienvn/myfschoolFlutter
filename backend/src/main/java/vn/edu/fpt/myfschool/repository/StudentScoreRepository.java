package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.myfschool.entity.StudentScore;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentScoreRepository extends JpaRepository<StudentScore, Long> {
    List<StudentScore> findByGradeItemId(Long gradeItemId);
    Optional<StudentScore> findByGradeItemIdAndStudentId(Long gradeItemId, Long studentId);
    List<StudentScore> findByStudentIdAndGradeItemGradeBookSemesterId(Long studentId, Long semesterId);
    List<StudentScore> findByGradeItemGradeBookId(Long gradeBookId);
}
