package vn.edu.fpt.myfschool.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.enums.AssignmentStatus;
import vn.edu.fpt.myfschool.common.enums.AcademicYearStatus;
import vn.edu.fpt.myfschool.common.enums.Shift;
import vn.edu.fpt.myfschool.common.enums.TimetableStatus;
import vn.edu.fpt.myfschool.common.enums.UserRole;
import vn.edu.fpt.myfschool.common.enums.UserStatus;
import vn.edu.fpt.myfschool.common.exception.ConflictException;
import vn.edu.fpt.myfschool.common.exception.ForbiddenException;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.entity.*;
import vn.edu.fpt.myfschool.repository.*;
import vn.edu.fpt.myfschool.service.ScheduleService;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service("scheduleService")
@RequiredArgsConstructor
@Transactional
public class ScheduleServiceImpl implements ScheduleService {
    private static final Set<TimetableStatus> PLANNING_STATUSES =
        Set.of(TimetableStatus.DRAFT, TimetableStatus.SCHEDULED, TimetableStatus.ACTIVE);

    private final ScheduleRepository scheduleRepository;
    private final TeachingAssignmentRepository teachingAssignmentRepository;
    private final TimetableRepository timetableRepository;
    private final ClassRepository classRepository;
    private final TeacherRepository teacherRepository;
    private final SemesterRepository semesterRepository;
    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;
    private final StudentGuardianRepository studentGuardianRepository;
    private final PeriodRepository periodRepository;
    private final AcademicYearPeriodRepository academicYearPeriodRepository;
    private final AcademicYearShiftRepository academicYearShiftRepository;

    @Override
    @Transactional(readOnly = true)
    public ClassScheduleDto getClassSchedule(Long classId, Long semesterId, LocalDate date) {
        SchoolClass cls = classRepository.findById(classId)
            .orElseThrow(() -> new ResourceNotFoundException("Class", "id", classId));
        Semester semester = semesterRepository.findById(semesterId)
            .orElseThrow(() -> new ResourceNotFoundException("Semester", "id", semesterId));
        requireSameAcademicYear(cls, semester);
        Timetable timetable = resolveTimetable(classId, semesterId, date);
        List<Schedule> schedules = timetable == null ? List.of()
            : scheduleRepository.findByTimetableIdOrderByDayOfWeekAscPeriodAsc(timetable.getId());
        return buildSchedule(cls.getId(), cls.getName(), semester, timetable, schedules);
    }

    @Override
    @Transactional(readOnly = true)
    public ClassScheduleDto getTeacherSchedule(Long teacherId, Long semesterId, LocalDate date) {
        Teacher teacher = teacherRepository.findById(teacherId)
            .orElseThrow(() -> new ResourceNotFoundException("Teacher", "id", teacherId));
        Semester semester = semesterRepository.findById(semesterId)
            .orElseThrow(() -> new ResourceNotFoundException("Semester", "id", semesterId));
        List<Timetable> timetables = date == null
            ? timetableRepository.findOverlappingPublished(semesterId, semester.getStartDate(), semester.getEndDate()).stream()
                .filter(item -> item.getStatus() == TimetableStatus.ACTIVE).toList()
            : timetableRepository.findEffectiveBySemester(semesterId, date);
        List<Schedule> schedules = timetables.stream()
            .flatMap(item -> scheduleRepository.findByTimetableIdOrderByDayOfWeekAscPeriodAsc(item.getId()).stream())
            .filter(item -> item.getAssignment().getTeacher().getId().equals(teacherId)).toList();
        return buildSchedule(teacherId, teacher.getUser().getName(), semester, null, schedules);
    }

    @Override
    @Transactional(readOnly = true)
    public ClassScheduleDto getStudentSchedule(Long studentId, Long userId, LocalDate date) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("Student", "id", studentId));
        Parent parent = parentRepository.findByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Parent", "userId", userId));
        if (!studentGuardianRepository.existsByStudentIdAndGuardianId(studentId, parent.getId())) {
            throw new ForbiddenException("Phụ huynh không có quyền xem thời khóa biểu của học sinh này");
        }
        return getScheduleForStudent(student, currentSemester(), date);
    }

    @Override
    @Transactional(readOnly = true)
    public ClassScheduleDto getMySchedule(Long userId, UserRole role, LocalDate date) {
        Semester semester = currentSemester();
        if (role == UserRole.STUDENT) {
            Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "userId", userId));
            return getScheduleForStudent(student, semester, date);
        }
        if (role == UserRole.TEACHER) {
            Teacher teacher = teacherRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", "userId", userId));
            return getTeacherSchedule(teacher.getId(), semester.getId(), date);
        }
        throw new ForbiddenException("Vai trò không được phép xem thời khóa biểu cá nhân");
    }

    @Override
    public ScheduleDto createSchedule(ScheduleRequest request) {
        Timetable timetable = timetableRepository.findById(request.timetableId())
            .orElseThrow(() -> new ResourceNotFoundException("Timetable", "id", request.timetableId()));
        if (timetable.getStatus() != TimetableStatus.DRAFT) {
            throw new ConflictException("Chỉ được chỉnh sửa thời khóa biểu nháp");
        }
        TeachingAssignment assignment = teachingAssignmentRepository.findById(request.assignmentId())
            .orElseThrow(() -> new ResourceNotFoundException("TeachingAssignment", "id", request.assignmentId()));
        if (assignment.getStatus() != AssignmentStatus.ACTIVE) {
            throw new ConflictException("Phân công giảng dạy không còn hiệu lực");
        }
        if (assignment.getTeacher().getUser().getStatus() != UserStatus.ACTIVE) {
            throw new ConflictException("Tài khoản giáo viên đã bị khóa");
        }
        if (!assignment.getCls().getId().equals(timetable.getCls().getId())) {
            throw new ConflictException("Phân công không thuộc lớp của thời khóa biểu");
        }
        if (assignment.getEffectiveFrom().isAfter(timetable.getSemester().getEndDate())
                || (assignment.getEffectiveTo() != null
                    && assignment.getEffectiveTo().isBefore(timetable.getSemester().getStartDate()))) {
            throw new ConflictException("Phân công giảng dạy không có hiệu lực trong học kỳ này");
        }
        Period period = periodRepository.findById(request.periodId())
            .orElseThrow(() -> new ResourceNotFoundException("Period", "id", request.periodId()));
        Long academicYearId = timetable.getCls().getAcademicYear().getId();
        if (!academicYearPeriodRepository.existsByAcademicYearIdAndPeriodId(academicYearId, period.getId())) {
            throw new ConflictException("Tiết học chưa được áp dụng cho năm học của lớp");
        }
        if (!academicYearShiftRepository.existsByAcademicYearIdAndShiftId(academicYearId, period.getShift().getId())) {
            throw new ConflictException("Buổi học chưa được áp dụng cho năm học của lớp");
        }
        if (scheduleRepository.findByTimetableIdAndDayOfWeekAndPeriodRefId(
                timetable.getId(), request.dayOfWeek(), period.getId()).isPresent()) {
            throw new ConflictException("Lớp đã có môn học tại khung giờ này");
        }
        if (scheduleRepository.existsTeacherPlanningConflict(
                assignment.getTeacher().getId(), timetable.getSemester().getId(), timetable.getCls().getId(),
                request.dayOfWeek(), period.getId(), PLANNING_STATUSES)) {
            throw new ConflictException("Giáo viên đã có tiết ở lớp khác tại khung giờ này");
        }

        Schedule schedule = new Schedule();
        schedule.setTimetable(timetable);
        schedule.setAssignment(assignment);
        schedule.setDayOfWeek(request.dayOfWeek());
        schedule.setPeriod(period.getOrder());
        schedule.setPeriodRef(period);
        schedule.setRoom(timetable.getCls().getName());
        schedule.setShift(resolveShift(period));
        return toDto(scheduleRepository.save(schedule));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScheduleDto> getTimetableSlots(Long timetableId) {
        if (!timetableRepository.existsById(timetableId)) {
            throw new ResourceNotFoundException("Timetable", "id", timetableId);
        }
        return scheduleRepository.findByTimetableIdOrderByDayOfWeekAscPeriodAsc(timetableId)
            .stream().map(this::toDto).toList();
    }

    @Override
    public void deleteSchedule(Long scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
            .orElseThrow(() -> new ResourceNotFoundException("Schedule", "id", scheduleId));
        if (schedule.getTimetable().getStatus() != TimetableStatus.DRAFT) {
            throw new ConflictException("Không thể sửa thời khóa biểu đã phát hành");
        }
        scheduleRepository.delete(schedule);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PeriodDto> getAvailablePeriods(Long classId, Long semesterId, Integer dayOfWeek, Long shiftId) {
        SchoolClass cls = classRepository.findById(classId)
            .orElseThrow(() -> new ResourceNotFoundException("Class", "id", classId));
        Semester semester = semesterRepository.findById(semesterId)
            .orElseThrow(() -> new ResourceNotFoundException("Semester", "id", semesterId));
        requireSameAcademicYear(cls, semester);
        if (!academicYearShiftRepository.existsByAcademicYearIdAndShiftId(cls.getAcademicYear().getId(), shiftId)) {
            throw new ConflictException("Buổi học chưa được áp dụng cho năm học của lớp");
        }
        Timetable timetable = timetableRepository
            .findFirstByClsIdAndSemesterIdAndStatusOrderByVersionDesc(classId, semesterId, TimetableStatus.DRAFT)
            .or(() -> timetableRepository.findFirstByClsIdAndSemesterIdAndStatusOrderByVersionDesc(
                classId, semesterId, TimetableStatus.ACTIVE)).orElse(null);
        List<Schedule> existing = timetable == null ? List.of()
            : scheduleRepository.findByTimetableIdOrderByDayOfWeekAscPeriodAsc(timetable.getId());
        Set<Long> occupied = existing.stream()
            .filter(item -> item.getDayOfWeek().equals(dayOfWeek) && item.getPeriodRef() != null)
            .map(item -> item.getPeriodRef().getId()).collect(Collectors.toSet());
        Set<Long> appliedPeriodIds = academicYearPeriodRepository.findByAcademicYearId(cls.getAcademicYear().getId())
            .stream().map(item -> item.getPeriod().getId()).collect(Collectors.toSet());
        return periodRepository.findByShiftIdOrderByOrderAsc(shiftId).stream()
            .filter(period -> appliedPeriodIds.contains(period.getId()) && !occupied.contains(period.getId()))
            .map(this::toPeriodDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentAvailabilityDto> getAssignmentAvailability(Long classId, Long semesterId) {
        SchoolClass cls = classRepository.findById(classId)
            .orElseThrow(() -> new ResourceNotFoundException("Class", "id", classId));
        Semester semester = semesterRepository.findById(semesterId)
            .orElseThrow(() -> new ResourceNotFoundException("Semester", "id", semesterId));
        requireSameAcademicYear(cls, semester);

        List<TeachingAssignment> assignments = teachingAssignmentRepository
            .findByClsIdAndStatus(classId, AssignmentStatus.ACTIVE).stream()
            .filter(assignment -> !assignment.getEffectiveFrom().isAfter(semester.getEndDate()))
            .filter(assignment -> assignment.getEffectiveTo() == null
                || !assignment.getEffectiveTo().isBefore(semester.getStartDate()))
            .filter(assignment -> assignment.getTeacher().getUser().getStatus() == UserStatus.ACTIVE)
            .toList();
        List<Period> periods = academicYearPeriodRepository.findByAcademicYearId(cls.getAcademicYear().getId())
            .stream().map(AcademicYearPeriod::getPeriod)
            .filter(period -> Boolean.TRUE.equals(period.getIsActive()))
            .sorted(Comparator.comparing((Period period) -> period.getShift().getOrder())
                .thenComparing(Period::getOrder))
            .toList();

        Map<SlotKey, Set<Long>> busyTeachers = new HashMap<>();
        scheduleRepository.findPlanningSchedules(semesterId, PLANNING_STATUSES).stream()
            .filter(slot -> !slot.getTimetable().getCls().getId().equals(classId))
            .forEach(slot -> busyTeachers
                .computeIfAbsent(new SlotKey(slot.getDayOfWeek(), slot.getPeriodRef().getId()), ignored -> new HashSet<>())
                .add(slot.getAssignment().getTeacher().getId()));

        List<AssignmentAvailabilityDto> result = new ArrayList<>();
        for (int day = 1; day <= 7; day++) {
            for (Period period : periods) {
                Set<Long> busy = busyTeachers.getOrDefault(new SlotKey(day, period.getId()), Set.of());
                List<Long> availableIds = assignments.stream()
                    .filter(assignment -> !busy.contains(assignment.getTeacher().getId()))
                    .map(TeachingAssignment::getId)
                    .toList();
                result.add(new AssignmentAvailabilityDto(day, period.getId(), availableIds));
            }
        }
        return result;
    }

    private Timetable resolveTimetable(Long classId, Long semesterId, LocalDate date) {
        if (date != null) {
            return timetableRepository.findEffective(classId, semesterId, date).stream().findFirst().orElse(null);
        }
        return timetableRepository.findFirstByClsIdAndSemesterIdAndStatusOrderByVersionDesc(
            classId, semesterId, TimetableStatus.ACTIVE).orElse(null);
    }

    private Semester currentSemester() {
        return semesterRepository.findFirstByIsCurrentTrueAndAcademicYearStatus(AcademicYearStatus.ACTIVE)
            .orElseThrow(() -> new ResourceNotFoundException("Semester", "isCurrent", true));
    }

    private ClassScheduleDto getScheduleForStudent(Student student, Semester semester, LocalDate date) {
        SchoolClass cls = student.getCurrentClass();
        if (cls == null) {
            throw new ConflictException("Học sinh chưa được xếp lớp");
        }
        return getClassSchedule(cls.getId(), semester.getId(), date);
    }

    private ClassScheduleDto buildSchedule(Long ownerId, String ownerName, Semester semester,
                                           Timetable timetable, List<Schedule> schedules) {
        Map<Integer, List<Schedule>> byDay = schedules.stream().collect(Collectors.groupingBy(Schedule::getDayOfWeek));
        List<DayScheduleDto> days = new ArrayList<>();
        for (int day = 1; day <= 7; day++) {
            List<Schedule> daySchedules = byDay.getOrDefault(day, List.of());
            List<ScheduleDto> morning = daySchedules.stream().filter(item -> item.getShift() == Shift.MORNING).map(this::toDto).toList();
            List<ScheduleDto> afternoon = daySchedules.stream().filter(item -> item.getShift() == Shift.AFTERNOON).map(this::toDto).toList();
            days.add(new DayScheduleDto(day, getDayName(day), morning, afternoon));
        }
        return new ClassScheduleDto(ownerId, ownerName, semester.getId(), semester.getName(),
            timetable == null ? null : timetable.getId(), timetable == null ? null : timetable.getVersion(), days);
    }

    private ScheduleDto toDto(Schedule schedule) {
        TeachingAssignment assignment = schedule.getAssignment();
        Timetable timetable = schedule.getTimetable();
        return new ScheduleDto(schedule.getId(), timetable.getId(), timetable.getVersion(), assignment.getId(),
            assignment.getCls().getId(), assignment.getCls().getName(),
            assignment.getSubject().getId(), assignment.getSubject().getName(), assignment.getSubject().getCode(),
            assignment.getTeacher().getId(), assignment.getTeacher().getUser().getName(),
            timetable.getSemester().getId(), timetable.getSemester().getName(),
            schedule.getDayOfWeek(), getDayName(schedule.getDayOfWeek()),
            schedule.getPeriodRef().getId(), schedule.getPeriodRef().getName(), schedule.getPeriodRef().getOrder(),
            schedule.getPeriodRef().getShift().getId(), schedule.getPeriodRef().getShift().getName(),
            schedule.getRoom(), schedule.getShift());
    }

    private PeriodDto toPeriodDto(Period period) {
        return new PeriodDto(period.getId(), period.getName(), period.getOrder(),
            period.getShift().getId(), period.getShift().getName());
    }

    private Shift resolveShift(Period period) {
        try {
            return Shift.valueOf(period.getShift().getCode().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ConflictException("Mã buổi học không hỗ trợ: " + period.getShift().getCode());
        }
    }

    private void requireSameAcademicYear(SchoolClass cls, Semester semester) {
        if (!cls.getAcademicYear().getId().equals(semester.getAcademicYear().getId())) {
            throw new ConflictException("Lớp và học kỳ không thuộc cùng năm học");
        }
    }

    private String getDayName(int day) {
        return switch (day) {
            case 1 -> "Chủ nhật"; case 2 -> "Thứ 2"; case 3 -> "Thứ 3"; case 4 -> "Thứ 4";
            case 5 -> "Thứ 5"; case 6 -> "Thứ 6"; case 7 -> "Thứ 7"; default -> "";
        };
    }

    private record SlotKey(Integer dayOfWeek, Long periodId) {}
}
