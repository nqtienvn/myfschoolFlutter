package vn.edu.fpt.myfschool.common.dto;

import java.math.BigDecimal;

public record AcademicYearArchiveStatsDto(
    long classesCount,
    long studentsCount,
    long teachersCount,
    long parentsCount,
    long subjectsCount,
    long announcementsCount,
    long messagesCount,
    long chatsCount,
    long attendanceCount,
    long gradesCount,
    BigDecimal tuitionCollected
) {}
