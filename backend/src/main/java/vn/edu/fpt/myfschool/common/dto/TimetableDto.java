package vn.edu.fpt.myfschool.common.dto;

import vn.edu.fpt.myfschool.common.enums.TimetableStatus;

import java.time.LocalDate;

public record TimetableDto(
    Long id,
    Long classId,
    String className,
    Long semesterId,
    String semesterName,
    Integer version,
    TimetableStatus status,
    LocalDate effectiveFrom,
    LocalDate effectiveTo,
    Integer slotCount
) {}
