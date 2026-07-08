package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.myfschool.controller.entity.MessageReceipt;

import java.util.Optional;

@Repository
public interface MessageReceiptRepository extends JpaRepository<MessageReceipt, Long> {
    Optional<MessageReceipt> findByMessageIdAndUserId(Long messageId, Long userId);
}
