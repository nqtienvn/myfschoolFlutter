package vn.edu.fpt.myfschool.common.dto;

public record GradeItemDto(
    Long id,
    String name,
    Integer weight,
    Integer maxScore,
    Integer order
) {}