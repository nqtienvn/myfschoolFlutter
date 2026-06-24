package vn.edu.fpt.myfschool.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.myfschool.entity.Message;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByConversationIdOrderByCreatedAtDesc(Long conversationId, Pageable pageable);

    @Query("SELECT m FROM Message m WHERE m.conversation.id = :convId AND m.id < :beforeId ORDER BY m.createdAt DESC")
    List<Message> findMessagesBefore(@Param("convId") Long convId,
                                      @Param("beforeId") Long beforeId, Pageable pageable);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversation.id = :convId " +
           "AND m.sender.id != :userId AND m.createdAt > " +
           "(SELECT COALESCE(cp.lastReadAt, cp.joinedAt) FROM ConversationParticipant cp " +
           "WHERE cp.conversation.id = :convId AND cp.user.id = :userId)")
    long countUnread(@Param("convId") Long convId, @Param("userId") Long userId);
}
