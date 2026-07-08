package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.myfschool.common.enums.AcademicYearStatus;
import vn.edu.fpt.myfschool.controller.entity.AcademicYear;

import java.util.List;
import java.util.Optional;

@Repository
public interface AcademicYearRepository extends JpaRepository<AcademicYear, Long> {
    boolean existsByName(String name);
    Optional<AcademicYear> findByName(String name);
    List<AcademicYear> findByStatus(AcademicYearStatus status);
}
