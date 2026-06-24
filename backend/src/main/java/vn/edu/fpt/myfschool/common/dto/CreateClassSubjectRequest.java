package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateClassSubjectRequest(
    @NotNull Long classId,
    @NotNull Long subjectId,
    @NotNull Long teacherId,
    Boolean isHomeroom,
    @NotBlank @Size(max = 9) String academicYear,
    Long semesterId
) {}
