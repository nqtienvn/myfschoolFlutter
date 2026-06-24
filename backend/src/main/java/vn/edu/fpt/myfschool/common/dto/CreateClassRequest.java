package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.*;

public record CreateClassRequest(
    @NotBlank @Size(max = 20) String name,
    @NotNull @Min(1) @Max(12) Integer gradeLevel,
    @NotBlank @Size(max = 9) String academicYear,
    @Size(max = 200) String schoolName
) {}
