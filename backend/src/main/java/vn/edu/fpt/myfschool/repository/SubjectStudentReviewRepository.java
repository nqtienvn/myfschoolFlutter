package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.myfschool.entity.SubjectStudentReview;

import java.util.List;
import java.util.Optional;

public interface SubjectStudentReviewRepository extends JpaRepository<SubjectStudentReview, Long> {
    Optional<SubjectStudentReview> findByStudentIdAndSubjectIdAndSemesterId(
            Long studentId, Long subjectId, Long semesterId);
    List<SubjectStudentReview> findByClsIdAndSubjectIdAndSemesterIdOrderByStudentStudentCode(
            Long classId, Long subjectId, Long semesterId);
    List<SubjectStudentReview> findByClsIdAndSemesterId(Long classId, Long semesterId);
    List<SubjectStudentReview> findByStudentIdAndSemesterId(Long studentId, Long semesterId);
}
