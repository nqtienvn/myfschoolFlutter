package vn.edu.fpt.myfschool.common.dto;

public record ClassSubjectDto(
    Long id,
    SubjectDto subject,
    TeacherSummaryDto teacher,
    Boolean isHomeroom
) {}
