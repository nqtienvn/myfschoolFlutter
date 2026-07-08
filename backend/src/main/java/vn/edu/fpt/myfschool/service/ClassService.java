package vn.edu.fpt.myfschool.service;

import org.springframework.data.domain.Page;
import vn.edu.fpt.myfschool.common.dto.*;

import java.util.List;

public interface ClassService {
    Page<ClassDto> listClasses(Long academicYearId, String keyword, int page, int size);

    ClassDetailDto getClassDetail(Long classId);

    ClassDto createClass(CreateClassRequest request);

    ClassDto updateClass(Long classId, CreateClassRequest request);

    void deleteClass(Long classId);

    List<StudentSummaryDto> getStudentsInClass(Long classId);

    ClassSubjectDto assignSubject(CreateClassSubjectRequest request);

    void removeSubject(Long classSubjectId);
}
