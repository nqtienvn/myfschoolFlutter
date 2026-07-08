package vn.edu.fpt.myfschool.common.dto;

import java.util.List;

public record StudentSemesterGradesDto(
    Long studentId, String studentName, String studentCode,
    Long semesterId, String semesterName, String academicYear,
    List<GradeDto> grades, SemesterResultDto summary
) {}
