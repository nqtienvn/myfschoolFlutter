package vn.edu.fpt.myfschool.common.dto;

import java.time.LocalDateTime;

public record AdminGradeImportBatchDto(
        Long id,
        String itemCode,
        String itemName,
        String fileName,
        int totalRows,
        int updatedScores,
        LocalDateTime importedAt
) {}
