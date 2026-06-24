package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.myfschool.entity.Conversation;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    @Query("SELECT c FROM Conversation c JOIN c.participants p " +
           "WHERE p.user.id = :userId ORDER BY c.lastMessageAt DESC NULLS LAST")
    java.util.List<Conversation> findConversationsByUserId(@Param("userId") Long userId);

    @Query("SELECT c FROM Conversation c JOIN c.participants p1 JOIN c.participants p2 " +
           "WHERE p1.user.id = :userId1 AND p2.user.id = :userId2 AND SIZE(c.participants) = 2")
    Optional<Conversation> findConversationBetweenUsers(
        @Param("userId1") Long userId1, @Param("userId2") Long userId2);
}
