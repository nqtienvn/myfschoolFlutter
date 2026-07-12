package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record CreateGradeConfigTemplateRequest(@NotBlank String name, @NotEmpty List<@Valid GradeConfigItemRequest> items) {}
