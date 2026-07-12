package vn.edu.fpt.myfschool.common.dto;

import java.util.List;

public record GradeConfigDto(Long id, String name, Integer version, Long academicYearId, String status,
    List<GradeConfigItemDto> items) {}
