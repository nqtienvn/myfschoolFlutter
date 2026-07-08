package vn.edu.fpt.myfschool.service.impl;

import vn.edu.fpt.myfschool.controller.entity.ClassSubject;
import vn.edu.fpt.myfschool.controller.entity.SchoolClass;
import vn.edu.fpt.myfschool.controller.entity.Subject;
import vn.edu.fpt.myfschool.controller.entity.Teacher;
import vn.edu.fpt.myfschool.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.exception.BadRequestException;
import vn.edu.fpt.myfschool.common.exception.ConflictException;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.mapper.ClassMapper;
import vn.edu.fpt.myfschool.repository.*;

import java.util.List;
import java.util.stream.Collectors;

@Service("classService")
@RequiredArgsConstructor
@Transactional
public class ClassServiceImpl implements ClassService {

    private final ClassRepository classRepository;
    private final ClassSubjectRepository classSubjectRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final SubjectRepository subjectRepository;
    private final SemesterRepository semesterRepository;
    private final ClassMapper classMapper;

    @Transactional(readOnly = true)
    @Override
    public Page<ClassDto> listClasses(String academicYear, String keyword, int page, int size) {
        String year = academicYear != null ? academicYear : "2026-2027";
        List<SchoolClass> classes;
        if (keyword != null && !keyword.isBlank()) {
            classes = classRepository.searchByYearAndKeyword(year, keyword);
        } else {
            classes = classRepository.findByAcademicYear(year);
        }
        int start = Math.min(page * size, classes.size());
        int end = Math.min(start + size, classes.size());
        List<ClassDto> dtos = classes.subList(start, end).stream()
            .map(classMapper::toDto).collect(Collectors.toList());
        return new PageImpl<>(dtos, PageRequest.of(page, size), classes.size());
    }

    @Transactional(readOnly = true)
    @Override
    public ClassDetailDto getClassDetail(Long classId) {
        SchoolClass cls = classRepository.findById(classId)
            .orElseThrow(() -> new ResourceNotFoundException("Class", "id", classId));
        List<StudentSummaryDto> students = studentRepository.findByCurrentClassId(classId)
            .stream().map(s -> new StudentSummaryDto(
                s.getId(), s.getUser().getName(), s.getStudentCode(),
                cls.getName(), s.getUser().getAvatar()))
            .collect(Collectors.toList());
        List<ClassSubjectDto> subjects = classSubjectRepository.findByClsIdAndAcademicYear(classId, cls.getAcademicYear())
            .stream().map(cs -> new ClassSubjectDto(
                cs.getId(),
                new SubjectDto(cs.getSubject().getId(), cs.getSubject().getName(), cs.getSubject().getCode()),
                new TeacherSummaryDto(cs.getTeacher().getId(), cs.getTeacher().getUser().getName(),
                    cs.getTeacher().getEmployeeCode(), cs.getTeacher().getDepartment(), cs.getTeacher().getUser().getAvatar()),
                cs.getIsHomeroom()))
            .collect(Collectors.toList());
        return new ClassDetailDto(cls.getId(), cls.getName(), cls.getGradeLevel(),
            cls.getAcademicYear(), cls.getSchoolName(), students, subjects);
    }

    @Override
    public ClassDto createClass(CreateClassRequest request) {
        if (classRepository.existsByNameAndAcademicYear(request.name(), request.academicYear())) {
            throw new ConflictException("Lớp đã tồn tại trong năm học này");
        }
        SchoolClass cls = new SchoolClass();
        cls.setName(request.name());
        cls.setGradeLevel(request.gradeLevel());
        cls.setAcademicYear(request.academicYear());
        cls.setSchoolName(request.schoolName() != null ? request.schoolName() : "FPT Schools");
        return classMapper.toDto(classRepository.save(cls));
    }

    @Override
    public ClassDto updateClass(Long classId, CreateClassRequest request) {
        SchoolClass cls = classRepository.findById(classId)
            .orElseThrow(() -> new ResourceNotFoundException("Class", "id", classId));
        if (request.name() != null) cls.setName(request.name());
        if (request.gradeLevel() != null) cls.setGradeLevel(request.gradeLevel());
        if (request.academicYear() != null) cls.setAcademicYear(request.academicYear());
        if (request.schoolName() != null) cls.setSchoolName(request.schoolName());
        return classMapper.toDto(classRepository.save(cls));
    }

    @Override
    public void deleteClass(Long classId) {
        SchoolClass cls = classRepository.findById(classId)
            .orElseThrow(() -> new ResourceNotFoundException("Class", "id", classId));
        if (!studentRepository.findByCurrentClassId(classId).isEmpty()) {
            throw new BadRequestException("Không thể xóa lớp có học sinh");
        }
        classRepository.delete(cls);
    }

    @Transactional(readOnly = true)
    @Override
    public List<StudentSummaryDto> getStudentsInClass(Long classId) {
        SchoolClass cls = classRepository.findById(classId)
            .orElseThrow(() -> new ResourceNotFoundException("Class", "id", classId));
        return studentRepository.findByCurrentClassId(classId)
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
}
