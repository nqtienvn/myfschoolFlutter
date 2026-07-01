package vn.edu.fpt.myfschool.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.exception.BadRequestException;
import vn.edu.fpt.myfschool.common.exception.ConflictException;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.entity.*;
import vn.edu.fpt.myfschool.mapper.ClassMapper;
import vn.edu.fpt.myfschool.repository.*;
import java.util.List;
import java.util.stream.Collectors;

public interface ClassService {
    Page<ClassDto> listClasses(String academicYear, String keyword, int page, int size);

    ClassDetailDto getClassDetail(Long classId);

    ClassDto createClass(CreateClassRequest request);

    ClassDto updateClass(Long classId, CreateClassRequest request);

    void deleteClass(Long classId);

    List<StudentSummaryDto> getStudentsInClass(Long classId);

    ClassSubjectDto assignSubject(CreateClassSubjectRequest request);

    void removeSubject(Long classSubjectId);
}
