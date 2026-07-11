package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record PublishTimetableRequest(@NotNull LocalDate effectiveFrom) {}
