package vn.edu.fpt.myfschool.common.dto;

public record GradeItemDto(
    Long id,
    String code,
    String name,
    Integer weight,
    Integer maxScore,
    Integer order,
    vn.edu.fpt.myfschool.common.enums.GradeEntryRole entryRole,
    vn.edu.fpt.myfschool.common.enums.AssessmentType assessmentType,
    Boolean requiredEntry
) {}
