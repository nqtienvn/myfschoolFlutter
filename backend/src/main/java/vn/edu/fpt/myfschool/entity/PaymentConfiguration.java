package vn.edu.fpt.myfschool.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "payment_configurations",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_payment_configurations_academic_year",
        columnNames = "academic_year_id"))
@Getter
@Setter
@NoArgsConstructor
public class PaymentConfiguration extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "academic_year_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_payment_configurations_academic_year"))
    private AcademicYear academicYear;

    @Column(name = "bank_code", length = 30)
    private String bankCode;

    @Column(name = "bank_name", nullable = false, length = 150)
    private String bankName;

    @Column(name = "account_number", nullable = false, length = 30)
    private String accountNumber;

    @Column(name = "account_holder", nullable = false, length = 150)
    private String accountHolder;

    @Column(length = 150)
    private String branch;

    @Column(name = "transfer_content_template", nullable = false, length = 255)
    private String transferContentTemplate;

    @Column(nullable = false)
    private Boolean enabled = true;
}
