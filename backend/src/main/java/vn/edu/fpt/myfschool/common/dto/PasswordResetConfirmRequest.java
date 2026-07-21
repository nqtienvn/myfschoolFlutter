package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetConfirmRequest(
    @NotBlank @Size(max = 256) String token,
    @NotBlank @Size(min = 8, max = 100) String newPassword,
    @NotBlank @Size(min = 8, max = 100) String confirmPassword
) {}
