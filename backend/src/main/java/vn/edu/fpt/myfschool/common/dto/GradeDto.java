package vn.edu.fpt.myfschool.common.dto;

import java.math.BigDecimal;

public record GradeDto(
    Long id, Long subjectId, String subjectName, String subjectCode,
    BigDecimal oral, BigDecimal quiz15m, BigDecimal midTerm,
    BigDecimal finalScore, BigDecimal average
) {}
