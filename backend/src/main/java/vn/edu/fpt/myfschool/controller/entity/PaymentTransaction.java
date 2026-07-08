package vn.edu.fpt.myfschool.controller.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import vn.edu.fpt.myfschool.common.enums.PaymentStatus;

@Entity
@Table(name = "payment_transactions")
@Data
@EqualsAndHashCode(callSuper = true)
public class PaymentTransaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_pt_bill"))
    private TuitionBill tuitionBill;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(length = 50)
    private String paymentMethod;

    @Column(length = 100)
    private String transactionRef;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.PENDING;

    private LocalDateTime paidAt;
}
