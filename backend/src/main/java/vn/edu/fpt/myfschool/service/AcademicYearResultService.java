package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.common.dto.AcademicYearResultDto;
import vn.edu.fpt.myfschool.common.dto.AcademicYearResultRequest;

import java.util.List;

public interface AcademicYearResultService {
    List<AcademicYearResultDto> getResults(Long academicYearId, Long classId);
    List<AcademicYearResultDto> calculate(AcademicYearResultRequest request);
    List<AcademicYearResultDto> publish(AcademicYearResultRequest request);
}
