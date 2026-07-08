package vn.edu.fpt.myfschool.common.dto;

public record SchoolDto(
    Long id,
    String name,
    String code,
    String address,
    String phone,
    String schoolName
) {}
