package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateFeeTemplateRequest(
    @NotNull Long feeCategoryId,
    @NotNull Long classId,
    @NotNull Long semesterId,
    @NotBlank @Size(max = 200) String name,
    @NotNull @DecimalMin("0.01") BigDecimal amount,
    @NotNull LocalDate dueDate
) {}