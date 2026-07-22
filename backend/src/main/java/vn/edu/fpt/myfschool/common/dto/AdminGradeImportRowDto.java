package vn.edu.fpt.myfschool.common.dto;

import java.util.List;

public record AdminGradeImportRowDto(
        Long studentId,
        String studentCode,
        String studentName,
        Long classId,
        String className,
        Integer sourceOrder,
        List<AdminGradeImportCellDto> cells
) {}
