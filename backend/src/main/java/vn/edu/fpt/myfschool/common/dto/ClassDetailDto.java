package vn.edu.fpt.myfschool.common.dto;

import java.util.List;

public record ClassDetailDto(
    Long id,
    String name,
    Integer gradeLevel,
    Long academicYearId,
    String academicYearName,
    String schoolName,
    List<StudentSummaryDto> students,
    List<ClassSubjectDto> subjects
) {}
