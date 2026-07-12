package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.*;
import vn.edu.fpt.myfschool.common.enums.*;

public record GradeConfigItemRequest(
    @NotBlank String code, @NotBlank String displayName,
    @NotNull @Min(1) Integer weight, @NotNull @Min(1) Integer quantity,
    @NotNull GradeEntryRole entryRole, @NotNull AssessmentType assessmentType,
    Boolean requiredEntry, @NotNull @Min(0) Integer displayOrder) {}
