package vn.edu.fpt.myfschool.common.dto;

import java.math.BigDecimal;

public record UpdateScoreEntry(
    Long studentId, BigDecimal score, String note,
    Boolean isGraded, Boolean isCommentBased, String comment
) {}