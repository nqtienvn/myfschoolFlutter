package vn.edu.fpt.myfschool.service.impl;

import vn.edu.fpt.myfschool.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.enums.BillStatus;
import vn.edu.fpt.myfschool.common.enums.PaymentStatus;
import vn.edu.fpt.myfschool.common.exception.BadRequestException;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.entity.PaymentTransaction;
import vn.edu.fpt.myfschool.entity.TuitionBill;
import vn.edu.fpt.myfschool.repository.PaymentTransactionRepository;
import vn.edu.fpt.myfschool.repository.TuitionBillRepository;

import java.time.LocalDateTime;

@Service("paymentTransactionService")
@RequiredArgsConstructor
@Transactional
public class PaymentTransactionServiceImpl implements PaymentTransactionService {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final TuitionBillRepository tuitionBillRepository;

    @Override
    public PaymentTransaction simulatePayment(Long billId) {
        TuitionBill bill = tuitionBillRepository.findById(billId)
            .orElseThrow(() -> new ResourceNotFoundException("TuitionBill", "id", billId));
        if (bill.getStatus() == BillStatus.PAID) {
            throw new BadRequestException("Khoản học phí đã được thanh toán");
        }

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
