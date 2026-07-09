package vn.edu.fpt.myfschool.common.dto;

import vn.edu.fpt.myfschool.common.enums.SemesterStatus;
import java.time.LocalDate;

public record SemesterDto(
    Long id,
    String name,
    Long academicYearId,
    String academicYearName,
    Integer order,
    LocalDate startDate,
    LocalDate endDate,
    Boolean isCurrent,
    SemesterStatus status
) {}
