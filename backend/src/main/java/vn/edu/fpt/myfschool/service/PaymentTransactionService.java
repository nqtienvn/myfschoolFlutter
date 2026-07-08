package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.controller.entity.PaymentTransaction;

public interface PaymentTransactionService {
    PaymentTransaction simulatePayment(Long billId);
}
