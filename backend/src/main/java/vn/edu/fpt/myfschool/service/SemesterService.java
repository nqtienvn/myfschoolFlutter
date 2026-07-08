package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.common.dto.*;

import java.util.List;

public interface SemesterService {
    List<SemesterDto> listSemesters(String academicYear);

    SemesterDto getCurrentSemester();

    SemesterDto getSemester(Long id);

    SemesterDto createSemester(CreateSemesterRequest request);

    SemesterDto updateSemester(Long id, CreateSemesterRequest request);

    void setCurrentSemester(Long semesterId);

    void deleteSemester(Long id);
}
