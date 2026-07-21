package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.common.enums.UserRole;

public interface MailGateway {
    void sendPasswordResetEmail(String email, String resetLink);
    void sendAccountCreatedEmail(String email, String name, String username,
                                 String temporaryPassword, UserRole role);
    void sendPasswordChangedEmail(String email, String name);
}
