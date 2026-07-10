package vn.edu.fpt.myfschool.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.enums.AssignmentStatus;
import vn.edu.fpt.myfschool.common.enums.SemesterStatus;
import vn.edu.fpt.myfschool.common.enums.UserStatus;
import vn.edu.fpt.myfschool.common.exception.BadRequestException;
import vn.edu.fpt.myfschool.common.exception.ConflictException;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.entity.*;
import vn.edu.fpt.myfschool.repository.*;
import vn.edu.fpt.myfschool.service.TeachingAssignmentService;

import java.util.List;

@Service("teachingAssignmentService")
@RequiredArgsConstructor
@Transactional
public class TeachingAssignmentServiceImpl implements TeachingAssignmentService {
    private final TeachingAssignmentRepository assignmentRepository;
    private final ClassRepository classRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;
    private final SemesterRepository semesterRepository;
    private final ScheduleRepository scheduleRepository;
    private final AcademicYearSubjectRepository yearSubjectRepository;

    @Override @Transactional(readOnly = true)
    public List<TeachingAssignmentDto> listByClass(Long classId, Long semesterId, AssignmentStatus status) {
        List<TeachingAssignment> items = semesterId == null
            ? assignmentRepository.findByClsIdAndStatus(classId, status)
            : assignmentRepository.findByClsIdAndSemesterIdAndStatus(classId, semesterId, status);
        return items.stream().map(this::toDto).toList();
    }

    @Override @Transactional(readOnly = true)
    public List<TeachingAssignmentDto> listByTeacher(Long teacherId, Long semesterId, AssignmentStatus status) {
        List<TeachingAssignment> items = semesterId == null
            ? assignmentRepository.findByTeacherIdAndStatus(teacherId, status)
            : assignmentRepository.findByTeacherIdAndSemesterIdAndStatus(teacherId, semesterId, status);
        return items.stream().map(this::toDto).toList();
    }

    @Override @Transactional(readOnly = true) public TeachingAssignmentDto getById(Long id) { return toDto(find(id)); }

    @Override @Transactional(readOnly = true)
    public TeachingAssignmentDetailDto getDetail(Long id) {
        TeachingAssignment assignment = find(id);
        return new TeachingAssignmentDetailDto(toDto(assignment), scheduleRepository.findByAssignmentId(id).stream().map(this::toScheduleDto).toList());
    }

    @Override
    public TeachingAssignmentDto create(CreateTeachingAssignmentRequest request) {
        Resolved resolved = resolve(request);
        validate(resolved);
        TeachingAssignment assignment = assignmentRepository
            .findByClsIdAndSubjectIdAndSemesterId(request.classId(), request.subjectId(), request.semesterId())
            .map(existing -> {
                if (existing.getStatus() == AssignmentStatus.ACTIVE) throw new ConflictException("Lớp đã có phân công cho môn này trong học kỳ");
                return existing;
            }).orElseGet(TeachingAssignment::new);
        apply(assignment, resolved, request);
        assignment.setStatus(AssignmentStatus.ACTIVE);
        assignment.setEffectiveTo(null);
        return toDto(assignmentRepository.save(assignment));
    }

    @Override
    public TeachingAssignmentDto update(Long id, CreateTeachingAssignmentRequest request) {
        TeachingAssignment assignment = find(id);
        Resolved resolved = resolve(request);
        validate(resolved);
        if (assignmentRepository.existsByClsIdAndSubjectIdAndSemesterIdAndIdNot(request.classId(), request.subjectId(), request.semesterId(), id)) {
            throw new ConflictException("Lớp đã có phân công cho môn này trong học kỳ");
        }
        apply(assignment, resolved, request);
        return toDto(assignmentRepository.save(assignment));
    }

    @Override
    public TeachingAssignmentDto deactivate(Long id) {
        TeachingAssignment assignment = find(id);
        requireEditable(assignment.getSemester());
        assignment.setStatus(AssignmentStatus.INACTIVE);
        assignment.setEffectiveTo(java.time.LocalDate.now());
        return toDto(assignmentRepository.save(assignment));
    }

    @Override
    public TeachingAssignmentDto reactivate(Long id) {
        TeachingAssignment assignment = find(id);
        requireEditable(assignment.getSemester());
        assignment.setStatus(AssignmentStatus.ACTIVE);
        assignment.setEffectiveTo(null);
        return toDto(assignmentRepository.save(assignment));
    }

    private Resolved resolve(CreateTeachingAssignmentRequest request) {
        return new Resolved(
            classRepository.findById(request.classId()).orElseThrow(() -> new ResourceNotFoundException("Class", "id", request.classId())),
            subjectRepository.findById(request.subjectId()).orElseThrow(() -> new ResourceNotFoundException("Subject", "id", request.subjectId())),
            teacherRepository.findById(request.teacherId()).orElseThrow(() -> new ResourceNotFoundException("Teacher", "id", request.teacherId())),
            semesterRepository.findById(request.semesterId()).orElseThrow(() -> new ResourceNotFoundException("Semester", "id", request.semesterId())));
    }

    private void validate(Resolved value) {
        requireEditable(value.semester());
        if (!value.cls().getAcademicYear().getId().equals(value.semester().getAcademicYear().getId())) throw new ConflictException("Lớp và học kỳ không cùng năm học");
        if (value.teacher().getUser().getStatus() != UserStatus.ACTIVE) throw new ConflictException("Giáo viên đã bị khóa");
        if (!value.teacher().getSubjects().contains(value.subject())) throw new BadRequestException("Giáo viên không phụ trách môn học này");
        if (!yearSubjectRepository.existsByAcademicYearIdAndSubjectId(value.cls().getAcademicYear().getId(), value.subject().getId())) {
            throw new ConflictException("Môn học chưa được áp dụng cho năm học");
        }
    }

    private void requireEditable(Semester semester) {
        if (semester.getStatus() == SemesterStatus.COMPLETED) throw new ConflictException("Không được sửa phân công của học kỳ đã kết thúc");
    }

    private void apply(TeachingAssignment target, Resolved value, CreateTeachingAssignmentRequest request) {
        target.setCls(value.cls()); target.setSubject(value.subject()); target.setTeacher(value.teacher()); target.setSemester(value.semester()); target.setEffectiveFrom(request.effectiveFrom());
    }

    private TeachingAssignment find(Long id) {
        return assignmentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("TeachingAssignment", "id", id));
    }

    private TeachingAssignmentDto toDto(TeachingAssignment item) {
        return new TeachingAssignmentDto(item.getId(), item.getCls().getId(), item.getCls().getName(), item.getCls().getGradeLevel(),
            item.getSubject().getId(), item.getSubject().getName(), item.getSubject().getCode(), item.getTeacher().getId(), item.getTeacher().getUser().getName(), item.getTeacher().getEmployeeCode(),
            item.getSemester().getId(), item.getSemester().getName(), item.getEffectiveFrom(), item.getEffectiveTo(), item.getStatus());
    }

    private ScheduleDto toScheduleDto(Schedule item) {
        return new ScheduleDto(item.getId(), item.getAssignment().getId(), item.getAssignment().getCls().getId(), item.getAssignment().getCls().getName(),
            item.getAssignment().getSubject().getId(), item.getAssignment().getSubject().getName(), item.getAssignment().getSubject().getCode(),
            item.getAssignment().getTeacher().getId(), item.getAssignment().getTeacher().getUser().getName(), item.getAssignment().getSemester().getId(), item.getAssignment().getSemester().getName(),
            item.getDayOfWeek(), dayName(item.getDayOfWeek()), item.getPeriod(), item.getRoom(), item.getShift());
    }

    private String dayName(Integer day) { return switch (day) { case 2 -> "Thứ 2"; case 3 -> "Thứ 3"; case 4 -> "Thứ 4"; case 5 -> "Thứ 5"; case 6 -> "Thứ 6"; case 7 -> "Thứ 7"; case 8 -> "Chủ nhật"; default -> ""; }; }
    private record Resolved(SchoolClass cls, Subject subject, Teacher teacher, Semester semester) {}
}
