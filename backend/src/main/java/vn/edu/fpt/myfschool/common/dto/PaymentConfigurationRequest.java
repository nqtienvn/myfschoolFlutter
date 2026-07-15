package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PaymentConfigurationRequest(
    @Size(max = 30) String bankCode,
    @NotBlank @Size(max = 150) String bankName,
    @NotBlank @Pattern(regexp = "\\d{6,30}", message = "Số tài khoản phải gồm 6 đến 30 chữ số")
        String accountNumber,
    @NotBlank @Size(max = 150) String accountHolder,
    @Size(max = 150) String branch,
    @NotBlank @Size(max = 255) String transferContentTemplate,
    @NotNull Boolean enabled
) {}
