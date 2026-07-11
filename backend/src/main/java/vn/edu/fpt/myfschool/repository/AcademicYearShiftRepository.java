package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.myfschool.entity.AcademicYearShift;
import java.util.List;

public interface AcademicYearShiftRepository extends JpaRepository<AcademicYearShift, Long> {
    List<AcademicYearShift> findByAcademicYearId(Long academicYearId);
    boolean existsByAcademicYearIdAndShiftId(Long academicYearId, Long shiftId);
}
