package vn.edu.fpt.myfschool.common.dto;

import vn.edu.fpt.myfschool.common.enums.BillStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record TuitionBillDto(
    Long id, Long studentId, String studentName, String studentCode,
    Long classId, String className, Long semesterId, String semesterName,
    Long feeTemplateId, String feeTemplateName,
    String name, BigDecimal amount, LocalDate dueDate, BillStatus status,
    LocalDateTime paidAt, List<Object> transactions, LocalDateTime createdAt
) {}
