package vn.edu.fpt.myfschool.common.dto;

import java.math.BigDecimal;

public record GradeComponentCellDto(
        BigDecimal score,
        String comment,
        Boolean isGraded
) {}
