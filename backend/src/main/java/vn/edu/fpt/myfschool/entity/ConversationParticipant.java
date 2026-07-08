package vn.edu.fpt.myfschool.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Entity
@Table(name = "conversation_participants",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"conversation_id", "user_id"}))
@Data
@EqualsAndHashCode(callSuper = true)
public class ConversationParticipant extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_cp_conversation"))
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_cp_user"))
    private User user;

    private LocalDateTime joinedAt;

    private LocalDateTime lastReadAt;

    private Long lastReadMessageId;

    private LocalDateTime lastSeenAt;
}
