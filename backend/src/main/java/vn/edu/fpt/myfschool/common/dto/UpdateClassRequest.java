package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateClassRequest(
    @NotBlank @Size(max = 20) String name,
    @NotNull @Min(1) @Max(12) Integer gradeLevel,
    @NotNull Long academicYearId,
    @Size(max = 200) String schoolName
) {}
