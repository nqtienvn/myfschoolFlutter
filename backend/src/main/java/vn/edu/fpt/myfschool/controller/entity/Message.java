package vn.edu.fpt.myfschool.controller.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import vn.edu.fpt.myfschool.common.enums.MessageType;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "messages",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_message_sender_client",
                        columnNames = {"sender_id", "client_message_id"}
                ),
                @UniqueConstraint(
                        name = "uk_message_conversation_seq",
                        columnNames = {"conversation_id", "server_seq"}
                )
        },
        indexes = {
                @Index(name = "idx_message_conversation_created", columnList = "conversation_id, created_at"),
                @Index(name = "idx_message_conversation_seq", columnList = "conversation_id, server_seq")
        })
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class Message extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_msg_conversation"))
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_msg_sender"))
    private User sender;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Attachment> attachments = new ArrayList<>();

    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<MessageReceipt> receipts = new ArrayList<>();

    @Column(name = "client_message_id", nullable = false, length = 80)
    private String clientMessageId;


    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 20)
    private MessageType messageType = MessageType.TEXT;

    @Column(name = "server_seq", nullable = false)
    private Long serverSeq;
}
