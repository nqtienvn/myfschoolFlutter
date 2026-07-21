package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.myfschool.entity.AcademicYearResult;

import java.util.List;
import java.util.Optional;

@Repository
public interface AcademicYearResultRepository extends JpaRepository<AcademicYearResult, Long> {
    Optional<AcademicYearResult> findByStudentIdAndAcademicYearId(Long studentId, Long academicYearId);
    List<AcademicYearResult> findByClsIdAndAcademicYearIdOrderByRankAsc(Long classId, Long academicYearId);
    List<AcademicYearResult> findByAcademicYearId(Long academicYearId);
    long countByAcademicYearIdAndPublishedAtIsNotNull(Long academicYearId);
}
