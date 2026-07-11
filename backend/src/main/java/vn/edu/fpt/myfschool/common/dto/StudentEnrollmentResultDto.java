package vn.edu.fpt.myfschool.common.dto;

public record StudentEnrollmentResultDto(
    Long studentId,
    String studentCode,
    String studentUsername,
    String studentInitialPassword,
    Long classId,
    String className,
    Long parentId,
    String parentUsername,
    String parentInitialPassword,
    boolean parentReused
) {}
