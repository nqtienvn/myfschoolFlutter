package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateFeeCategoryRequest(
    @NotBlank @Size(max = 50) String name,
    @NotBlank @Size(max = 200) String description
) {}