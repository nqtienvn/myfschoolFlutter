package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.myfschool.controller.entity.SchoolClass;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClassRepository extends JpaRepository<SchoolClass, Long> {

    List<SchoolClass> findByAcademicYearId(Long academicYearId);

    Optional<SchoolClass> findByNameAndAcademicYearId(String name, Long academicYearId);

    boolean existsByNameAndAcademicYearId(String name, Long academicYearId);

    @Query("SELECT c FROM SchoolClass c WHERE c.academicYear.id = :academicYearId AND " +
           "(LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')))" )
    List<SchoolClass> searchByYearAndKeyword(@Param("academicYearId") Long academicYearId,
                                              @Param("keyword") String keyword);

    @Query("SELECT DISTINCT cs.cls FROM ClassSubject cs " +
           "WHERE cs.teacher.id = :teacherId AND cs.academicYear = :year")
    List<SchoolClass> findClassesByTeacher(@Param("teacherId") Long teacherId,
                                            @Param("year") String year);
}
