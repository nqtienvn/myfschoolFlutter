package vn.edu.fpt.myfschool.common.dto;

import java.util.List;

public record GradeBookDto(
    Long id,
    Long classId, String className,
    Long subjectId, String subjectName,
    Long semesterId, String semesterName,
    Boolean isFinalized,
    List<GradeItemDto> items
) {}