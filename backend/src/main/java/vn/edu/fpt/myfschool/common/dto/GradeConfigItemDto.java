package vn.edu.fpt.myfschool.common.dto;

import vn.edu.fpt.myfschool.common.enums.*;

public record GradeConfigItemDto(Long id, String code, String displayName, Integer weight, Integer quantity,
    GradeEntryRole entryRole, AssessmentType assessmentType, Boolean requiredEntry, Integer displayOrder) {}
