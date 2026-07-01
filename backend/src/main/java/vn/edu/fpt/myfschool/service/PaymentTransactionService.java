package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.common.enums.BillStatus;
import vn.edu.fpt.myfschool.common.enums.PaymentStatus;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.entity.PaymentTransaction;
import vn.edu.fpt.myfschool.entity.TuitionBill;
import vn.edu.fpt.myfschool.repository.PaymentTransactionRepository;
import vn.edu.fpt.myfschool.repository.TuitionBillRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface PaymentTransactionService {
    PaymentTransaction simulatePayment(Long billId);
}
