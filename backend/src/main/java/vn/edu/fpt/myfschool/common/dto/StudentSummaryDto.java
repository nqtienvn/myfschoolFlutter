package vn.edu.fpt.myfschool.common.dto;

public record StudentSummaryDto(
    Long id,
    String name,
    String studentCode,
    String className,
    String avatar
) {}
