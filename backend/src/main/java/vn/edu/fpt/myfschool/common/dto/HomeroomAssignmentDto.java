package vn.edu.fpt.myfschool.common.dto;

import java.time.LocalDate;

public record HomeroomAssignmentDto(
    Long id,
    Long classId, String className,
    Long teacherId, String teacherName,
    Long academicYearId, String academicYearName,
    LocalDate effectiveFrom, LocalDate effectiveTo
) {}