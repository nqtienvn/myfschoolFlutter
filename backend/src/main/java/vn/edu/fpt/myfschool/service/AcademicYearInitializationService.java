package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.common.dto.InitializeAcademicYearResponse;

public interface AcademicYearInitializationService {
    InitializeAcademicYearResponse initialize(Long newAcademicYearId, Long fromAcademicYearId);
}