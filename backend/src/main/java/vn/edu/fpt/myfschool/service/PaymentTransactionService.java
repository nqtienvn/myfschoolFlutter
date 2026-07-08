package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.entity.PaymentTransaction;

public interface PaymentTransactionService {
    PaymentTransaction simulatePayment(Long billId);
}
