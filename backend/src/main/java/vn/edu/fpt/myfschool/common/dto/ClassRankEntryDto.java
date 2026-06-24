package vn.edu.fpt.myfschool.common.dto;

import java.math.BigDecimal;

public record ClassRankEntryDto(
    Integer rank, Long studentId, String studentName, String studentCode,
    BigDecimal gpa, String academicAbility, String conduct
) {}
