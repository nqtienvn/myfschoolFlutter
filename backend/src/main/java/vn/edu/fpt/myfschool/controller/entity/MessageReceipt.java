package vn.edu.fpt.myfschool.controller.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import vn.edu.fpt.myfschool.common.enums.MessageReceiptStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "message_receipts",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_receipt_message_user",
                columnNames = {"message_id", "user_id"}
        ),
        indexes = {
                @Index(name = "idx_receipt_user_status", columnList = "user_id, status"),
                @Index(name = "idx_receipt_message", columnList = "message_id")
        })
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class MessageReceipt extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_receipt_message"))
    private Message message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_receipt_user"))
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageReceiptStatus status = MessageReceiptStatus.DELIVERED;

    private LocalDateTime deliveredAt;

    private LocalDateTime readAt;
}
