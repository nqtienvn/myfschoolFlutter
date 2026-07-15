package vn.edu.fpt.myfschool.common.dto;

public record ReviewAssignmentDto(
    Long academicYearId,
    Long classId,
    String className,
    Long subjectId,
    String subjectName
) {}
