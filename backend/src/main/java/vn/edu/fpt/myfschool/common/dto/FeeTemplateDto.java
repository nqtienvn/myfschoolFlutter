package vn.edu.fpt.myfschool.common.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FeeTemplateDto(
    Long id,
    Long feeCategoryId, String feeCategoryName,
    Long classId, String className,
    Long semesterId, String semesterName,
    String name, BigDecimal amount, LocalDate dueDate,
    Integer studentCount
) {}