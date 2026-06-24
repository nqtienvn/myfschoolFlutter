package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.myfschool.entity.SemesterResult;
import java.util.List;
import java.util.Optional;

@Repository
public interface SemesterResultRepository extends JpaRepository<SemesterResult, Long> {
    Optional<SemesterResult> findByStudentIdAndSemesterId(Long studentId, Long semesterId);
    List<SemesterResult> findByClassIdAndSemesterIdOrderByRankAsc(Long classId, Long semesterId);
    List<SemesterResult> findBySemesterId(Long semesterId);
    List<SemesterResult> findByStudentIdOrderBySemesterIdDesc(Long studentId);
}
