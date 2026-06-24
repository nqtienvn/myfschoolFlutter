package vn.edu.fpt.myfschool.common.dto;

import java.time.LocalDate;

public record SemesterDto(
    Long id,
    String name,
    String academicYear,
    LocalDate startDate,
    LocalDate endDate,
    Boolean isCurrent
) {}
