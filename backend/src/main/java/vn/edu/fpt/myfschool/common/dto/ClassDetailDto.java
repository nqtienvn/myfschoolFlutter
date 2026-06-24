package vn.edu.fpt.myfschool.common.dto;

import java.util.List;

public record ClassDetailDto(
    Long id,
    String name,
    Integer gradeLevel,
    String academicYear,
    String schoolName,
    List<StudentSummaryDto> students,
    List<ClassSubjectDto> subjects
) {}
