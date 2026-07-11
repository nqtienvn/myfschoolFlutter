package vn.edu.fpt.myfschool.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.CreateTimetableRequest;
import vn.edu.fpt.myfschool.common.dto.TimetableDto;
import vn.edu.fpt.myfschool.common.enums.TimetableStatus;
import vn.edu.fpt.myfschool.common.exception.BadRequestException;
import vn.edu.fpt.myfschool.common.exception.ConflictException;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.entity.Schedule;
import vn.edu.fpt.myfschool.entity.SchoolClass;
import vn.edu.fpt.myfschool.entity.Semester;
import vn.edu.fpt.myfschool.entity.Timetable;
import vn.edu.fpt.myfschool.repository.ClassRepository;
import vn.edu.fpt.myfschool.repository.ScheduleRepository;
import vn.edu.fpt.myfschool.repository.SemesterRepository;
import vn.edu.fpt.myfschool.repository.StudentGuardianRepository;
import vn.edu.fpt.myfschool.repository.StudentRepository;
import vn.edu.fpt.myfschool.repository.TimetableRepository;
import vn.edu.fpt.myfschool.service.NotificationService;
import vn.edu.fpt.myfschool.service.TimetableService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class TimetableServiceImpl implements TimetableService {
    private final TimetableRepository timetableRepository;
    private final ScheduleRepository scheduleRepository;
    private final ClassRepository classRepository;
    private final SemesterRepository semesterRepository;
    private final StudentRepository studentRepository;
    private final StudentGuardianRepository studentGuardianRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional(readOnly = true)
    public List<TimetableDto> list(Long classId, Long semesterId) {
        SchoolClass cls = classRepository.findById(classId)
            .orElseThrow(() -> new ResourceNotFoundException("Class", "id", classId));
        Semester semester = semesterRepository.findById(semesterId)
            .orElseThrow(() -> new ResourceNotFoundException("Semester", "id", semesterId));
        requireSameYear(cls, semester);
        return timetableRepository.findByClsIdAndSemesterIdOrderByVersionDesc(classId, semesterId)
            .stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TimetableDto get(Long id) {
        return toDto(find(id));
    }

    @Override
    public TimetableDto createDraft(CreateTimetableRequest request) {
        SchoolClass cls = classRepository.findById(request.classId())
            .orElseThrow(() -> new ResourceNotFoundException("Class", "id", request.classId()));
        Semester semester = semesterRepository.findById(request.semesterId())
            .orElseThrow(() -> new ResourceNotFoundException("Semester", "id", request.semesterId()));
        requireSameYear(cls, semester);
        LocalDate initialEffectiveFrom = request.effectiveFrom() == null
            ? semester.getStartDate() : request.effectiveFrom();
        requireDateInSemester(initialEffectiveFrom, semester);
        if (timetableRepository.existsByClsIdAndSemesterIdAndStatus(cls.getId(), semester.getId(), TimetableStatus.DRAFT)) {
            throw new ConflictException("Lớp đã có một thời khóa biểu nháp trong học kỳ này");
        }

        int version = timetableRepository.findFirstByClsIdAndSemesterIdOrderByVersionDesc(cls.getId(), semester.getId())
            .map(item -> item.getVersion() + 1).orElse(1);
        Timetable timetable = new Timetable();
        timetable.setCls(cls);
        timetable.setSemester(semester);
        timetable.setVersion(version);
        timetable.setStatus(TimetableStatus.DRAFT);
        timetable.setEffectiveFrom(initialEffectiveFrom);
        timetable.setEffectiveTo(semester.getEndDate());
        timetable = timetableRepository.save(timetable);

        if (request.copyFromTimetableId() != null) {
            Timetable source = find(request.copyFromTimetableId());
            if (!source.getCls().getId().equals(cls.getId()) || !source.getSemester().getId().equals(semester.getId())) {
                throw new BadRequestException("Chỉ được sao chép thời khóa biểu của cùng lớp và học kỳ");
            }
            for (Schedule old : scheduleRepository.findByTimetableIdOrderByDayOfWeekAscPeriodAsc(source.getId())) {
                Schedule slot = new Schedule();
                slot.setTimetable(timetable);
                slot.setAssignment(old.getAssignment());
                slot.setDayOfWeek(old.getDayOfWeek());
                slot.setPeriod(old.getPeriod());
                slot.setPeriodRef(old.getPeriodRef());
                slot.setRoom(cls.getName());
                slot.setShift(old.getShift());
                scheduleRepository.save(slot);
            }
        }
        return toDto(timetable);
    }

    @Override
    public TimetableDto publish(Long id, LocalDate effectiveFrom) {
        Timetable timetable = find(id);
        if (timetable.getStatus() != TimetableStatus.DRAFT) {
            throw new ConflictException("Chỉ có thể phát hành thời khóa biểu nháp");
        }
        return activate(timetable, effectiveFrom);
    }

    @Override
    public TimetableDto schedulePublish(Long id, LocalDate publishDate) {
        Timetable timetable = find(id);
        if (timetable.getStatus() != TimetableStatus.DRAFT) {
            throw new ConflictException("Chỉ có thể hẹn phát hành thời khóa biểu nháp");
        }
        if (!publishDate.isAfter(LocalDate.now())) {
            throw new BadRequestException("Ngày hẹn phát hành phải sau ngày hiện tại; hãy dùng Phát hành ngay cho hôm nay");
        }
        requireReadyToPublish(timetable, publishDate);
        if (timetableRepository.existsByClsIdAndSemesterIdAndStatus(
                timetable.getCls().getId(), timetable.getSemester().getId(), TimetableStatus.SCHEDULED)) {
            throw new ConflictException("Lớp đã có một thời khóa biểu đang chờ phát hành");
        }
        timetable.setStatus(TimetableStatus.SCHEDULED);
        timetable.setEffectiveFrom(publishDate);
        timetable.setEffectiveTo(timetable.getSemester().getEndDate());
        return toDto(timetableRepository.save(timetable));
    }

    @Override
    public TimetableDto publishScheduled(Long id) {
        Timetable timetable = find(id);
        if (timetable.getStatus() != TimetableStatus.SCHEDULED) {
            throw new ConflictException("Thời khóa biểu không ở trạng thái chờ phát hành");
        }
        if (timetable.getEffectiveFrom().isAfter(LocalDate.now())) {
            throw new ConflictException("Chưa đến ngày phát hành thời khóa biểu");
        }
        return activate(timetable, timetable.getEffectiveFrom());
    }

    private TimetableDto activate(Timetable timetable, LocalDate effectiveFrom) {
        requireReadyToPublish(timetable, effectiveFrom);

        timetableRepository.findFirstByClsIdAndSemesterIdAndStatusOrderByVersionDesc(
            timetable.getCls().getId(), timetable.getSemester().getId(), TimetableStatus.ACTIVE).ifPresent(current -> {
                if (!effectiveFrom.isAfter(current.getEffectiveFrom())) {
                    throw new ConflictException("Ngày áp dụng bản mới phải sau ngày áp dụng thời khóa biểu hiện tại");
                }
                current.setStatus(TimetableStatus.ARCHIVED);
                current.setEffectiveTo(effectiveFrom.minusDays(1));
                timetableRepository.save(current);
            });

        timetable.setStatus(TimetableStatus.ACTIVE);
        timetable.setEffectiveFrom(effectiveFrom);
        timetable.setEffectiveTo(timetable.getSemester().getEndDate());
        Timetable saved = timetableRepository.save(timetable);
        notifyTimetablePublished(saved);
        return toDto(saved);
    }

    private void requireReadyToPublish(Timetable timetable, LocalDate effectiveFrom) {
        requireDateInSemester(effectiveFrom, timetable.getSemester());
        List<Schedule> slots = scheduleRepository.findByTimetableIdOrderByDayOfWeekAscPeriodAsc(timetable.getId());
        if (slots.isEmpty()) throw new ConflictException("Thời khóa biểu chưa có tiết học");
        validatePublishedConflicts(timetable, slots, effectiveFrom, timetable.getSemester().getEndDate());
    }

    @Override
    public void deleteDraft(Long id) {
        Timetable timetable = find(id);
        if (timetable.getStatus() != TimetableStatus.DRAFT
                && timetable.getStatus() != TimetableStatus.SCHEDULED) {
            throw new ConflictException("Không thể xóa thời khóa biểu đã phát hành");
        }
        timetableRepository.delete(timetable);
    }

    private void validatePublishedConflicts(Timetable timetable, List<Schedule> slots,
                                            LocalDate fromDate, LocalDate toDate) {
        List<Timetable> others = timetableRepository.findOverlappingPublished(
            timetable.getSemester().getId(), fromDate, toDate).stream()
            .filter(item -> !item.getCls().getId().equals(timetable.getCls().getId())).toList();
        List<Schedule> otherSlots = others.stream()
            .flatMap(item -> scheduleRepository.findByTimetableIdOrderByDayOfWeekAscPeriodAsc(item.getId()).stream()).toList();
        for (Schedule slot : slots) {
            boolean teacherConflict = otherSlots.stream().anyMatch(other ->
                other.getDayOfWeek().equals(slot.getDayOfWeek()) && samePeriod(other, slot)
                    && other.getAssignment().getTeacher().getId().equals(slot.getAssignment().getTeacher().getId()));
            if (teacherConflict) throw new ConflictException("Giáo viên bị trùng lịch tại " + dayLabel(slot.getDayOfWeek()) + ", tiết " + slot.getPeriod());
            if (slot.getRoom() != null && !slot.getRoom().isBlank()) {
                boolean roomConflict = otherSlots.stream().anyMatch(other ->
                    other.getDayOfWeek().equals(slot.getDayOfWeek()) && samePeriod(other, slot)
                        && slot.getRoom().equalsIgnoreCase(other.getRoom()));
                if (roomConflict) throw new ConflictException("Phòng " + slot.getRoom() + " bị trùng tại " + dayLabel(slot.getDayOfWeek()) + ", tiết " + slot.getPeriod());
            }
        }
    }

    private void notifyTimetablePublished(Timetable timetable) {
        List<Schedule> slots = scheduleRepository
            .findByTimetableIdOrderByDayOfWeekAscPeriodAsc(timetable.getId());
        Set<Long> recipientIds = new LinkedHashSet<>();
        studentRepository.findByCurrentClassId(timetable.getCls().getId()).forEach(student -> {
            recipientIds.add(student.getUser().getId());
            studentGuardianRepository.findByStudentId(student.getId())
                .forEach(link -> recipientIds.add(link.getGuardian().getUser().getId()));
        });
        slots.forEach(slot -> recipientIds.add(slot.getAssignment().getTeacher().getUser().getId()));

        String date = timetable.getEffectiveFrom().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String title = "Thời khóa biểu mới của lớp " + timetable.getCls().getName();
        String body = "Thời khóa biểu đã được phát hành và áp dụng từ ngày " + date + ".";
        recipientIds.forEach(userId ->
            notificationService.createNotification(userId, title, body, "Thời khóa biểu"));
    }

    private boolean samePeriod(Schedule first, Schedule second) {
        return first.getPeriodRef().getId().equals(second.getPeriodRef().getId());
    }

    private void requireSameYear(SchoolClass cls, Semester semester) {
        if (!cls.getAcademicYear().getId().equals(semester.getAcademicYear().getId())) {
            throw new ConflictException("Lớp và học kỳ không thuộc cùng năm học");
        }
    }

    private void requireDateInSemester(LocalDate date, Semester semester) {
        if (date.isBefore(semester.getStartDate()) || date.isAfter(semester.getEndDate())) {
            throw new BadRequestException("Ngày áp dụng phải nằm trong học kỳ");
        }
    }

    private Timetable find(Long id) {
        return timetableRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Timetable", "id", id));
    }

    private TimetableDto toDto(Timetable item) {
        return new TimetableDto(item.getId(), item.getCls().getId(), item.getCls().getName(),
            item.getSemester().getId(), item.getSemester().getName(), item.getVersion(), item.getStatus(),
            item.getEffectiveFrom(), item.getEffectiveTo(),
            scheduleRepository.findByTimetableIdOrderByDayOfWeekAscPeriodAsc(item.getId()).size());
    }

    private String dayLabel(int day) {
        return day == 1 ? "Chủ nhật" : "Thứ " + day;
    }
}
