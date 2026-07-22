package vn.edu.fpt.myfschool.common.dto;

import vn.edu.fpt.myfschool.common.enums.AssessmentType;

public record AdminGradeImportItemDto(
        Long configItemId,
        Integer occurrence,
        String itemCode,
        String displayName,
        AssessmentType assessmentType
) {}
