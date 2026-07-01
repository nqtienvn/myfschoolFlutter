package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.exception.ConflictException;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.entity.Semester;
import vn.edu.fpt.myfschool.mapper.SemesterMapper;
import vn.edu.fpt.myfschool.repository.SemesterRepository;
import java.util.List;
import java.util.stream.Collectors;

public interface SemesterService {
    List<SemesterDto> listSemesters(String academicYear);

    SemesterDto getCurrentSemester();

    SemesterDto getSemester(Long id);

    SemesterDto createSemester(CreateSemesterRequest request);

    SemesterDto updateSemester(Long id, CreateSemesterRequest request);

    void setCurrentSemester(Long semesterId);
}
