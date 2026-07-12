package vn.edu.fpt.myfschool.common.dto;

import vn.edu.fpt.myfschool.common.enums.LeaveShift;
import vn.edu.fpt.myfschool.common.enums.LeaveStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record LeaveRequestDto(
    Long id, Long studentId, String studentName, String studentCode,
    Long parentId, String parentName, Long classId, String className,
    Long academicYearId, String academicYearName,
    LocalDate dateFrom, LocalDate dateTo, LeaveShift shift, String reason,
    LeaveStatus status, String response, Long approvedById, String approvedByName,
    LocalDateTime approvedAt, List<AttachmentDto> attachments, LocalDateTime createdAt
) {}
