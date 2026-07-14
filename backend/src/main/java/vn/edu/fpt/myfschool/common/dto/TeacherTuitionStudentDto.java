package vn.edu.fpt.myfschool.common.dto;

import java.math.BigDecimal;
import java.util.List;

public record TeacherTuitionStudentDto(
    Long studentId,
    String studentName,
    String studentCode,
    String paymentState,
    BigDecimal outstandingAmount,
    List<TuitionBillDto> bills
) {}
