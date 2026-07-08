package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.myfschool.entity.FeeCategory;
import java.util.Optional;

@Repository
public interface FeeCategoryRepository extends JpaRepository<FeeCategory, Long> {
    boolean existsByName(String name);
    Optional<FeeCategory> findByName(String name);
}