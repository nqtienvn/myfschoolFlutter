package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.*;
import vn.edu.fpt.myfschool.common.enums.StudentEventType;

import java.time.LocalDate;

public record SaveStudentEventRequest(
        @NotNull Long academicYearId,
        @NotNull Long semesterId,
        @NotNull Long classId,
        @NotNull StudentEventType eventType,
        @Size(max = 100) String category,
        @NotBlank @Size(max = 200) String title,
        @Size(max = 2000) String description,
        @NotNull LocalDate eventDate
) {}
