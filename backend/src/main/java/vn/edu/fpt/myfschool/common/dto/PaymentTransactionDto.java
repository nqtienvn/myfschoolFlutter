package vn.edu.fpt.myfschool.common.dto;

import vn.edu.fpt.myfschool.common.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentTransactionDto(
    Long id,
    BigDecimal amount,
    String paymentMethod,
    String transactionRef,
    PaymentStatus status,
    LocalDateTime paidAt,
    LocalDateTime createdAt
) {}
