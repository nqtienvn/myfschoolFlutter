package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.myfschool.controller.entity.Semester;
import java.util.List;
import java.util.Optional;

@Repository
public interface SemesterRepository extends JpaRepository<Semester, Long> {

    List<Semester> findByAcademicYear(String academicYear);

    Optional<Semester> findByIsCurrentTrue();

    Optional<Semester> findByNameAndAcademicYear(String name, String academicYear);
}
