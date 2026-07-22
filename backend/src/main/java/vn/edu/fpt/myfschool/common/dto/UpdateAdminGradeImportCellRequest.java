package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateAdminGradeImportCellRequest(
        @NotNull Long subjectId,
        String value
) {}
