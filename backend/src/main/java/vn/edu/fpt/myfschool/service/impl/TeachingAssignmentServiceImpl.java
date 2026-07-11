package vn.edu.fpt.myfschool.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.enums.AcademicYearStatus;
import vn.edu.fpt.myfschool.common.enums.AssignmentStatus;
import vn.edu.fpt.myfschool.common.enums.UserStatus;
import vn.edu.fpt.myfschool.common.exception.BadRequestException;
import vn.edu.fpt.myfschool.common.exception.ConflictException;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.entity.*;
import vn.edu.fpt.myfschool.repository.*;
import vn.edu.fpt.myfschool.service.TeachingAssignmentService;

import java.time.LocalDate;
import java.util.List;

@Service("teachingAssignmentService")
@RequiredArgsConstructor
@Transactional
public class TeachingAssignmentServiceImpl implements TeachingAssignmentService {
    private final TeachingAssignmentRepository assignmentRepository;
    private final ClassRepository classRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;
    private final ScheduleRepository scheduleRepository;
    private final AcademicYearSubjectRepository yearSubjectRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TeachingAssignmentDto> listByClass(Long classId, AssignmentStatus status) {
        return assignmentRepository.findByClsIdAndStatus(classId, status).stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeachingAssignmentDto> listByTeacher(Long teacherId, AssignmentStatus status) {
        return assignmentRepository.findByTeacherIdAndStatus(teacherId, status).stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TeachingAssignmentDto getById(Long id) {
        return toDto(find(id));
    }

    @Override
    @Transactional(readOnly = true)
    public TeachingAssignmentDetailDto getDetail(Long id) {
        TeachingAssignment assignment = find(id);
        return new TeachingAssignmentDetailDto(toDto(assignment),
            scheduleRepository.findByAssignmentId(id).stream().map(this::toScheduleDto).toList());
    }

    @Override
    public TeachingAssignmentDto create(CreateTeachingAssignmentRequest request) {
        Resolved resolved = resolve(request);
        validate(resolved);
        TeachingAssignment assignment = assignmentRepository.findByClsIdAndSubjectId(request.classId(), request.subjectId())
            .map(existing -> {
                if (existing.getStatus() == AssignmentStatus.ACTIVE) {
                    throw new ConflictException("Lớp đã có giáo viên phụ trách môn này trong năm học");
                }
                return existing;
            }).orElseGet(TeachingAssignment::new);
        apply(assignment, resolved, request.effectiveFrom());
        assignment.setStatus(AssignmentStatus.ACTIVE);
        assignment.setEffectiveTo(null);
        return toDto(assignmentRepository.save(assignment));
    }

    @Override
    public TeachingAssignmentDto update(Long id, CreateTeachingAssignmentRequest request) {
        TeachingAssignment assignment = find(id);
        Resolved resolved = resolve(request);
        validate(resolved);
        if (assignmentRepository.existsByClsIdAndSubjectIdAndIdNot(request.classId(), request.subjectId(), id)) {
            throw new ConflictException("Lớp đã có giáo viên phụ trách môn này trong năm học");
        }
        apply(assignment, resolved, request.effectiveFrom());
        return toDto(assignmentRepository.save(assignment));
    }

    @Override
    public TeachingAssignmentDto deactivate(Long id) {
        TeachingAssignment assignment = find(id);
        requireYearEditable(assignment.getCls());
        assignment.setStatus(AssignmentStatus.INACTIVE);
        assignment.setEffectiveTo(LocalDate.now());
        return toDto(assignmentRepository.save(assignment));
    }

    @Override
    public TeachingAssignmentDto reactivate(Long id) {
        TeachingAssignment assignment = find(id);
        requireYearEditable(assignment.getCls());
        assignment.setStatus(AssignmentStatus.ACTIVE);
        assignment.setEffectiveTo(null);
        return toDto(assignmentRepository.save(assignment));
    }

    private Resolved resolve(CreateTeachingAssignmentRequest request) {
        return new Resolved(
            classRepository.findById(request.classId())
                .orElseThrow(() -> new ResourceNotFoundException("Class", "id", request.classId())),
            subjectRepository.findById(request.subjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject", "id", request.subjectId())),
            teacherRepository.findById(request.teacherId())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", "id", request.teacherId())));
    }

    private void validate(Resolved value) {
        requireYearEditable(value.cls());
        if (value.teacher().getUser().getStatus() != UserStatus.ACTIVE) {
            throw new ConflictException("Giáo viên đã bị khóa");
        }
        if (!value.teacher().getSubjects().contains(value.subject())) {
            throw new BadRequestException("Giáo viên không phụ trách môn học này");
        }
        if (!yearSubjectRepository.existsByAcademicYearIdAndSubjectId(
                value.cls().getAcademicYear().getId(), value.subject().getId())) {
            throw new ConflictException("Môn học chưa được áp dụng cho năm học");
        }
    }

    private void requireYearEditable(SchoolClass cls) {
        if (cls.getAcademicYear().getStatus() == AcademicYearStatus.COMPLETED) {
            throw new ConflictException("Không được sửa phân công của năm học đã kết thúc");
        }
    }

    private void apply(TeachingAssignment target, Resolved value, LocalDate effectiveFrom) {
        target.setCls(value.cls());
        target.setSubject(value.subject());
        target.setTeacher(value.teacher());
        target.setEffectiveFrom(effectiveFrom != null ? effectiveFrom : value.cls().getAcademicYear().getStartDate());
    }

    private TeachingAssignment find(Long id) {
        return assignmentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("TeachingAssignment", "id", id));
    }

    private TeachingAssignmentDto toDto(TeachingAssignment item) {
        return new TeachingAssignmentDto(item.getId(), item.getCls().getId(), item.getCls().getName(), item.getCls().getGradeLevel(),
            item.getSubject().getId(), item.getSubject().getName(), item.getSubject().getCode(),
            item.getTeacher().getId(), item.getTeacher().getUser().getName(), item.getTeacher().getEmployeeCode(),
            item.getEffectiveFrom(), item.getEffectiveTo(), item.getStatus());
    }

    private ScheduleDto toScheduleDto(Schedule item) {
        TeachingAssignment assignment = item.getAssignment();
        Timetable timetable = item.getTimetable();
        return new ScheduleDto(item.getId(), timetable.getId(), timetable.getVersion(), assignment.getId(),
            assignment.getCls().getId(), assignment.getCls().getName(),
            assignment.getSubject().getId(), assignment.getSubject().getName(), assignment.getSubject().getCode(),
            assignment.getTeacher().getId(), assignment.getTeacher().getUser().getName(),
            timetable.getSemester().getId(), timetable.getSemester().getName(),
            item.getDayOfWeek(), dayName(item.getDayOfWeek()), item.getPeriod(), item.getRoom(), item.getShift());
    }

    private String dayName(Integer day) {
        return switch (day) {
            case 1 -> "Chủ nhật"; case 2 -> "Thứ 2"; case 3 -> "Thứ 3"; case 4 -> "Thứ 4";
            case 5 -> "Thứ 5"; case 6 -> "Thứ 6"; case 7 -> "Thứ 7"; default -> "";
        };
    }

    private record Resolved(SchoolClass cls, Subject subject, Teacher teacher) {}
}
