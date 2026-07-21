package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetValidateRequest(
    @NotBlank @Size(max = 256) String token
) {}
