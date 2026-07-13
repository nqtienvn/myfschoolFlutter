package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateTeacherProfileRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Pattern(regexp = "0[0-9]{9}", message = "Số điện thoại phải gồm 10 chữ số và bắt đầu bằng 0") String phone,
        @Email @Size(max = 255) String email) {
}
