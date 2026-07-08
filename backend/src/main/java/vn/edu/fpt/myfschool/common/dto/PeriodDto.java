package vn.edu.fpt.myfschool.common.dto;

public record PeriodDto(
    Long id,
    String name,
    Integer order,
    Long shiftId,
    String shiftName
) {}
