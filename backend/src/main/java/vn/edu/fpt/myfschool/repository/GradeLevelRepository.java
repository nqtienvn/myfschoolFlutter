package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.myfschool.entity.GradeLevel;

import java.util.List;
import java.util.Optional;

@Repository
public interface GradeLevelRepository extends JpaRepository<GradeLevel, Long> {
    List<GradeLevel> findAllByOrderByOrderAsc();
    Optional<GradeLevel> findByCode(String code);
    boolean existsByCode(String code);
}
