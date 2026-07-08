package vn.edu.fpt.myfschool.common.dto;

import java.math.BigDecimal;

public record StudentScoreDto(
    Long id,
    Long studentId, String studentName, String studentCode,
    Long gradeItemId,
    BigDecimal score,
    Boolean isGraded,
    String note,
    Boolean isCommentBased,
    String comment,
    BigDecimal average
) {}