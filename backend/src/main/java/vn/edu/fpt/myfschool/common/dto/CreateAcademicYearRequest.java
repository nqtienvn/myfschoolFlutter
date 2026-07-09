package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateAcademicYearRequest(
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate
) {
}
