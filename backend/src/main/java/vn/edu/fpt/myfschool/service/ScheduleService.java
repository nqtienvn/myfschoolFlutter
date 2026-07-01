package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.enums.Shift;
import vn.edu.fpt.myfschool.common.exception.BadRequestException;
import vn.edu.fpt.myfschool.common.exception.ConflictException;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.entity.*;
import vn.edu.fpt.myfschool.repository.*;
import java.util.*;
import java.util.stream.Collectors;

public interface ScheduleService {
    ClassScheduleDto getClassSchedule(Long classId, Long semesterId);

    ClassScheduleDto getTeacherSchedule(Long teacherId, Long semesterId);

    ScheduleDto createSchedule(ScheduleRequest request);

    void deleteSchedule(Long scheduleId);

    List<Integer> getAvailablePeriods(Long classId, Long semesterId, Integer dayOfWeek, Shift shift);
}
