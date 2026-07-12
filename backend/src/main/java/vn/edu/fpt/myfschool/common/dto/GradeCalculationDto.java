package vn.edu.fpt.myfschool.common.dto;
import java.math.BigDecimal;
public record GradeCalculationDto(Long studentId,String studentName,BigDecimal average) {}
