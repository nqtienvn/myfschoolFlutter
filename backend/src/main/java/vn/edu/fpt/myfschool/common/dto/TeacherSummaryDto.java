package vn.edu.fpt.myfschool.common.dto;

public record TeacherSummaryDto(
    Long id,
    String name,
    String employeeCode,
    String department,
    String avatar
) {}
