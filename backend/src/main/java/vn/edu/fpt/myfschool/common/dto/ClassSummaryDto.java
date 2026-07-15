package vn.edu.fpt.myfschool.common.dto;

import java.math.BigDecimal;
import java.util.Map;

public record ClassSummaryDto(
        Long academicYearId,
        Long semesterId,
        Long classId,
        String className,
        Integer gradeLevel,
        long studentCount,
        BigDecimal attendanceRate,
        long openRiskCount,
        BigDecimal averageGpa,
        Map<String, Long> academicAbilityDistribution,
        Map<String, Long> conductDistribution,
        long submittedSubjectReviews,
        long expectedSubjectReviews,
        BigDecimal reviewProgressRate,
        long parentContactCount,
        long meetingCount,
        BigDecimal meetingParticipationRate,
        long rewardCount,
        long violationCount
) {}
