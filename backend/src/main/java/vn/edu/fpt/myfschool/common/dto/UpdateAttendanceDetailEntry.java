package vn.edu.fpt.myfschool.common.dto;

public record UpdateAttendanceDetailEntry(
    Long studentId, String status, String note
) {}