package vn.edu.fpt.myfschool.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import vn.edu.fpt.myfschool.common.enums.UserRole;
import vn.edu.fpt.myfschool.config.MailProperties;

@Service
@Slf4j
public class MailDeliveryService {
    private final MailGateway gateway;
    private final TaskExecutor taskExecutor;
    private final MailProperties properties;

    public MailDeliveryService(MailGateway gateway,
                               @Qualifier("mailTaskExecutor") TaskExecutor taskExecutor,
                               MailProperties properties) {
        this.gateway = gateway;
        this.taskExecutor = taskExecutor;
        this.properties = properties;
    }

    public void sendPasswordResetAfterCommit(String email, String resetLink) {
        afterCommit(() -> dispatch("password-reset", email,
                () -> gateway.sendPasswordResetEmail(email, resetLink)));
    }

    public void sendAccountCreatedAfterCommit(String email, String name, String username,
                                              String temporaryPassword, UserRole role) {
        afterCommit(() -> dispatch("account-created", email,
                () -> gateway.sendAccountCreatedEmail(email, name, username, temporaryPassword, role)));
    }

    public void sendPasswordChangedAfterCommit(String email, String name) {
        afterCommit(() -> dispatch("password-changed", email,
                () -> gateway.sendPasswordChangedEmail(email, name)));
    }

    private void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }

    private void dispatch(String mailType, String recipient, Runnable action) {
        taskExecutor.execute(() -> {
            int attempts = Math.max(1, properties.getMaxAttempts());
            for (int attempt = 1; attempt <= attempts; attempt++) {
                try {
                    action.run();
                    return;
                } catch (RuntimeException exception) {
                    if (attempt == attempts) {
                        log.error("Mail {} failed after {} attempts for {}", mailType, attempts, masked(recipient));
                        return;
                    }
                    log.warn("Mail {} attempt {}/{} failed for {}; retrying", mailType, attempt, attempts, masked(recipient));
                    pause();
                }
            }
        });
    }

    private void pause() {
        try {
            Thread.sleep(Math.max(0, properties.getRetryDelayMillis()));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private String masked(String email) {
        int at = email.indexOf('@');
        return at <= 1 ? "***" : email.charAt(0) + "***" + email.substring(at);
    }
}
