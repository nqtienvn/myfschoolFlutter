package vn.edu.fpt.myfschool.common.dto;

import java.util.List;

public record AttendanceSessionDto(
    Long id,
    Long classId, String className,
    Long teacherId, String teacherName,
    java.time.LocalDate date, vn.edu.fpt.myfschool.common.enums.Shift shift,
    Long scheduleId,
    Integer total, Integer present, Integer late, Integer absent,
    Boolean isClosed,
    List<AttendanceDetailDto> details
) {}