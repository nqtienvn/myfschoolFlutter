package vn.edu.fpt.myfschool.common.dto;

import vn.edu.fpt.myfschool.common.enums.AssessmentType;

public record GradeComponentColumnDto(
        String code,
        String name,
        Integer weight,
        AssessmentType assessmentType
) {}
