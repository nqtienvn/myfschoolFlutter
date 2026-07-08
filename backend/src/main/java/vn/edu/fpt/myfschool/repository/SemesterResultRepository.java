package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.myfschool.entity.SemesterResult;
import java.util.List;
import java.util.Optional;

@Repository
public interface SemesterResultRepository extends JpaRepository<SemesterResult, Long> {
    Optional<SemesterResult> findByStudentIdAndSemesterId(Long studentId, Long semesterId);

    @Query("SELECT sr FROM SemesterResult sr WHERE sr.cls.id = :classId AND sr.semester.id = :semesterId ORDER BY sr.rank ASC")
    List<SemesterResult> findByClassIdAndSemesterIdOrderByRankAsc(@Param("classId") Long classId, @Param("semesterId") Long semesterId);

    List<SemesterResult> findBySemesterId(Long semesterId);
    List<SemesterResult> findByStudentIdOrderBySemesterIdDesc(Long studentId);
}
