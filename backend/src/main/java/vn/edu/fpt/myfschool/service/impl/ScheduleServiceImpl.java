package vn.edu.fpt.myfschool.service.impl;

import vn.edu.fpt.myfschool.controller.entity.*;
import vn.edu.fpt.myfschool.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.enums.Shift;
import vn.edu.fpt.myfschool.common.exception.ConflictException;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.repository.*;

import java.util.*;
import java.util.stream.Collectors;

@Service("scheduleService")
@RequiredArgsConstructor
@Transactional
public class ScheduleServiceImpl implements ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final TeachingAssignmentRepository teachingAssignmentRepository;
    private final ClassRepository classRepository;
    private final TeacherRepository teacherRepository;
    private final SemesterRepository semesterRepository;

    @Transactional(readOnly = true)
    @Override
    public ClassScheduleDto getClassSchedule(Long classId, Long semesterId) {
        SchoolClass cls = classRepository.findById(classId)
            .orElseThrow(() -> new ResourceNotFoundException("Class", "id", classId));
        Semester sem = semesterRepository.findById(semesterId)
            .orElseThrow(() -> new ResourceNotFoundException("Semester", "id", semesterId));

        List<Schedule> schedules = scheduleRepository.findByClassIdAndSemesterId(classId, semesterId);
        Map<Integer, List<Schedule>> byDay = schedules.stream()
            .collect(Collectors.groupingBy(Schedule::getDayOfWeek));

        List<DayScheduleDto> days = new ArrayList<>();
        for (int d = 1; d <= 7; d++) {
            List<Schedule> daySchedules = byDay.getOrDefault(d, List.of());
            List<ScheduleDto> morning = daySchedules.stream()
                .filter(s -> s.getShift() == Shift.MORNING).map(this::toDto)
                .collect(Collectors.toList());
            List<ScheduleDto> afternoon = daySchedules.stream()
                .filter(s -> s.getShift() == Shift.AFTERNOON).map(this::toDto)
                .collect(Collectors.toList());
            days.add(new DayScheduleDto(d, getDayName(d), morning, afternoon));
        }

        return new ClassScheduleDto(classId, cls.getName(), semesterId, sem.getName(), days);
    }

    @Transactional(readOnly = true)
    @Override
    public ClassScheduleDto getTeacherSchedule(Long teacherId, Long semesterId) {
        Teacher teacher = teacherRepository.findById(teacherId)
            .orElseThrow(() -> new ResourceNotFoundException("Teacher", "id", teacherId));
        Semester sem = semesterRepository.findById(semesterId)
            .orElseThrow(() -> new ResourceNotFoundException("Semester", "id", semesterId));

        List<Schedule> schedules = scheduleRepository.findByTeacherIdAndSemesterId(teacherId, semesterId);
        Map<Integer, List<Schedule>> byDay = schedules.stream()
            .collect(Collectors.groupingBy(Schedule::getDayOfWeek));

        List<DayScheduleDto> days = new ArrayList<>();
        for (int d = 1; d <= 7; d++) {
            List<Schedule> daySchedules = byDay.getOrDefault(d, List.of());
            List<ScheduleDto> morning = daySchedules.stream()
                .filter(s -> s.getShift() == Shift.MORNING).map(this::toDto)
                .collect(Collectors.toList());
            List<ScheduleDto> afternoon = daySchedules.stream()
                .filter(s -> s.getShift() == Shift.AFTERNOON).map(this::toDto)
                .collect(Collectors.toList());
            days.add(new DayScheduleDto(d, getDayName(d), morning, afternoon));
        }

        return new ClassScheduleDto(teacherId, teacher.getUser().getName(), semesterId, sem.getName(), days);
    }

    @Override
    public ScheduleDto createSchedule(ScheduleRequest request) {
        TeachingAssignment ta = teachingAssignmentRepository.findById(request.assignmentId())
            .orElseThrow(() -> new ResourceNotFoundException("TeachingAssignment", "id", request.assignmentId()));

        Long classId = ta.getCls().getId();
        Long semesterId = ta.getSemester().getId();

        if (scheduleRepository.findByAssignmentIdAndDayOfWeekAndPeriod(
                request.assignmentId(), request.dayOfWeek(), request.period()).isPresent()) {
            throw new ConflictException("Phan cong da co tai khung gio nay");
        }

        // Check teacher conflict (same teacher, different assignment, same day+period+semester)
        List<TeachingAssignment> teacherAssignments = teachingAssignmentRepository
            .findByTeacherIdAndSemesterIdAndStatus(ta.getTeacher().getId(), semesterId,
                vn.edu.fpt.myfschool.common.enums.AssignmentStatus.ACTIVE);
        for (TeachingAssignment otherTa : teacherAssignments) {
            if (!otherTa.getId().equals(request.assignmentId())) {
                if (scheduleRepository.findByAssignmentIdAndDayOfWeekAndPeriod(
                        otherTa.getId(), request.dayOfWeek(), request.period()).isPresent()) {
                    throw new ConflictException("Giao vien da co tiet day tai khung gio nay");
                }
            }
        }

        Schedule sch = new Schedule();
        sch.setAssignment(ta);
        sch.setDayOfWeek(request.dayOfWeek());
        sch.setPeriod(request.period());
        sch.setRoom(request.room());
        sch.setShift(request.shift());
        sch = scheduleRepository.save(sch);
        return toDto(sch);
    }

    @Override
    public void deleteSchedule(Long scheduleId) {
        scheduleRepository.deleteById(scheduleId);
    }

    @Override
    public List<Integer> getAvailablePeriods(Long classId, Long semesterId, Integer dayOfWeek, Shift shift) {
        List<Schedule> existing = scheduleRepository.findByClassIdAndSemesterId(classId, semesterId);
        Set<Integer> occupied = existing.stream()
            .filter(s -> s.getDayOfWeek().equals(dayOfWeek) && s.getShift() == shift)
            .map(Schedule::getPeriod).collect(Collectors.toSet());
        List<Integer> all = shift == Shift.MORNING ? List.of(1,2,3,4,5) : List.of(6,7,8,9,10);
        return all.stream().filter(p -> !occupied.contains(p)).collect(Collectors.toList());
    }

    private ScheduleDto toDto(Schedule s) {
        TeachingAssignment ta = s.getAssignment();
        return new ScheduleDto(
            s.getId(),
            ta.getId(),
            ta.getCls().getId(), ta.getCls().getName(),
            ta.getSubject().getId(), ta.getSubject().getName(), ta.getSubject().getCode(),
            ta.getTeacher().getId(), ta.getTeacher().getUser().getName(),
            ta.getSemester().getId(), ta.getSemester().getName(),
            s.getDayOfWeek(), getDayName(s.getDayOfWeek()),
            s.getPeriod(), s.getRoom(), s.getShift()
        );
    }

    private String getDayName(int day) {
        return switch (day) {
            case 1 -> "Chu nhat"; case 2 -> "Thu 2"; case 3 -> "Thu 3";
            case 4 -> "Thu 4"; case 5 -> "Thu 5"; case 6 -> "Thu 6"; case 7 -> "Thu 7";
            default -> "";
        };
    }
}
