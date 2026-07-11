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
import vn.edu.fpt.myfschool.repository.TimetableRepository;
import vn.edu.fpt.myfschool.service.TimetableService;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TimetableServiceImpl implements TimetableService {
    private final TimetableRepository timetableRepository;
    private final ScheduleRepository scheduleRepository;
    private final ClassRepository classRepository;
    private final SemesterRepository semesterRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TimetableDto> list(Long classId, Long semesterId) {
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
        requireDateInSemester(request.effectiveFrom(), semester);
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
        timetable.setEffectiveFrom(request.effectiveFrom());
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
                slot.setRoom(old.getRoom());
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
        requireDateInSemester(effectiveFrom, timetable.getSemester());
        List<Schedule> slots = scheduleRepository.findByTimetableIdOrderByDayOfWeekAscPeriodAsc(id);
        if (slots.isEmpty()) throw new ConflictException("Thời khóa biểu chưa có tiết học");
        validatePublishedConflicts(timetable, slots, effectiveFrom, timetable.getSemester().getEndDate());

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
        return toDto(timetableRepository.save(timetable));
    }

    @Override
    public void deleteDraft(Long id) {
        Timetable timetable = find(id);
        if (timetable.getStatus() != TimetableStatus.DRAFT) {
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
                other.getDayOfWeek().equals(slot.getDayOfWeek()) && other.getPeriod().equals(slot.getPeriod())
                    && other.getAssignment().getTeacher().getId().equals(slot.getAssignment().getTeacher().getId()));
            if (teacherConflict) throw new ConflictException("Giáo viên bị trùng lịch tại " + dayLabel(slot.getDayOfWeek()) + ", tiết " + slot.getPeriod());
            if (slot.getRoom() != null && !slot.getRoom().isBlank()) {
                boolean roomConflict = otherSlots.stream().anyMatch(other ->
                    other.getDayOfWeek().equals(slot.getDayOfWeek()) && other.getPeriod().equals(slot.getPeriod())
                        && slot.getRoom().equalsIgnoreCase(other.getRoom()));
                if (roomConflict) throw new ConflictException("Phòng " + slot.getRoom() + " bị trùng tại " + dayLabel(slot.getDayOfWeek()) + ", tiết " + slot.getPeriod());
            }
        }
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
