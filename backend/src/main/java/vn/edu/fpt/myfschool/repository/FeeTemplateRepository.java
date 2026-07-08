package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.myfschool.entity.FeeTemplate;
import java.util.List;

@Repository
public interface FeeTemplateRepository extends JpaRepository<FeeTemplate, Long> {
    List<FeeTemplate> findByClsIdAndSemesterId(Long classId, Long semesterId);
    List<FeeTemplate> findByClsId(Long classId);
    boolean existsByFeeCategoryIdAndClsIdAndSemesterId(Long categoryId, Long classId, Long semesterId);

    @Query("SELECT ft FROM FeeTemplate ft WHERE ft.cls.academicYear.id = :academicYearId")
    List<FeeTemplate> findByAcademicYearId(@Param("academicYearId") Long academicYearId);
}