package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import vn.edu.fpt.myfschool.common.enums.AcademicYearStatus;

import java.time.LocalDate;

public record CreateAcademicYearRequest(
    @NotBlank @Size(max = 20) String name,
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate,
    AcademicYearStatus status
) {}
