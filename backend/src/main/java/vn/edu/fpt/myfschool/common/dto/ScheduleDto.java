package vn.edu.fpt.myfschool.common.dto;

import vn.edu.fpt.myfschool.common.enums.Shift;

public record ScheduleDto(
    Long id, Long classId, String className, Long subjectId, String subjectName, String subjectCode,
    Long teacherId, String teacherName, Long semesterId, String semesterName,
    Integer dayOfWeek, String dayOfWeekName, Integer period, String room, Shift shift
) {}
