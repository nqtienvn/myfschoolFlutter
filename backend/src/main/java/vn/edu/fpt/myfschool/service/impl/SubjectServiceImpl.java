package vn.edu.fpt.myfschool.service.impl;

import vn.edu.fpt.myfschool.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.exception.ConflictException;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.entity.Subject;
import vn.edu.fpt.myfschool.mapper.SubjectMapper;
import vn.edu.fpt.myfschool.repository.ClassSubjectRepository;
import vn.edu.fpt.myfschool.repository.SubjectRepository;
import java.util.List;
import java.util.stream.Collectors;

@Service("subjectService")
@RequiredArgsConstructor
@Transactional
public class SubjectServiceImpl implements SubjectService {

    private final SubjectRepository subjectRepository;
    private final ClassSubjectRepository classSubjectRepository;
    private final SubjectMapper subjectMapper;

    @Transactional(readOnly = true)
    @Override
    public List<SubjectDto> listSubjects(String keyword) {
        List<Subject> subjects;
        if (keyword != null && !keyword.isBlank()) {
            subjects = subjectRepository.search(keyword);
        } else {
            subjects = subjectRepository.findAll();
        }
        return subjects.stream().map(subjectMapper::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Override
    public SubjectDto getSubject(Long id) {
        Subject subject = subjectRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Subject", "id", id));
        return subjectMapper.toDto(subject);
    }

    @Override
    public SubjectDto createSubject(CreateSubjectRequest request) {
        if (subjectRepository.existsByCode(request.code())) {
            throw new ConflictException("Mã môn học đã tồn tại");
        }
        Subject subject = new Subject();
        subject.setName(request.name());
        subject.setCode(request.code());
        return subjectMapper.toDto(subjectRepository.save(subject));
    }

    @Override
    public SubjectDto updateSubject(Long id, CreateSubjectRequest request) {
        Subject subject = subjectRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Subject", "id", id));
        subject.setName(request.name());
        subject.setCode(request.code());
        return subjectMapper.toDto(subjectRepository.save(subject));
    }

    @Override
    public void deleteSubject(Long id) {
        Subject subject = subjectRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Subject", "id", id));
        subjectRepository.delete(subject);
    }

    @Transactional(readOnly = true)
    @Override
    public List<ClassSubjectDto> getSubjectsForClass(Long classId, String academicYear) {
        return classSubjectRepository.findByClsIdAndAcademicYear(classId, academicYear)
            .stream().map(cs -> new ClassSubjectDto(
                cs.getId(),
                new SubjectDto(cs.getSubject().getId(), cs.getSubject().getName(), cs.getSubject().getCode()),
                new TeacherSummaryDto(cs.getTeacher().getId(), cs.getTeacher().getUser().getName(),
                    cs.getTeacher().getEmployeeCode(), cs.getTeacher().getDepartment(), cs.getTeacher().getUser().getAvatar()),
                cs.getIsHomeroom()))
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Override
    public List<SubjectDto> getSubjectsForTeacher(Long teacherId, String academicYear) {
        return classSubjectRepository.findSubjectsByTeacher(teacherId, academicYear)
            .stream().map(subjectMapper::toDto).collect(Collectors.toList());
    }
}
