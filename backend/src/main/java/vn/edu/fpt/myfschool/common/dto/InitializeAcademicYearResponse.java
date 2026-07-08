package vn.edu.fpt.myfschool.common.dto;

import java.util.List;

public record InitializeAcademicYearResponse(
    Long newAcademicYearId,
    int classesCreated,
    int teachingAssignmentsCopied,
    int feeTemplatesCopied,
    List<String> warnings
) {}