package vn.edu.fpt.myfschool.common.dto;

import java.util.List;

public record GradeImportResultDto(
        int totalRows,
        int importedRows,
        int updatedScores,
        int zeroFilledScores,
        List<String> errors
) {}
