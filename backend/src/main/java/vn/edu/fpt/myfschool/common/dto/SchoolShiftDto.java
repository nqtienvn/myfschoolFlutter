package vn.edu.fpt.myfschool.common.dto;

public record SchoolShiftDto(
    Long id,
    String name,
    String code,
    Integer order
) {}
