package vn.edu.fpt.myfschool.common.dto;

import java.util.List;

public record DayScheduleDto(Integer dayOfWeek, String dayOfWeekName, List<ScheduleDto> morningSlots, List<ScheduleDto> afternoonSlots) {}
