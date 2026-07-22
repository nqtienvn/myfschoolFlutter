package vn.edu.fpt.myfschool.common.dto;

import java.util.List;
import vn.edu.fpt.myfschool.common.enums.AssessmentType;

public record AdminGradeImportTableDto(
        Long batchId,
        String itemCode,
        String itemName,
        AssessmentType assessmentType,
        List<AdminGradeImportSubjectDto> subjects,
        List<AdminGradeImportRowDto> rows
) {}
