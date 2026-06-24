package vn.edu.fpt.myfschool.common.dto;

import java.time.LocalDateTime;

public record ClassDto(
    Long id,
    String name,
    Integer gradeLevel,
    String academicYear,
    String schoolName,
    Integer studentCount,
    LocalDateTime createdAt
) {}
