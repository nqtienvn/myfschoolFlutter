package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.common.dto.*;

import java.util.List;

public interface SubjectService {
    List<SubjectDto> listSubjects(String keyword);

    SubjectDto getSubject(Long id);

    SubjectDto createSubject(CreateSubjectRequest request);

    SubjectDto updateSubject(Long id, CreateSubjectRequest request);

    void deleteSubject(Long id);
}
