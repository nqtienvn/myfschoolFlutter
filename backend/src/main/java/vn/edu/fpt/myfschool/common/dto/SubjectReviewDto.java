package vn.edu.fpt.myfschool.common.dto;

import vn.edu.fpt.myfschool.common.enums.SubjectReviewStatus;

import java.time.LocalDateTime;

public record SubjectReviewDto(
    Long id,
    Long academicYearId,
    Long semesterId,
    Long classId,
    String className,
    Long studentId,
    String studentName,
    String studentCode,
    Long subjectId,
    String subjectName,
    Long subjectTeacherId,
    String subjectTeacherName,
    String comment,
    String strengths,
    String improvements,
    SubjectReviewStatus status,
    String returnReason,
    LocalDateTime submittedAt,
    LocalDateTime updatedAt
) {}
