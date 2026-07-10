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

import java.util.Comparator;
import java.util.LinkedHashMap;
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
        return (semesterId == null ? distinctYearAssignments(items) : items).stream().map(this::toDto).toList();
    }

    @Override @Transactional(readOnly = true)
    public List<TeachingAssignmentDto> listByTeacher(Long teacherId, Long semesterId, AssignmentStatus status) {
        List<TeachingAssignment> items = semesterId == null
            ? assignmentRepository.findByTeacherIdAndStatus(teacherId, status)
            : assignmentRepository.findByTeacherIdAndSemesterIdAndStatus(teacherId, semesterId, status);
        return (semesterId == null ? distinctYearAssignments(items) : items).stream().map(this::toDto).toList();
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
        if (request.semesterId() == null) return saveYearAssignment(request, resolved, null);
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
        if (request.semesterId() == null) {
            if (!assignment.getCls().getId().equals(request.classId()) || !assignment.getSubject().getId().equals(request.subjectId())) {
                throw new BadRequestException("Không thể đổi lớp hoặc môn của phân công năm học");
            }
            return saveYearAssignment(request, resolved, id);
        }
        if (assignmentRepository.existsByClsIdAndSubjectIdAndSemesterIdAndIdNot(request.classId(), request.subjectId(), request.semesterId(), id)) {
            throw new ConflictException("Lớp đã có phân công cho môn này trong học kỳ");
        }
        apply(assignment, resolved, request);
        return toDto(assignmentRepository.save(assignment));
    }

    @Override
    public TeachingAssignmentDto deactivate(Long id) {
        TeachingAssignment assignment = find(id);
        requireYearEditable(assignment.getCls());
        List<TeachingAssignment> yearItems = assignmentRepository.findByClsIdAndSubjectId(
            assignment.getCls().getId(), assignment.getSubject().getId());
        yearItems.forEach(item -> {
            item.setStatus(AssignmentStatus.INACTIVE);
            item.setEffectiveTo(java.time.LocalDate.now());
        });
        assignmentRepository.saveAll(yearItems);
        return toDto(assignment);
    }

    @Override
    public TeachingAssignmentDto reactivate(Long id) {
        TeachingAssignment assignment = find(id);
        requireYearEditable(assignment.getCls());
        List<TeachingAssignment> yearItems = assignmentRepository.findByClsIdAndSubjectId(
            assignment.getCls().getId(), assignment.getSubject().getId());
        yearItems.forEach(item -> {
            item.setStatus(AssignmentStatus.ACTIVE);
            item.setEffectiveTo(null);
        });
        assignmentRepository.saveAll(yearItems);
        return toDto(assignment);
    }

    private Resolved resolve(CreateTeachingAssignmentRequest request) {
        SchoolClass cls = classRepository.findById(request.classId())
            .orElseThrow(() -> new ResourceNotFoundException("Class", "id", request.classId()));
        return new Resolved(
            cls,
            subjectRepository.findById(request.subjectId()).orElseThrow(() -> new ResourceNotFoundException("Subject", "id", request.subjectId())),
            teacherRepository.findById(request.teacherId()).orElseThrow(() -> new ResourceNotFoundException("Teacher", "id", request.teacherId())),
            request.semesterId() == null ? null : semesterRepository.findById(request.semesterId())
                .orElseThrow(() -> new ResourceNotFoundException("Semester", "id", request.semesterId())));
    }

    private void validate(Resolved value) {
        if (value.semester() != null) {
            requireEditable(value.semester());
            if (!value.cls().getAcademicYear().getId().equals(value.semester().getAcademicYear().getId())) throw new ConflictException("Lớp và học kỳ không cùng năm học");
        } else {
            requireYearEditable(value.cls());
        }
        if (value.teacher().getUser().getStatus() != UserStatus.ACTIVE) throw new ConflictException("Giáo viên đã bị khóa");
        if (!value.teacher().getSubjects().contains(value.subject())) throw new BadRequestException("Giáo viên không phụ trách môn học này");
        if (!yearSubjectRepository.existsByAcademicYearIdAndSubjectId(value.cls().getAcademicYear().getId(), value.subject().getId())) {
            throw new ConflictException("Môn học chưa được áp dụng cho năm học");
        }
    }

    private void requireEditable(Semester semester) {
        if (semester.getStatus() == SemesterStatus.COMPLETED) throw new ConflictException("Không được sửa phân công của học kỳ đã kết thúc");
    }

    private void requireYearEditable(SchoolClass cls) {
        if (cls.getAcademicYear().getStatus() == vn.edu.fpt.myfschool.common.enums.AcademicYearStatus.COMPLETED) {
            throw new ConflictException("Không được sửa phân công của năm học đã kết thúc");
        }
    }

    private void apply(TeachingAssignment target, Resolved value, CreateTeachingAssignmentRequest request) {
        target.setCls(value.cls()); target.setSubject(value.subject()); target.setTeacher(value.teacher()); target.setSemester(value.semester());
        target.setEffectiveFrom(request.effectiveFrom() != null ? request.effectiveFrom() :
            (value.semester() != null ? value.semester().getStartDate() : value.cls().getAcademicYear().getStartDate()));
    }

    private TeachingAssignmentDto saveYearAssignment(CreateTeachingAssignmentRequest request, Resolved resolved, Long updateId) {
        List<Semester> semesters = semesterRepository.findByAcademicYearIdOrderByOrderAsc(resolved.cls().getAcademicYear().getId());
        if (semesters.isEmpty()) throw new ConflictException("Năm học chưa có học kỳ");
        List<TeachingAssignment> saved = new java.util.ArrayList<>();
        for (Semester semester : semesters) {
            TeachingAssignment item = assignmentRepository
                .findByClsIdAndSubjectIdAndSemesterId(request.classId(), request.subjectId(), semester.getId())
                .orElseGet(TeachingAssignment::new);
            if (updateId == null && item.getId() != null && item.getStatus() == AssignmentStatus.ACTIVE
                    && !item.getTeacher().getId().equals(request.teacherId())) {
                throw new ConflictException("Lớp đã có giáo viên phụ trách môn này trong năm học");
            }
            Resolved semesterResolved = new Resolved(resolved.cls(), resolved.subject(), resolved.teacher(), semester);
            apply(item, semesterResolved, request);
            item.setStatus(AssignmentStatus.ACTIVE);
            item.setEffectiveTo(null);
            saved.add(assignmentRepository.save(item));
        }
        return toDto(saved.getFirst());
    }

    private List<TeachingAssignment> distinctYearAssignments(List<TeachingAssignment> items) {
        LinkedHashMap<String, TeachingAssignment> distinct = new LinkedHashMap<>();
        items.stream().sorted(Comparator.comparing(TeachingAssignment::getId)).forEach(item ->
            distinct.putIfAbsent(item.getCls().getId() + "-" + item.getSubject().getId(), item));
        return List.copyOf(distinct.values());
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
