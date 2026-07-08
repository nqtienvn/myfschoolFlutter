package vn.edu.fpt.myfschool.common.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FeeCategoryDto(
    Long id,
    String name,
    String description
) {}