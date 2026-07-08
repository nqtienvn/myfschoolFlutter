package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.myfschool.controller.entity.GradeBook;
import java.util.List;
import java.util.Optional;

@Repository
public interface GradeBookRepository extends JpaRepository<GradeBook, Long> {
    List<GradeBook> findBySemesterId(Long semesterId);
    List<GradeBook> findByClsIdAndSemesterId(Long classId, Long semesterId);
    Optional<GradeBook> findByClsIdAndSubjectIdAndSemesterId(Long classId, Long subjectId, Long semesterId);
    boolean existsByClsIdAndSubjectIdAndSemesterId(Long classId, Long subjectId, Long semesterId);
}