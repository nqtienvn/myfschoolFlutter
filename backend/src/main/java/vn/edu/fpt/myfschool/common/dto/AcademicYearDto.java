package vn.edu.fpt.myfschool.common.dto;

import vn.edu.fpt.myfschool.common.enums.AcademicYearStatus;

import java.time.LocalDate;

public record AcademicYearDto(
    Long id,
    String name,
    LocalDate startDate,
    LocalDate endDate,
    AcademicYearStatus status
) {}
