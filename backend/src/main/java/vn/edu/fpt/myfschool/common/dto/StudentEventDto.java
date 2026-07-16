package vn.edu.fpt.myfschool.common.dto;

import vn.edu.fpt.myfschool.common.enums.StudentEventStatus;
import vn.edu.fpt.myfschool.common.enums.StudentEventType;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record StudentEventDto(
        Long id,
        Long studentId,
        String studentName,
        Long academicYearId,
        Long semesterId,
        Long classId,
        String className,
        StudentEventType eventType,
        String category,
        String title,
        String description,
        LocalDate eventDate,
        StudentEventStatus status,
        Long createdBy,
        String createdByName,
        LocalDateTime submittedAt
) {}
