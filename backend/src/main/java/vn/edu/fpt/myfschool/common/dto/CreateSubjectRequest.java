package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSubjectRequest(
    @NotBlank @Size(max = 100) String name,
    @NotBlank @Size(max = 20) String code
) {}
