package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminAnnouncementRequest(
    @NotBlank String title,
    @NotBlank String body,
    @NotNull Long academicYearId
) {}
