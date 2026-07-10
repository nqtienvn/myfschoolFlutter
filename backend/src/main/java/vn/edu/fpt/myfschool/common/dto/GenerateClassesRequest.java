package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.*;

public record GenerateClassesRequest(
    @NotNull Long academicYearId,
    @NotNull @Min(1) @Max(12) Integer gradeLevel,
    @NotBlank @Pattern(regexp = "[A-Za-z]{1,3}") String namingPrefix,
    @NotNull @Min(1) @Max(50) Integer count
) {}
