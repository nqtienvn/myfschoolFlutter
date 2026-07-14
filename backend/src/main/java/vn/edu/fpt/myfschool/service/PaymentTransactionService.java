package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.common.dto.PaymentTransactionDto;

public interface PaymentTransactionService {
    PaymentTransactionDto requestBankTransfer(Long billId);

    PaymentTransactionDto simulatePayment(Long billId);
}
