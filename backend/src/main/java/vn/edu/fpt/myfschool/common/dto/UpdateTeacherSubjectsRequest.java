package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record UpdateTeacherSubjectsRequest(
        @NotEmpty List<Long> subjectIds) {
}
