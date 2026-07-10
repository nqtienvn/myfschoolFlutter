package vn.edu.fpt.myfschool.common.dto;

public record StudentEnrollmentResultDto(Long studentId, String studentCode, Long classId, String className, Long parentId, boolean parentReused) {}
