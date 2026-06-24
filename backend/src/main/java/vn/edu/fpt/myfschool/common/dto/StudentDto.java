package vn.edu.fpt.myfschool.common.dto;

import java.time.LocalDate;

public record StudentDto(
    Long id,
    String studentCode,
    String className,
    Long classId,
    LocalDate dateOfBirth
) {}
