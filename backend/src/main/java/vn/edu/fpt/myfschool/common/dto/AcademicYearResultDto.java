package vn.edu.fpt.myfschool.common.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AcademicYearResultDto(
        Long studentId,
        String studentName,
        String studentCode,
        Long academicYearId,
        Long classId,
        String className,
        BigDecimal semester1Average,
        String semester1AcademicAbility,
        String semester1Conduct,
        BigDecimal semester2Average,
        String semester2AcademicAbility,
        String semester2Conduct,
        BigDecimal annualAverage,
        Integer rank,
        String academicAbility,
        String conduct,
        String honor,
        String status,
        LocalDateTime publishedAt
) {}
