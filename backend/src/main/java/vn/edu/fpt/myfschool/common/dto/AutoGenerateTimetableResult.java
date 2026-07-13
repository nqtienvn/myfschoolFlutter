package vn.edu.fpt.myfschool.common.dto;

import java.util.List;

public record AutoGenerateTimetableResult(
    Long academicYearId,
    Long semesterId,
    int classCount,
    int timetableCount,
    int slotCount,
    List<TimetableDto> timetables
) {}
