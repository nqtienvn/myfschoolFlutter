package vn.edu.fpt.myfschool.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.enums.AssignmentStatus;
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

    private final TeachingAssignmentRepository teachingAssignmentRepository;
    private final ClassRepository classRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;
    private final SemesterRepository semesterRepository;
    private final ScheduleRepository scheduleRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TeachingAssignmentDto> listByClass(Long classId, Long semesterId, AssignmentStatus status) {
        return teachingAssignmentRepository
            .findByClsIdAndSemesterIdAndStatus(classId, semesterId, status)
            .stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeachingAssignmentDto> listByTeacher(Long teacherId, Long semesterId, AssignmentStatus status) {
        return teachingAssignmentRepository
            .findByTeacherIdAndSemesterIdAndStatus(teacherId, semesterId, status)
            .stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TeachingAssignmentDto getById(Long id) {
        return toDto(findEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public TeachingAssignmentDetailDto getDetail(Long id) {
        TeachingAssignment ta = findEntity(id);
        List<ScheduleDto> schedules = scheduleRepository.findByAssignmentId(id)
            .stream().map(this::toScheduleDto).toList();
        return new TeachingAssignmentDetailDto(toDto(ta), schedules);
    }

    @Override
    public TeachingAssignmentDto create(CreateTeachingAssignmentRequest request) {
        SchoolClass cls = classRepository.findById(request.classId())
            .orElseThrow(() -> new ResourceNotFoundException("Class", "id", request.classId()));
        Subject subject = subjectRepository.findById(request.subjectId())
            .orElseThrow(() -> new ResourceNotFoundException("Subject", "id", request.subjectId()));
        Teacher teacher = teacherRepository.findById(request.teacherId())
            .orElseThrow(() -> new ResourceNotFoundException("Teacher", "id", request.teacherId()));
        Semester semester = semesterRepository.findById(request.semesterId())
            .orElseThrow(() -> new ResourceNotFoundException("Semester", "id", request.semesterId()));

        if (teachingAssignmentRepository.existsByClsIdAndSubjectIdAndSemesterIdAndEffectiveFrom(
                request.classId(), request.subjectId(), request.semesterId(), request.effectiveFrom())) {
            throw new ConflictException("Da co phan cong cho mon nay trong khoang thoi gian nay");
        }

        TeachingAssignment ta = new TeachingAssignment();
        ta.setCls(cls);
        ta.setSubject(subject);
        ta.setTeacher(teacher);
        ta.setSemester(semester);
        ta.setEffectiveFrom(request.effectiveFrom());
        ta.setStatus(AssignmentStatus.ACTIVE);
        ta = teachingAssignmentRepository.save(ta);

        return toDto(ta);
    }

    @Override
    public TeachingAssignmentDto update(Long id, CreateTeachingAssignmentRequest request) {
        TeachingAssignment ta = findEntity(id);
        SchoolClass cls = classRepository.findById(request.classId())
            .orElseThrow(() -> new ResourceNotFoundException("Class", "id", request.classId()));
        Subject subject = subjectRepository.findById(request.subjectId())
            .orElseThrow(() -> new ResourceNotFoundException("Subject", "id", request.subjectId()));
        Teacher teacher = teacherRepository.findById(request.teacherId())
            .orElseThrow(() -> new ResourceNotFoundException("Teacher", "id", request.teacherId()));
        Semester semester = semesterRepository.findById(request.semesterId())
            .orElseThrow(() -> new ResourceNotFoundException("Semester", "id", request.semesterId()));

        ta.setCls(cls);
        ta.setSubject(subject);
        ta.setTeacher(teacher);
        ta.setSemester(semester);
        ta.setEffectiveFrom(request.effectiveFrom());
        ta = teachingAssignmentRepository.save(ta);

        return toDto(ta);
    }

    @Override
    public TeachingAssignmentDto deactivate(Long id) {
        TeachingAssignment ta = findEntity(id);
        ta.setStatus(AssignmentStatus.INACTIVE);
        ta.setEffectiveTo(java.time.LocalDate.now());
        ta = teachingAssignmentRepository.save(ta);
        return toDto(ta);
    }

    @Override
    public TeachingAssignmentDto reactivate(Long id) {
        TeachingAssignment ta = findEntity(id);
        ta.setStatus(AssignmentStatus.ACTIVE);
        ta.setEffectiveTo(null);
        ta = teachingAssignmentRepository.save(ta);
        return toDto(ta);
    }

    private TeachingAssignment findEntity(Long id) {
        return teachingAssignmentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("TeachingAssignment", "id", id));
    }

    private TeachingAssignmentDto toDto(TeachingAssignment ta) {
        return new TeachingAssignmentDto(
            ta.getId(),
            ta.getCls().getId(), ta.getCls().getName(), ta.getCls().getGradeLevel(),
            ta.getSubject().getId(), ta.getSubject().getName(), ta.getSubject().getCode(),
            ta.getTeacher().getId(), ta.getTeacher().getUser().getName(), ta.getTeacher().getEmployeeCode(),
            ta.getSemester().getId(), ta.getSemester().getName(),
            ta.getEffectiveFrom(), ta.getEffectiveTo(),
            ta.getStatus()
        );
    }

    private ScheduleDto toScheduleDto(Schedule s) {
        return new ScheduleDto(
            s.getId(),
            s.getAssignment().getId(),
            s.getAssignment().getCls().getId(), s.getAssignment().getCls().getName(),
            s.getAssignment().getSubject().getId(), s.getAssignment().getSubject().getName(),
            s.getAssignment().getSubject().getCode(),
            s.getAssignment().getTeacher().getId(), s.getAssignment().getTeacher().getUser().getName(),
            s.getAssignment().getSemester().getId(), s.getAssignment().getSemester().getName(),
            s.getDayOfWeek(), getDayName(s.getDayOfWeek()),
            s.getPeriod(), s.getRoom(), s.getShift()
        );
    }

    private String getDayName(Integer dayOfWeek) {
        return switch (dayOfWeek) {
            case 2 -> "Thu 2";
            case 3 -> "Thu 3";
            case 4 -> "Thu 4";
            case 5 -> "Thu 5";
            case 6 -> "Thu 6";
            case 7 -> "Thu 7";
            case 8 -> "Chu nhat";
            default -> "";
        };
    }
}
