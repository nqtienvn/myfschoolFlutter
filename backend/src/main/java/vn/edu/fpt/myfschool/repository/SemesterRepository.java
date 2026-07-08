package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.myfschool.controller.entity.Semester;
import java.util.List;
import java.util.Optional;

@Repository
public interface SemesterRepository extends JpaRepository<Semester, Long> {

    List<Semester> findByAcademicYearId(Long academicYearId);

    List<Semester> findByAcademicYearIdOrderByOrderAsc(Long academicYearId);

    Optional<Semester> findByIsCurrentTrue();

    Optional<Semester> findByNameAndAcademicYearId(String name, Long academicYearId);
}
