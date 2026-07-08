package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.myfschool.controller.entity.ConversationParticipant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, Long> {
    List<ConversationParticipant> findByConversationId(Long conversationId);
    Optional<ConversationParticipant> findByConversationIdAndUserId(Long conversationId, Long userId);
    boolean existsByConversationIdAndUserId(Long conversationId, Long userId);

    @Query("SELECT cp.user.id FROM ConversationParticipant cp " +
            "WHERE cp.conversation.id = :conversationId AND cp.user.id <> :userId")
    List<Long> findOtherUserIds(@Param("conversationId") Long conversationId, @Param("userId") Long userId);

    @Query("SELECT cp.conversation.id FROM ConversationParticipant cp WHERE cp.user.id = :userId")
    List<Long> findConversationIdsByUserId(@Param("userId") Long userId);

    @Query("SELECT DISTINCT other.user.id FROM ConversationParticipant mine " +
            "JOIN ConversationParticipant other ON other.conversation.id = mine.conversation.id " +
            "WHERE mine.user.id = :userId AND other.user.id <> :userId")
    List<Long> findRelatedUserIds(@Param("userId") Long userId);
}
