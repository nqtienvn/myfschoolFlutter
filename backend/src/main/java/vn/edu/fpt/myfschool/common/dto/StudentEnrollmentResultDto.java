package vn.edu.fpt.myfschool.common.dto;

public record StudentEnrollmentResultDto(
    Long studentId,
    String studentCode,
    String studentUsername,
    Long classId,
    String className,
    Long parentId,
    String parentUsername,
    boolean parentReused
) {}
