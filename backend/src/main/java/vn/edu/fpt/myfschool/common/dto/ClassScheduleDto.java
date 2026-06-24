package vn.edu.fpt.myfschool.common.dto;

import java.util.List;

public record ClassScheduleDto(Long classId, String className, Long semesterId, String semesterName, List<DayScheduleDto> days) {}
