package vn.edu.fpt.myfschool.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.enums.BillStatus;
import vn.edu.fpt.myfschool.common.enums.PaymentStatus;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.entity.PaymentTransaction;
import vn.edu.fpt.myfschool.entity.TuitionBill;
import vn.edu.fpt.myfschool.repository.PaymentTransactionRepository;
import vn.edu.fpt.myfschool.repository.TuitionBillRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentTransactionService {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final TuitionBillRepository tuitionBillRepository;

    public PaymentTransaction simulatePayment(Long billId) {
        TuitionBill bill = tuitionBillRepository.findById(billId)
            .orElseThrow(() -> new ResourceNotFoundException("TuitionBill", "id", billId));

        PaymentTransaction pt = new PaymentTransaction();
        pt.setTuitionBill(bill);
        pt.setAmount(bill.getAmount());
        pt.setPaymentMethod("SIMULATED");
        pt.setStatus(PaymentStatus.SUCCESS);
        pt.setPaidAt(LocalDateTime.now());
        pt = paymentTransactionRepository.save(pt);

        bill.setStatus(BillStatus.PAID);
        bill.setPaidAt(LocalDateTime.now());
        tuitionBillRepository.save(bill);

        return pt;
    }
}
