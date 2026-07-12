package vn.edu.fpt.myfschool.common.dto;

import java.util.List;

public record AvailableAcademicYearDto(
    Long id,
    String name,
    List<SemesterDto> semesters
) {}
