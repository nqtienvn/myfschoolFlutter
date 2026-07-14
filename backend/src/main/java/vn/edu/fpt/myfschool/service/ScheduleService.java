package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.enums.UserRole;

import java.util.*;
import java.time.LocalDate;

public interface ScheduleService {
    ClassScheduleDto getClassSchedule(Long classId, Long semesterId, LocalDate date);

    ClassScheduleDto getTeacherSchedule(Long teacherId, Long semesterId, LocalDate date);

    ClassScheduleDto getStudentSchedule(Long studentId, Long userId, Long semesterId, LocalDate date);

    ClassScheduleDto getMySchedule(Long userId, UserRole role, Long semesterId, LocalDate date);

    ScheduleDto createSchedule(ScheduleRequest request);

    List<ScheduleDto> getTimetableSlots(Long timetableId);

    void deleteSchedule(Long scheduleId);

    List<PeriodDto> getAvailablePeriods(Long classId, Long semesterId, Integer dayOfWeek, Long shiftId);

    List<AssignmentAvailabilityDto> getAssignmentAvailability(Long classId, Long semesterId);
}
