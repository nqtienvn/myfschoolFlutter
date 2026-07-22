package vn.edu.fpt.myfschool.common.dto;

public record AdminGradeImportResultDto(
        Long batchId,
        String itemCode,
        String itemName,
        int totalRows,
        int updatedScores
) {}
