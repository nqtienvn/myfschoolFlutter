package vn.edu.fpt.myfschool.common.dto;

import java.math.BigDecimal;

public record AdminGradeImportCellDto(
        Long subjectId,
        BigDecimal score,
        String comment,
        Boolean isGraded
) {}
