package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.common.dto.AcademicYearReadinessDto;

public interface AcademicYearReadinessService {
    AcademicYearReadinessDto check(Long academicYearId);
    void requireReady(Long academicYearId);
}
