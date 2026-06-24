package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank @Size(min = 10, max = 15) String phone,
    @NotBlank @Size(min = 6, max = 100) String password
) {}
