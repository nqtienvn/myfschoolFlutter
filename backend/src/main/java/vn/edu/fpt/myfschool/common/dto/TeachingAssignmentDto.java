package vn.edu.fpt.myfschool.common.dto;

import vn.edu.fpt.myfschool.common.enums.AssignmentStatus;
import java.time.LocalDate;

public record TeachingAssignmentDto(
    Long id,
    Long classId, String className, Integer gradeLevel,
    Long subjectId, String subjectName, String subjectCode,
    Long teacherId, String teacherName, String teacherCode,
    Long semesterId, String semesterName,
    LocalDate effectiveFrom, LocalDate effectiveTo,
    AssignmentStatus status
) {}