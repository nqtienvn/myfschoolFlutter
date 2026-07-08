package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.myfschool.entity.Period;

import java.util.List;
import java.util.Optional;

@Repository
public interface PeriodRepository extends JpaRepository<Period, Long> {
    List<Period> findByShiftIdOrderByOrderAsc(Long shiftId);
    List<Period> findAllByOrderByOrderAsc();
    Optional<Period> findByNameAndShiftId(String name, Long shiftId);
    boolean existsByNameAndShiftId(String name, Long shiftId);
}
