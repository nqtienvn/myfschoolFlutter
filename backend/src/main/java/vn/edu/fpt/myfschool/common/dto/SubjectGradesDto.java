package vn.edu.fpt.myfschool.common.dto;

import java.util.List;

public record SubjectGradesDto(
    Long subjectId, String subjectName, String subjectCode,
    Long semesterId, String semesterName, Long classId, String className,
    List<StudentGradeRowDto> studentGrades
) {}
