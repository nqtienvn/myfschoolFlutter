package vn.edu.fpt.myfschool.common.dto;

import vn.edu.fpt.myfschool.common.enums.AcademicYearStatus;

import java.util.List;

public record AvailableAcademicYearDto(
    Long id,
    String name,
    AcademicYearStatus status,
    List<SemesterDto> semesters
) {}
