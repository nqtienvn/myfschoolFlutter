package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.enums.Shift;

import java.util.*;

public interface ScheduleService {
    ClassScheduleDto getClassSchedule(Long classId, Long semesterId);

    ClassScheduleDto getTeacherSchedule(Long teacherId, Long semesterId);

    ScheduleDto createSchedule(ScheduleRequest request);

    void deleteSchedule(Long scheduleId);

    List<Integer> getAvailablePeriods(Long classId, Long semesterId, Integer dayOfWeek, Shift shift);
}
