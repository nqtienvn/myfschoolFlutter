package vn.edu.fpt.myfschool.service.impl;

import vn.edu.fpt.myfschool.controller.entity.Schedule;
import vn.edu.fpt.myfschool.controller.entity.SchoolClass;
import vn.edu.fpt.myfschool.controller.entity.Semester;
import vn.edu.fpt.myfschool.controller.entity.Subject;
import vn.edu.fpt.myfschool.controller.entity.Teacher;
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
    private final ClassRepository classRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;
    private final SemesterRepository semesterRepository;

    @Transactional(readOnly = true)
    @Override
    public ClassScheduleDto getClassSchedule(Long classId, Long semesterId) {
        SchoolClass cls = classRepository.findById(classId)
            .orElseThrow(() -> new ResourceNotFoundException("Class", "id", classId));
        Semester sem = semesterRepository.findById(semesterId)
            .orElseThrow(() -> new ResourceNotFoundException("Semester", "id", semesterId));

        List<Schedule> schedules = scheduleRepository.findByClassIdAndSemesterIdOrderByDayOfWeekAscPeriodAsc(classId, semesterId);
        Map<Integer, List<Schedule>> byDay = schedules.stream().collect(Collectors.groupingBy(Schedule::getDayOfWeek));

        List<DayScheduleDto> days = new ArrayList<>();
        for (int d = 1; d <= 7; d++) {
            List<Schedule> daySchedules = byDay.getOrDefault(d, List.of());
            List<ScheduleDto> morning = daySchedules.stream().filter(s -> s.getShift() == Shift.MORNING).map(this::toDto).collect(Collectors.toList());
            List<ScheduleDto> afternoon = daySchedules.stream().filter(s -> s.getShift() == Shift.AFTERNOON).map(this::toDto).collect(Collectors.toList());
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

        List<Schedule> schedules = scheduleRepository.findByTeacherIdAndSemesterIdOrderByDayOfWeekAscPeriodAsc(teacherId, semesterId);
        Map<Integer, List<Schedule>> byDay = schedules.stream().collect(Collectors.groupingBy(Schedule::getDayOfWeek));

        List<DayScheduleDto> days = new ArrayList<>();
        for (int d = 1; d <= 7; d++) {
            List<Schedule> daySchedules = byDay.getOrDefault(d, List.of());
            List<ScheduleDto> morning = daySchedules.stream().filter(s -> s.getShift() == Shift.MORNING).map(this::toDto).collect(Collectors.toList());
            List<ScheduleDto> afternoon = daySchedules.stream().filter(s -> s.getShift() == Shift.AFTERNOON).map(this::toDto).collect(Collectors.toList());
            days.add(new DayScheduleDto(d, getDayName(d), morning, afternoon));
        }

        return new ClassScheduleDto(teacherId, teacher.getUser().getName(), semesterId, sem.getName(), days);
    }

    @Override
    public ScheduleDto createSchedule(ScheduleRequest request) {
        SchoolClass cls = classRepository.findById(request.classId()).orElseThrow();
        Subject subject = subjectRepository.findById(request.subjectId()).orElseThrow();
        Teacher teacher = teacherRepository.findById(request.teacherId()).orElseThrow();
        Semester sem = semesterRepository.findById(request.semesterId()).orElseThrow();

        if (scheduleRepository.findByClassIdAndSemesterIdAndDayOfWeekAndPeriod(
                request.classId(), request.semesterId(), request.dayOfWeek(), request.period()).isPresent()) {
            throw new ConflictException("Lớp đã có tiết học tại khung giờ này");
        }

        if (scheduleRepository.findTeacherConflict(request.teacherId(), request.semesterId(),
                request.dayOfWeek(), request.period()).isPresent()) {
            throw new ConflictException("Giáo viên đã có tiết dạy tại khung giờ này");
        }

        Schedule sch = new Schedule();
        sch.setCls(cls);
        sch.setSubject(subject);
        sch.setTeacher(teacher);
        sch.setSemester(sem);
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
        List<Schedule> existing = scheduleRepository.findByClassIdAndSemesterIdOrderByDayOfWeekAscPeriodAsc(classId, semesterId);
        Set<Integer> occupied = existing.stream()
            .filter(s -> s.getDayOfWeek().equals(dayOfWeek) && s.getShift() == shift)
            .map(Schedule::getPeriod).collect(Collectors.toSet());
        List<Integer> all = shift == Shift.MORNING ? List.of(1,2,3,4,5) : List.of(6,7,8,9,10);
        return all.stream().filter(p -> !occupied.contains(p)).collect(Collectors.toList());
    }

    private ScheduleDto toDto(Schedule s) {
        return new ScheduleDto(s.getId(), s.getCls().getId(), s.getCls().getName(),
            s.getSubject().getId(), s.getSubject().getName(), s.getSubject().getCode(),
            s.getTeacher().getId(), s.getTeacher().getUser().getName(),
            s.getSemester().getId(), s.getSemester().getName(),
            s.getDayOfWeek(), getDayName(s.getDayOfWeek()), s.getPeriod(), s.getRoom(), s.getShift());
    }

    private String getDayName(int day) {
        return switch (day) {
            case 1 -> "Chủ nhật"; case 2 -> "Thứ 2"; case 3 -> "Thứ 3";
            case 4 -> "Thứ 4"; case 5 -> "Thứ 5"; case 6 -> "Thứ 6"; case 7 -> "Thứ 7";
            default -> "";
        };
    }
}
