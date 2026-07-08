package vn.edu.fpt.myfschool.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.enums.AcademicYearStatus;
import vn.edu.fpt.myfschool.common.exception.BadRequestException;
import vn.edu.fpt.myfschool.common.exception.ConflictException;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.controller.entity.AcademicYear;
import vn.edu.fpt.myfschool.controller.entity.ClassSubject;
import vn.edu.fpt.myfschool.controller.entity.SchoolClass;
import vn.edu.fpt.myfschool.controller.entity.Student;
import vn.edu.fpt.myfschool.controller.entity.Subject;
import vn.edu.fpt.myfschool.controller.entity.Teacher;
import vn.edu.fpt.myfschool.mapper.ClassMapper;
import vn.edu.fpt.myfschool.repository.*;
import vn.edu.fpt.myfschool.service.AcademicYearService;
import vn.edu.fpt.myfschool.service.ClassService;

import java.util.List;
import java.util.stream.Collectors;

@Service("classService")
@RequiredArgsConstructor
@Transactional
public class ClassServiceImpl implements ClassService {

    private final ClassRepository classRepository;
    private final ClassSubjectRepository classSubjectRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final TeacherRepository teacherRepository;
    private final SubjectRepository subjectRepository;
    private final SemesterRepository semesterRepository;
    private final AcademicYearRepository academicYearRepository;
    private final AcademicYearService academicYearService;
    private final ClassMapper classMapper;

    @Transactional(readOnly = true)
    @Override
    public Page<ClassDto> listClasses(Long academicYearId, String keyword, int page, int size) {
        Long yearId = resolveAcademicYearId(academicYearId);
        List<SchoolClass> classes;
        if (keyword != null && !keyword.isBlank()) {
            classes = classRepository.searchByYearAndKeyword(yearId, keyword.trim());
        } else {
            classes = classRepository.findByAcademicYearId(yearId);
        }
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, size);
        int start = Math.min(safePage * safeSize, classes.size());
        int end = Math.min(start + safeSize, classes.size());
        List<ClassDto> dtos = classes.subList(start, end).stream()
            .map(classMapper::toDto).collect(Collectors.toList());
        return new PageImpl<>(dtos, PageRequest.of(safePage, safeSize), classes.size());
    }

    @Transactional(readOnly = true)
    @Override
    public ClassDetailDto getClassDetail(Long classId) {
        SchoolClass cls = classRepository.findById(classId)
            .orElseThrow(() -> new ResourceNotFoundException("Class", "id", classId));
        List<StudentSummaryDto> students = enrollmentRepository.findActiveStudentsByClassAndYear(classId, cls.getAcademicYear().getId())
            .stream().map(s -> new StudentSummaryDto(
                s.getId(), s.getUser().getName(), s.getStudentCode(),
                cls.getName(), s.getUser().getAvatar()))
            .collect(Collectors.toList());
        List<ClassSubjectDto> subjects = classSubjectRepository.findByClsIdAndAcademicYear(classId, cls.getAcademicYear().getName())
            .stream().map(cs -> new ClassSubjectDto(
                cs.getId(),
                new SubjectDto(cs.getSubject().getId(), cs.getSubject().getName(), cs.getSubject().getCode()),
                new TeacherSummaryDto(cs.getTeacher().getId(), cs.getTeacher().getUser().getName(),
                    cs.getTeacher().getEmployeeCode(), cs.getTeacher().getDepartment(), cs.getTeacher().getUser().getAvatar()),
                cs.getIsHomeroom()))
            .collect(Collectors.toList());
        return new ClassDetailDto(cls.getId(), cls.getName(), cls.getGradeLevel(),
            cls.getAcademicYear().getId(), cls.getAcademicYear().getName(), cls.getSchoolName(), students, subjects);
    }

    @Override
    public ClassDto createClass(CreateClassRequest request) {
        AcademicYear year = academicYearService.findEntity(request.academicYearId());
        if (classRepository.existsByNameAndAcademicYearId(request.name(), year.getId())) {
            throw new ConflictException("Lớp đã tồn tại trong năm học này");
        }
        SchoolClass cls = new SchoolClass();
        cls.setName(request.name());
        cls.setGradeLevel(request.gradeLevel());
        cls.setAcademicYear(year);
        cls.setSchoolName(request.schoolName() != null ? request.schoolName() : "FPT Schools");
        return classMapper.toDto(classRepository.save(cls));
    }

    @Override
    public ClassDto updateClass(Long classId, CreateClassRequest request) {
        SchoolClass cls = classRepository.findById(classId)
            .orElseThrow(() -> new ResourceNotFoundException("Class", "id", classId));
        AcademicYear year = academicYearService.findEntity(request.academicYearId());
        classRepository.findByNameAndAcademicYearId(request.name(), year.getId())
            .filter(existing -> !existing.getId().equals(classId))
            .ifPresent(existing -> { throw new ConflictException("Lớp đã tồn tại trong năm học này"); });
        cls.setName(request.name());
        cls.setGradeLevel(request.gradeLevel());
        cls.setAcademicYear(year);
        if (request.schoolName() != null) cls.setSchoolName(request.schoolName());
        return classMapper.toDto(classRepository.save(cls));
    }

    @Override
    public void deleteClass(Long classId) {
        SchoolClass cls = classRepository.findById(classId)
            .orElseThrow(() -> new ResourceNotFoundException("Class", "id", classId));
        if (!enrollmentRepository.findActiveStudentsByClassAndYear(classId, cls.getAcademicYear().getId()).isEmpty()) {
            throw new BadRequestException("Không thể xóa lớp có học sinh");
        }
        classRepository.delete(cls);
    }

    @Transactional(readOnly = true)
    @Override
    public List<StudentSummaryDto> getStudentsInClass(Long classId) {
        SchoolClass cls = classRepository.findById(classId)
            .orElseThrow(() -> new ResourceNotFoundException("Class", "id", classId));
        return enrollmentRepository.findActiveStudentsByClassAndYear(classId, cls.getAcademicYear().getId())
            .stream().map(s -> new StudentSummaryDto(
                s.getId(), s.getUser().getName(), s.getStudentCode(),
                cls.getName(), s.getUser().getAvatar()))
            .collect(Collectors.toList());
    }

    @Override
    public ClassSubjectDto assignSubject(CreateClassSubjectRequest request) {
        SchoolClass cls = classRepository.findById(request.classId())
            .orElseThrow(() -> new ResourceNotFoundException("Class", "id", request.classId()));
        Subject subject = subjectRepository.findById(request.subjectId())
            .orElseThrow(() -> new ResourceNotFoundException("Subject", "id", request.subjectId()));
        Teacher teacher = teacherRepository.findById(request.teacherId())
            .orElseThrow(() -> new ResourceNotFoundException("Teacher", "id", request.teacherId()));

        if (classSubjectRepository.findByClsIdAndSubjectIdAndAcademicYear(
                request.classId(), request.subjectId(), request.academicYear()).isPresent()) {
            throw new ConflictException("Môn học đã được phân công cho lớp này");
        }

        ClassSubject cs = new ClassSubject();
        cs.setCls(cls);
        cs.setSubject(subject);
        cs.setTeacher(teacher);
        cs.setIsHomeroom(request.isHomeroom() != null ? request.isHomeroom() : false);
        cs.setAcademicYear(request.academicYear());
        if (request.semesterId() != null) {
            cs.setSemester(semesterRepository.findById(request.semesterId()).orElse(null));
        }
        cs = classSubjectRepository.save(cs);

        return new ClassSubjectDto(cs.getId(),
            new SubjectDto(subject.getId(), subject.getName(), subject.getCode()),
            new TeacherSummaryDto(teacher.getId(), teacher.getUser().getName(),
                teacher.getEmployeeCode(), teacher.getDepartment(), teacher.getUser().getAvatar()),
            cs.getIsHomeroom());
    }

    @Override
    public void removeSubject(Long classSubjectId) {
        ClassSubject cs = classSubjectRepository.findById(classSubjectId)
            .orElseThrow(() -> new ResourceNotFoundException("ClassSubject", "id", classSubjectId));
        classSubjectRepository.delete(cs);
    }

    private Long resolveAcademicYearId(Long academicYearId) {
        if (academicYearId != null) return academicYearId;
        return academicYearRepository.findByStatus(AcademicYearStatus.ACTIVE).stream()
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("AcademicYear", "status", AcademicYearStatus.ACTIVE))
            .getId();
    }
}
