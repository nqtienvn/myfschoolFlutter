package vn.edu.fpt.myfschool.common.dto;

import vn.edu.fpt.myfschool.common.enums.ParentContactType;

import java.time.LocalDateTime;

public record ParentContactLogDto(
        Long id,
        Long studentId,
        String studentName,
        Long academicYearId,
        Long semesterId,
        Long classId,
        String className,
        ParentContactType contactType,
        String subject,
        String summary,
        String result,
        LocalDateTime contactedAt,
        LocalDateTime nextActionAt,
        Long createdBy,
        String createdByName
) {}
