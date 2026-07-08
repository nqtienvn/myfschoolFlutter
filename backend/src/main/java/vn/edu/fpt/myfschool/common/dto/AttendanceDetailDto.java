package vn.edu.fpt.myfschool.common.dto;

public record AttendanceDetailDto(
    Long id, Long sessionId,
    Long studentId, String studentName, String studentCode,
    String status, String note
) {}