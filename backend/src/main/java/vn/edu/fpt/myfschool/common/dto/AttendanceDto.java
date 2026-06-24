package vn.edu.fpt.myfschool.common.dto;

import vn.edu.fpt.myfschool.common.enums.AttendanceStatus;
import vn.edu.fpt.myfschool.common.enums.Shift;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record AttendanceDto(
    Long id, Long studentId, String studentName, String studentCode,
    Long classId, String className, LocalDate date, Shift shift,
    AttendanceStatus status, Long leaveRequestId, String teacherName, LocalDateTime createdAt
) {}
