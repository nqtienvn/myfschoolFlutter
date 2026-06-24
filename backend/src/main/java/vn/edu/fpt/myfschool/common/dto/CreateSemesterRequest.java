package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateSemesterRequest(
    @NotBlank @Size(max = 50) String name,
    @NotBlank @Size(max = 9) String academicYear,
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate,
    Boolean isCurrent
) {}
