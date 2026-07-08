package vn.edu.fpt.myfschool.common.dto;

public record GradeLevelDto(
    Long id,
    String name,
    String code,
    Integer order,
    String description
) {}
