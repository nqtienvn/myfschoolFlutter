package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.NotNull;
import vn.edu.fpt.myfschool.common.enums.AcademicYearStatus;

public record UpdateAcademicYearStatusRequest(@NotNull AcademicYearStatus status) {}
