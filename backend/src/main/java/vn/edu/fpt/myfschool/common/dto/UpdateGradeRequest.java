package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record UpdateGradeRequest(
    @NotNull Long studentId, @NotNull Long subjectId, @NotNull Long semesterId,
    @DecimalMin("0") @DecimalMax("10") BigDecimal oral,
    @DecimalMin("0") @DecimalMax("10") BigDecimal quiz15m,
    @DecimalMin("0") @DecimalMax("10") BigDecimal midTerm,
    @DecimalMin("0") @DecimalMax("10") BigDecimal finalScore
) {}
