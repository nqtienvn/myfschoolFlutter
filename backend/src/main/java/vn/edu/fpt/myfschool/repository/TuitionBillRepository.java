package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.myfschool.entity.TuitionBill;
import vn.edu.fpt.myfschool.common.enums.BillStatus;
import java.math.BigDecimal;
import java.util.List;

@Repository
public interface TuitionBillRepository extends JpaRepository<TuitionBill, Long> {
    List<TuitionBill> findByStudentIdOrderByCreatedAtDesc(Long studentId);
    List<TuitionBill> findByClassIdAndSemesterIdOrderByCreatedAtDesc(Long classId, Long semesterId);
    List<TuitionBill> findByClassIdAndSemesterIdAndStatus(Long classId, Long semesterId, BillStatus status);

    boolean existsByStudentIdAndSemesterIdAndName(Long studentId, Long semesterId, String name);

    @Query("SELECT COALESCE(SUM(tb.amount), 0) FROM TuitionBill tb " +
           "WHERE tb.student.id = :studentId AND tb.status = 'UNPAID'")
    BigDecimal totalUnpaidByStudent(@Param("studentId") Long studentId);
}
