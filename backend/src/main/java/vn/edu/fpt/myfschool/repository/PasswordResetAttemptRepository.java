package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.myfschool.entity.PasswordResetAttempt;

import java.time.LocalDateTime;

public interface PasswordResetAttemptRepository extends JpaRepository<PasswordResetAttempt, Long> {
    long countByPhoneHashAndCreatedAtAfter(String phoneHash, LocalDateTime after);
    long countByRequestedIpAndCreatedAtAfter(String requestedIp, LocalDateTime after);
}
