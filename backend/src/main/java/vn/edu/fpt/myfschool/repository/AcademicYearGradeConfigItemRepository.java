package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.myfschool.entity.AcademicYearGradeConfigItem;
import java.util.List;

public interface AcademicYearGradeConfigItemRepository extends JpaRepository<AcademicYearGradeConfigItem,Long> {
    List<AcademicYearGradeConfigItem> findByConfigAcademicYearIdOrderByDisplayOrderAsc(Long academicYearId);
}
