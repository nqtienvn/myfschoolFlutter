package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import jakarta.validation.Valid;

public record CreateAcademicYearRequest(
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        Long gradeConfigTemplateId,
        List<@Valid GradeConfigItemRequest> gradeConfigItems
) {
}
