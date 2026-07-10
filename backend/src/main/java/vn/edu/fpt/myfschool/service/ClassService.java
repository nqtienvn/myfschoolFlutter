package vn.edu.fpt.myfschool.service;

import org.springframework.data.domain.Page;
import vn.edu.fpt.myfschool.common.dto.*;

import java.util.List;

public interface ClassService {
    Page<ClassDto> listClasses(Long academicYearId, String keyword, int page, int size);

    ClassDetailDto getClassDetail(Long classId);

    List<ClassDto> generateClasses(GenerateClassesRequest request);

    ClassDto updateClass(Long classId, UpdateClassRequest request);

    void deleteClass(Long classId);

    List<StudentSummaryDto> getStudentsInClass(Long classId);
}
