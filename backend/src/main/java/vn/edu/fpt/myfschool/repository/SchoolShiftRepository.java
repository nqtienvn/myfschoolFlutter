package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.myfschool.entity.SchoolShift;

import java.util.List;
import java.util.Optional;

@Repository
public interface SchoolShiftRepository extends JpaRepository<SchoolShift, Long> {
    Optional<SchoolShift> findByCode(String code);
    boolean existsByCode(String code);
    List<SchoolShift> findAllByOrderByOrderAsc();
}
