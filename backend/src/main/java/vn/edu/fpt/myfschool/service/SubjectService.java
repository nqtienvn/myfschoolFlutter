package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.exception.ConflictException;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.entity.Subject;
import vn.edu.fpt.myfschool.mapper.SubjectMapper;
import vn.edu.fpt.myfschool.repository.ClassSubjectRepository;
import vn.edu.fpt.myfschool.repository.SubjectRepository;
import java.util.List;
import java.util.stream.Collectors;

public interface SubjectService {
    List<SubjectDto> listSubjects(String keyword);

    SubjectDto getSubject(Long id);

    SubjectDto createSubject(CreateSubjectRequest request);

    SubjectDto updateSubject(Long id, CreateSubjectRequest request);

    void deleteSubject(Long id);

    List<ClassSubjectDto> getSubjectsForClass(Long classId, String academicYear);

    List<SubjectDto> getSubjectsForTeacher(Long teacherId, String academicYear);
}
