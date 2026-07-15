package vn.edu.fpt.myfschool.common.dto;

import vn.edu.fpt.myfschool.common.enums.PeriodicReportStatus;

import java.time.LocalDateTime;
import java.util.List;

public record HomeroomReportDto(
    Long id,
    Long academicYearId,
    Long semesterId,
    Long classId,
    String className,
    Long studentId,
    String studentName,
    String studentCode,
    Long homeroomTeacherId,
    String homeroomTeacherName,
    String generalComment,
    String conduct,
    String suggestedConduct,
    PeriodicReportStatus status,
    LocalDateTime publishedAt,
    int submittedSubjects,
    int totalSubjects,
    List<String> missingSubjects,
    List<SubjectReviewDto> subjectReviews
) {}
