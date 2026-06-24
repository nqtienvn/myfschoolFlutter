package vn.edu.fpt.myfschool.common.dto;

import java.math.BigDecimal;
import java.util.List;

public record SimulationResultDto(
    List<GradeDto> simulatedGrades, BigDecimal simulatedGpa,
    String simulatedAcademicAbility, String simulatedConduct, Integer simulatedRank
) {}
