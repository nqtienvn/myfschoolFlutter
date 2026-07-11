package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.enums.Shift;

import java.util.*;
import java.time.LocalDate;

public interface ScheduleService {
    ClassScheduleDto getClassSchedule(Long classId, Long semesterId, LocalDate date);

    ClassScheduleDto getTeacherSchedule(Long teacherId, Long semesterId, LocalDate date);

    ScheduleDto createSchedule(ScheduleRequest request);

    List<ScheduleDto> getTimetableSlots(Long timetableId);

    void deleteSchedule(Long scheduleId);

    List<Integer> getAvailablePeriods(Long classId, Long semesterId, Integer dayOfWeek, Shift shift);
}
