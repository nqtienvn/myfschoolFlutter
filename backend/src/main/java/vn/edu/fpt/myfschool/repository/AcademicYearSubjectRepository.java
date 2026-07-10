package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.myfschool.entity.AcademicYearSubject;
import java.util.List;

public interface AcademicYearSubjectRepository extends JpaRepository<AcademicYearSubject, Long> {
    List<AcademicYearSubject> findByAcademicYearId(Long academicYearId);
    void deleteByAcademicYearId(Long academicYearId);
    boolean existsByAcademicYearIdAndSubjectId(Long academicYearId, Long subjectId);
}
