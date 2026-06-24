package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.myfschool.entity.PaymentTransaction;
import vn.edu.fpt.myfschool.common.enums.PaymentStatus;
import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    List<PaymentTransaction> findByTuitionBillIdOrderByCreatedAtDesc(Long billId);

    @Query("SELECT COALESCE(SUM(pt.amount), 0) FROM PaymentTransaction pt " +
           "WHERE pt.tuitionBill.id = :billId AND pt.status = 'SUCCESS'")
    BigDecimal totalPaidByBill(@Param("billId") Long billId);
}
