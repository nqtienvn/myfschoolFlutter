package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.common.dto.PasswordResetConfirmRequest;
import vn.edu.fpt.myfschool.common.dto.PasswordResetValidationResponse;

public interface PasswordResetService {
    void request(String phone, String requestedIp);
    PasswordResetValidationResponse validate(String rawToken);
    void confirm(PasswordResetConfirmRequest request);
}
