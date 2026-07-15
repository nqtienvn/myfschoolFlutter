package vn.edu.fpt.myfschool.common.dto;

import vn.edu.fpt.myfschool.common.enums.*;

import java.time.LocalDateTime;

public record StudentRiskFlagDto(
        Long id,
        Long academicYearId,
        Long semesterId,
        Long classId,
        String className,
        Long studentId,
        String studentName,
        String studentCode,
        RiskType riskType,
        RiskSeverity severity,
        String metricValue,
        String thresholdValue,
        String message,
        RiskStatus status,
        LocalDateTime detectedAt,
        Long acknowledgedBy,
        Long resolvedBy,
        LocalDateTime resolvedAt,
        String sourceSnapshotJson
) {}
