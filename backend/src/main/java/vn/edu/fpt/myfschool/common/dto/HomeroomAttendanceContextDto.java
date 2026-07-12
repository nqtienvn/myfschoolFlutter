package vn.edu.fpt.myfschool.common.dto;

import vn.edu.fpt.myfschool.common.enums.Shift;

import java.util.List;

public record HomeroomAttendanceContextDto(
    Long classId,
    String className,
    Long academicYearId,
    String academicYearName,
    List<Shift> shifts
) {}
