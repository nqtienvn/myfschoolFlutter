package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.*;
import vn.edu.fpt.myfschool.common.enums.UserRole;

public record RegisterRequest(
    @NotBlank @Size(min = 10, max = 15) String phone,
    @NotBlank @Size(min = 6, max = 100) String password,
    @NotBlank @Size(max = 100) String name,
    @Email String email,
    @NotNull UserRole role,
    String studentCode,
    String employeeCode,
    String department,
    String address,
    String occupation
) {}
