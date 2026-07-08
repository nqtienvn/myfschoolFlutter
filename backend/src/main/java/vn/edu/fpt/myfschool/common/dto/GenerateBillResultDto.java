package vn.edu.fpt.myfschool.common.dto;

public record GenerateBillResultDto(
    Long feeTemplateId,
    int totalStudents,
    int created,
    int skipped
) {}