package vn.edu.fpt.myfschool.common.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ResultSummaryDto(
        Long studentId,
        String studentName,
        String studentCode,
        Long academicYearId,
        Long semesterId,
        Long classId,
        String className,
        BigDecimal gpa,
        Integer rank,
        long violationCount,
        long absentWithLeave,
        long absentWithoutLeave,
        String suggestedAcademicAbility,
        String suggestedConduct,
        String academicAbility,
        String conduct,
        String honor,
        String status,
        LocalDateTime publishedAt
) {}
