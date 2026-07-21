package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminAnnouncementRequest(
    @NotBlank @Size(max = 500) String title,
    @NotBlank @Size(max = 10000) String body,
    @NotNull Long academicYearId
) {}
