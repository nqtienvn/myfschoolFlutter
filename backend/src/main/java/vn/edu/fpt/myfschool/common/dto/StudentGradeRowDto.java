package vn.edu.fpt.myfschool.common.dto;

import java.math.BigDecimal;

public record StudentGradeRowDto(
    Long studentId, String studentName, String studentCode,
    BigDecimal oral, BigDecimal quiz15m, BigDecimal midTerm,
    BigDecimal finalScore, BigDecimal average
) {}
