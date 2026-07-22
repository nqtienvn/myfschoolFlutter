package vn.edu.fpt.myfschool.common.dto;

import java.math.BigDecimal;
import java.util.Map;

public record GradeComponentOverviewRowDto(
        Long classId,
        String className,
        Long subjectId,
        String subjectName,
        Long studentId,
        String studentName,
        String studentCode,
        Map<String, GradeComponentCellDto> values,
        BigDecimal average
) {}
