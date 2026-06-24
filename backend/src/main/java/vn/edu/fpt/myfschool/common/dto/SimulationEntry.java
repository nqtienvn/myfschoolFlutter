package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record SimulationEntry(
    @NotNull Long subjectId, BigDecimal oral, BigDecimal quiz15m,
    BigDecimal midTerm, BigDecimal finalScore, String conduct
) {}
