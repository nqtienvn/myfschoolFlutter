package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TuitionBillRequest(
    @NotNull Long studentId, @NotNull Long classId, @NotNull Long semesterId,
    @NotBlank @Size(max = 200) String name,
    @NotNull @DecimalMin("0") BigDecimal amount, @NotNull LocalDate dueDate
) {}
