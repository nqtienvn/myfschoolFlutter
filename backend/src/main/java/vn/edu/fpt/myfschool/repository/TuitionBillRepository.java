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

    @Query("SELECT tb FROM TuitionBill tb WHERE tb.cls.id = :classId AND tb.semester.id = :semesterId ORDER BY tb.createdAt DESC")
    List<TuitionBill> findByClassIdAndSemesterIdOrderByCreatedAtDesc(@Param("classId") Long classId, @Param("semesterId") Long semesterId);

    @Query("SELECT tb FROM TuitionBill tb WHERE tb.cls.id = :classId AND tb.semester.id = :semesterId AND tb.status = :status ORDER BY tb.createdAt DESC")
    List<TuitionBill> findByClassIdAndSemesterIdAndStatus(@Param("classId") Long classId, @Param("semesterId") Long semesterId, @Param("status") BillStatus status);

    boolean existsByStudentIdAndSemesterIdAndName(Long studentId, Long semesterId, String name);

    boolean existsByStudentIdAndSemesterIdAndFeeTemplateId(Long studentId, Long semesterId, Long feeTemplateId);

    boolean existsByFeeTemplateId(Long feeTemplateId);

    @Query("SELECT COALESCE(SUM(tb.amount), 0) FROM TuitionBill tb WHERE tb.student.id = :studentId AND tb.status = 'UNPAID'")
    BigDecimal totalUnpaidByStudent(@Param("studentId") Long studentId);
}
