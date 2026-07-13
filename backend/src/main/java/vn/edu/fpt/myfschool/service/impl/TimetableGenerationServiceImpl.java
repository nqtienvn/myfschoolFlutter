package vn.edu.fpt.myfschool.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.AutoGenerateTimetableRequest;
import vn.edu.fpt.myfschool.common.dto.AutoGenerateTimetableResult;
import vn.edu.fpt.myfschool.common.dto.TimetableDto;
import vn.edu.fpt.myfschool.common.enums.AssignmentStatus;
import vn.edu.fpt.myfschool.common.enums.Shift;
import vn.edu.fpt.myfschool.common.enums.TimetableStatus;
import vn.edu.fpt.myfschool.common.enums.UserStatus;
import vn.edu.fpt.myfschool.common.exception.BadRequestException;
import vn.edu.fpt.myfschool.common.exception.ConflictException;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.entity.AcademicYearPeriod;
import vn.edu.fpt.myfschool.entity.Period;
import vn.edu.fpt.myfschool.entity.Schedule;
import vn.edu.fpt.myfschool.entity.SchoolClass;
import vn.edu.fpt.myfschool.entity.Semester;
import vn.edu.fpt.myfschool.entity.TeachingAssignment;
import vn.edu.fpt.myfschool.entity.Timetable;
import vn.edu.fpt.myfschool.repository.AcademicYearPeriodRepository;
import vn.edu.fpt.myfschool.repository.AcademicYearShiftRepository;
import vn.edu.fpt.myfschool.repository.ClassRepository;
import vn.edu.fpt.myfschool.repository.PeriodRepository;
import vn.edu.fpt.myfschool.repository.ScheduleRepository;
import vn.edu.fpt.myfschool.repository.SemesterRepository;
import vn.edu.fpt.myfschool.repository.TeachingAssignmentRepository;
import vn.edu.fpt.myfschool.repository.TimetableRepository;
import vn.edu.fpt.myfschool.service.TimetableGenerationService;
import vn.edu.fpt.myfschool.service.TimetableService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class TimetableGenerationServiceImpl implements TimetableGenerationService {
    private static final List<Integer> SCHOOL_DAYS = List.of(2, 3, 4, 5, 6, 7);
    private static final Set<TimetableStatus> PLANNING_STATUSES =
        Set.of(TimetableStatus.DRAFT, TimetableStatus.SCHEDULED, TimetableStatus.ACTIVE);

    private final ClassRepository classRepository;
    private final SemesterRepository semesterRepository;
    private final PeriodRepository periodRepository;
    private final AcademicYearPeriodRepository academicYearPeriodRepository;
    private final AcademicYearShiftRepository academicYearShiftRepository;
    private final TeachingAssignmentRepository teachingAssignmentRepository;
    private final TimetableRepository timetableRepository;
    private final ScheduleRepository scheduleRepository;
    private final TimetableService timetableService;

    @Override
    public AutoGenerateTimetableResult generate(AutoGenerateTimetableRequest request) {
        Semester semester = semesterRepository.findById(request.semesterId())
            .orElseThrow(() -> new ResourceNotFoundException("Semester", "id", request.semesterId()));
        if (!semester.getAcademicYear().getId().equals(request.academicYearId())) {
            throw new ConflictException("Học kỳ không thuộc năm học đang chọn");
        }

        List<SchoolClass> classes = resolveClasses(request);
        List<Period> periods = resolvePeriods(request);
        List<ClassPlan> classPlans = resolveClassPlans(classes, semester);
        Set<Long> selectedClassIds = classes.stream().map(SchoolClass::getId).collect(LinkedHashSet::new, Set::add, Set::addAll);

        for (SchoolClass cls : classes) {
            if (timetableRepository.existsByClsIdAndSemesterIdAndStatus(
                    cls.getId(), semester.getId(), TimetableStatus.SCHEDULED)) {
                throw new ConflictException("Lớp " + cls.getName() + " đang có thời khóa biểu chờ phát hành");
            }
        }

        PlanningOccupancy occupancy = planningOccupancy(semester.getId(), selectedClassIds);
        Map<Long, Integer> assignmentUsage = new HashMap<>();
        List<PlannedSlot> plannedSlots = new ArrayList<>();

        for (Integer day : SCHOOL_DAYS) {
            for (Period period : periods) {
                SlotKey key = new SlotKey(day, period.getId());
                Set<Long> unavailableTeachers = new HashSet<>(
                    occupancy.externallyBusyTeachers().getOrDefault(key, Set.of()));
                Map<Long, TeachingAssignment> selection = new HashMap<>();
                Map<Long, Set<Long>> activeTeacherClasses = occupancy.selectedActiveTeacherClasses()
                    .getOrDefault(key, Map.of());
                if (!assignSlot(new ArrayList<>(classPlans), unavailableTeachers,
                        activeTeacherClasses, selection, assignmentUsage)) {
                    throw new ConflictException(
                        "Không thể xếp lịch không trùng giáo viên tại " + dayLabel(day) + ", " + period.getName()
                            + ". Hãy bỏ bớt lớp/tiết hoặc bổ sung phân công giáo viên.");
                }
                for (ClassPlan classPlan : classPlans) {
                    plannedSlots.add(new PlannedSlot(
                        classPlan.cls().getId(), selection.get(classPlan.cls().getId()), day, period));
                }
            }
        }

        Map<Long, Timetable> drafts = prepareDrafts(classes, semester);
        List<Schedule> oldSlots = drafts.values().stream()
            .flatMap(timetable -> scheduleRepository
                .findByTimetableIdOrderByDayOfWeekAscPeriodAsc(timetable.getId()).stream())
            .toList();
        if (!oldSlots.isEmpty()) {
            scheduleRepository.deleteAll(oldSlots);
            scheduleRepository.flush();
        }

        List<Schedule> schedules = plannedSlots.stream().map(planned -> {
            Schedule schedule = new Schedule();
            schedule.setTimetable(drafts.get(planned.classId()));
            schedule.setAssignment(planned.assignment());
            schedule.setDayOfWeek(planned.dayOfWeek());
            schedule.setPeriod(planned.period().getOrder());
            schedule.setPeriodRef(planned.period());
            schedule.setRoom(drafts.get(planned.classId()).getCls().getName());
            schedule.setShift(resolveShift(planned.period()));
            return schedule;
        }).toList();
        scheduleRepository.saveAll(schedules);
        scheduleRepository.flush();

        List<TimetableDto> timetableDtos = drafts.values().stream()
            .map(timetable -> timetableService.get(timetable.getId()))
            .toList();
        return new AutoGenerateTimetableResult(
            request.academicYearId(), semester.getId(), classes.size(), drafts.size(), schedules.size(), timetableDtos);
    }

    private List<SchoolClass> resolveClasses(AutoGenerateTimetableRequest request) {
        List<SchoolClass> classes;
        if (request.classIds() == null || request.classIds().isEmpty()) {
            classes = new ArrayList<>(classRepository.findByAcademicYearId(request.academicYearId()));
        } else {
            classes = new LinkedHashSet<>(request.classIds()).stream().map(classId -> {
                SchoolClass cls = classRepository.findById(classId)
                    .orElseThrow(() -> new ResourceNotFoundException("Class", "id", classId));
                if (!cls.getAcademicYear().getId().equals(request.academicYearId())) {
                    throw new ConflictException("Lớp " + cls.getName() + " không thuộc năm học đang chọn");
                }
                return cls;
            }).toList();
        }
        if (classes.isEmpty()) {
            throw new ConflictException("Năm học chưa có lớp để tạo thời khóa biểu");
        }
        return classes.stream().sorted(Comparator.comparing(SchoolClass::getName)).toList();
    }

    private List<Period> resolvePeriods(AutoGenerateTimetableRequest request) {
        Set<Long> selectedShiftIds = new LinkedHashSet<>(request.shiftIds());
        Set<Long> selectedPeriodIds = new LinkedHashSet<>(request.periodIds());
        Set<Long> appliedShiftIds = academicYearShiftRepository.findByAcademicYearId(request.academicYearId()).stream()
            .map(item -> item.getShift().getId()).collect(LinkedHashSet::new, Set::add, Set::addAll);
        Set<Long> appliedPeriodIds = academicYearPeriodRepository.findByAcademicYearId(request.academicYearId()).stream()
            .map(AcademicYearPeriod::getPeriod).map(Period::getId)
            .collect(LinkedHashSet::new, Set::add, Set::addAll);

        if (!appliedShiftIds.containsAll(selectedShiftIds)) {
            throw new ConflictException("Có ca học chưa được áp dụng cho năm học");
        }
        if (!appliedPeriodIds.containsAll(selectedPeriodIds)) {
            throw new ConflictException("Có tiết học chưa được áp dụng cho năm học");
        }

        List<Period> periods = selectedPeriodIds.stream().map(periodId -> {
            Period period = periodRepository.findById(periodId)
                .orElseThrow(() -> new ResourceNotFoundException("Period", "id", periodId));
            if (!selectedShiftIds.contains(period.getShift().getId())) {
                throw new BadRequestException("Tiết " + period.getName() + " không thuộc ca học đã chọn");
            }
            if (!Boolean.TRUE.equals(period.getIsActive())) {
                throw new ConflictException("Tiết " + period.getName() + " đã ngừng sử dụng");
            }
            return period;
        }).sorted(Comparator.comparing((Period period) -> period.getShift().getOrder())
            .thenComparing(Period::getOrder)).toList();

        Set<Long> shiftsWithPeriods = periods.stream().map(period -> period.getShift().getId())
            .collect(LinkedHashSet::new, Set::add, Set::addAll);
        if (!shiftsWithPeriods.containsAll(selectedShiftIds)) {
            throw new BadRequestException("Mỗi ca học đã chọn phải có ít nhất một tiết");
        }
        return periods;
    }

    private List<ClassPlan> resolveClassPlans(List<SchoolClass> classes, Semester semester) {
        return classes.stream().map(cls -> {
            List<TeachingAssignment> assignments = teachingAssignmentRepository
                .findByClsIdAndStatus(cls.getId(), AssignmentStatus.ACTIVE).stream()
                .filter(assignment -> !assignment.getEffectiveFrom().isAfter(semester.getEndDate()))
                .filter(assignment -> assignment.getEffectiveTo() == null
                    || !assignment.getEffectiveTo().isBefore(semester.getStartDate()))
                .filter(assignment -> assignment.getTeacher().getUser().getStatus() == UserStatus.ACTIVE)
                .sorted(Comparator.comparing(assignment -> assignment.getSubject().getName()))
                .toList();
            if (assignments.isEmpty()) {
                throw new ConflictException("Lớp " + cls.getName() + " chưa có phân công giảng dạy");
            }
            return new ClassPlan(cls, assignments);
        }).toList();
    }

    private PlanningOccupancy planningOccupancy(Long semesterId, Set<Long> selectedClassIds) {
        Map<SlotKey, Set<Long>> externalBusy = new HashMap<>();
        Map<SlotKey, Map<Long, Set<Long>>> selectedActive = new HashMap<>();
        for (Schedule slot : scheduleRepository.findPlanningSchedules(semesterId, PLANNING_STATUSES)) {
            Long classId = slot.getTimetable().getCls().getId();
            Long teacherId = slot.getAssignment().getTeacher().getId();
            SlotKey key = new SlotKey(slot.getDayOfWeek(), slot.getPeriodRef().getId());
            if (!selectedClassIds.contains(classId)) {
                externalBusy.computeIfAbsent(key, ignored -> new HashSet<>()).add(teacherId);
            } else if (slot.getTimetable().getStatus() == TimetableStatus.ACTIVE) {
                selectedActive.computeIfAbsent(key, ignored -> new HashMap<>())
                    .computeIfAbsent(teacherId, ignored -> new HashSet<>()).add(classId);
            }
        }
        return new PlanningOccupancy(externalBusy, selectedActive);
    }

    private boolean assignSlot(List<ClassPlan> remaining,
                               Set<Long> unavailableTeachers,
                               Map<Long, Set<Long>> activeTeacherClasses,
                               Map<Long, TeachingAssignment> selection,
                               Map<Long, Integer> assignmentUsage) {
        if (remaining.isEmpty()) return true;

        ClassPlan next = remaining.stream()
            .min(Comparator.comparingLong(plan -> plan.assignments().stream()
                .filter(assignment -> isAvailableForClass(
                    assignment.getTeacher().getId(), plan.cls().getId(), unavailableTeachers, activeTeacherClasses))
                .map(assignment -> assignment.getTeacher().getId()).distinct().count()))
            .orElseThrow();
        List<ClassPlan> afterNext = new ArrayList<>(remaining);
        afterNext.remove(next);

        List<TeachingAssignment> candidates = next.assignments().stream()
            .filter(assignment -> isAvailableForClass(
                assignment.getTeacher().getId(), next.cls().getId(), unavailableTeachers, activeTeacherClasses))
            .sorted(Comparator
                .comparingInt((TeachingAssignment assignment) -> assignmentUsage.getOrDefault(assignment.getId(), 0))
                .thenComparing(assignment -> assignment.getSubject().getName())
                .thenComparing(assignment -> assignment.getTeacher().getId()))
            .toList();

        for (TeachingAssignment candidate : candidates) {
            Long teacherId = candidate.getTeacher().getId();
            unavailableTeachers.add(teacherId);
            selection.put(next.cls().getId(), candidate);
            assignmentUsage.merge(candidate.getId(), 1, Integer::sum);
            if (assignSlot(afterNext, unavailableTeachers, activeTeacherClasses, selection, assignmentUsage)) return true;
            assignmentUsage.computeIfPresent(candidate.getId(), (ignored, count) -> count <= 1 ? null : count - 1);
            selection.remove(next.cls().getId());
            unavailableTeachers.remove(teacherId);
        }
        return false;
    }

    private boolean isAvailableForClass(Long teacherId, Long classId,
                                        Set<Long> unavailableTeachers,
                                        Map<Long, Set<Long>> activeTeacherClasses) {
        if (unavailableTeachers.contains(teacherId)) return false;
        return activeTeacherClasses.getOrDefault(teacherId, Set.of()).stream()
            .allMatch(ownerClassId -> ownerClassId.equals(classId));
    }

    private Map<Long, Timetable> prepareDrafts(List<SchoolClass> classes, Semester semester) {
        Map<Long, Timetable> drafts = new LinkedHashMap<>();
        for (SchoolClass cls : classes) {
            Timetable timetable = timetableRepository.findFirstByClsIdAndSemesterIdAndStatusOrderByVersionDesc(
                cls.getId(), semester.getId(), TimetableStatus.DRAFT).orElseGet(() -> {
                    int version = timetableRepository.findFirstByClsIdAndSemesterIdOrderByVersionDesc(
                        cls.getId(), semester.getId()).map(item -> item.getVersion() + 1).orElse(1);
                    Timetable draft = new Timetable();
                    draft.setCls(cls);
                    draft.setSemester(semester);
                    draft.setVersion(version);
                    draft.setStatus(TimetableStatus.DRAFT);
                    draft.setEffectiveFrom(semester.getStartDate());
                    draft.setEffectiveTo(semester.getEndDate());
                    return timetableRepository.save(draft);
                });
            drafts.put(cls.getId(), timetable);
        }
        return drafts;
    }

    private Shift resolveShift(Period period) {
        try {
            return Shift.valueOf(period.getShift().getCode().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ConflictException("Mã ca học không hỗ trợ: " + period.getShift().getCode());
        }
    }

    private String dayLabel(int day) {
        return day == 1 ? "Chủ nhật" : "Thứ " + day;
    }

    private record SlotKey(Integer dayOfWeek, Long periodId) {}
    private record PlanningOccupancy(
        Map<SlotKey, Set<Long>> externallyBusyTeachers,
        Map<SlotKey, Map<Long, Set<Long>>> selectedActiveTeacherClasses
    ) {}
    private record ClassPlan(SchoolClass cls, List<TeachingAssignment> assignments) {}
    private record PlannedSlot(Long classId, TeachingAssignment assignment, Integer dayOfWeek, Period period) {}
}
