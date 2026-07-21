package vn.edu.fpt.myfschool.common.dto;

public record StudentEnrollmentResultDto(
    Long studentId,
    String studentCode,
    String studentUsername,
    boolean studentCredentialsEmailed,
    Long classId,
    String className,
    Long parentId,
    String parentUsername,
    boolean parentCredentialsEmailed,
    boolean parentReused
) {}
