package vn.edu.fpt.myfschool.service.impl;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import vn.edu.fpt.myfschool.common.enums.UserRole;
import vn.edu.fpt.myfschool.service.MailGateway;

import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "FAKE", matchIfMissing = true)
public class FakeMailGateway implements MailGateway {
    private final List<SentMail> sent = new ArrayList<>();

    @Override
    public synchronized void sendPasswordResetEmail(String email, String resetLink) {
        sent.add(new SentMail("PASSWORD_RESET", email, resetLink, null, null, null));
    }

    @Override
    public synchronized void sendAccountCreatedEmail(String email, String name, String username,
                                                      String temporaryPassword, UserRole role) {
        sent.add(new SentMail("ACCOUNT_CREATED", email, null, username, temporaryPassword, role));
    }

    @Override
    public synchronized void sendPasswordChangedEmail(String email, String name) {
        sent.add(new SentMail("PASSWORD_CHANGED", email, null, null, null, null));
    }

    public synchronized List<SentMail> sentMessages() {
        return List.copyOf(sent);
    }

    public synchronized void clear() {
        sent.clear();
    }

    public record SentMail(String type, String recipient, String resetLink, String username,
                           String temporaryPassword, UserRole role) {}
}
