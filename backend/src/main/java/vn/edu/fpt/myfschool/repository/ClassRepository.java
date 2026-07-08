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

    List<SchoolClass> findByAcademicYear(String academicYear);

    Optional<SchoolClass> findByNameAndAcademicYear(String name, String academicYear);

    boolean existsByNameAndAcademicYear(String name, String academicYear);

    @Query("SELECT c FROM SchoolClass c WHERE c.academicYear = :year AND " +
           "(LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<SchoolClass> searchByYearAndKeyword(@Param("year") String year,
                                              @Param("keyword") String keyword);

    @Query("SELECT DISTINCT cs.cls FROM ClassSubject cs " +
           "WHERE cs.teacher.id = :teacherId AND cs.academicYear = :year")
    List<SchoolClass> findClassesByTeacher(@Param("teacherId") Long teacherId,
                                            @Param("year") String year);
}
