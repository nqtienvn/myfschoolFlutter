package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateTeacherAccountRequest(
        @NotBlank @Size(min = 10, max = 15) String phone,
        @NotBlank @Size(max = 100) String name,
        @Email String email,
        @NotBlank @Size(max = 20) String employeeCode,
        @Size(max = 100) String department,
        @NotEmpty List<Long> subjectIds) {
}
