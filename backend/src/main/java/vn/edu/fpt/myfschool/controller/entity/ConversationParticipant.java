package vn.edu.fpt.myfschool.controller.entity;

import jakarta.persistence.*;
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
