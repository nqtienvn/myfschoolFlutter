package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
    @Size(max = 100) String name,
    @Email String email,
    String address,
    String occupation
) {}
