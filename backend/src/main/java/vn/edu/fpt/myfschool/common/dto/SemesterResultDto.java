package vn.edu.fpt.myfschool.common.dto;

import java.math.BigDecimal;

public record SemesterResultDto(
    Long id, Long studentId, String studentName, Long semesterId, String semesterName,
    Long classId, String className, BigDecimal gpa, Integer rank,
    String honor, String conduct, String academicAbility
) {}
