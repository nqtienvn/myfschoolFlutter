package vn.edu.fpt.myfschool.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;
import vn.edu.fpt.myfschool.common.enums.BillStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tuition_bills",
       uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "semester_id", "fee_template_id"}))
@Data
@EqualsAndHashCode(callSuper = true)
public class TuitionBill extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_tb_student"))
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_tb_class"))
    private SchoolClass cls;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_tb_semester"))
    private Semester semester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fee_template_id",
                foreignKey = @ForeignKey(name = "fk_tb_fee_template"))
    private FeeTemplate feeTemplate;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BillStatus status = BillStatus.UNPAID;

    private LocalDateTime paidAt;

    @Column(name = "last_reminder_at")
    private LocalDateTime lastReminderAt;

    @OneToMany(mappedBy = "tuitionBill", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PaymentTransaction> paymentTransactions = new ArrayList<>();
}
