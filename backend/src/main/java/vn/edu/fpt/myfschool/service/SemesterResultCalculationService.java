package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.common.dto.CalculateSemesterResultRequest;
import vn.edu.fpt.myfschool.common.dto.CalculateSemesterResultResponse;

public interface SemesterResultCalculationService {
    CalculateSemesterResultResponse calculate(Long classId, Long semesterId);
    CalculateSemesterResultResponse calculateSchool(Long academicYearId, Long semesterId);
}
