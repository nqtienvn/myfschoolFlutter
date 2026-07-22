package vn.edu.fpt.myfschool.common.dto;

public record AdminGradeImportContextDto(
        Long academicYearId,
        String academicYearName,
        Long semesterId,
        String semesterName
) {}
