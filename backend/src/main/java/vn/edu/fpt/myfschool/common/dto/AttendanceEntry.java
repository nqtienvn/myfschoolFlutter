package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.NotNull;
import vn.edu.fpt.myfschool.common.enums.AttendanceStatus;

public record AttendanceEntry(@NotNull Long studentId, @NotNull AttendanceStatus status) {}
