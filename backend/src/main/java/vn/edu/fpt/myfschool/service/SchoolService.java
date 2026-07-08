package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.common.dto.CreateSchoolRequest;
import vn.edu.fpt.myfschool.common.dto.SchoolDto;

import java.util.List;

public interface SchoolService {
    List<SchoolDto> listSchools();
    SchoolDto createSchool(CreateSchoolRequest request);
    SchoolDto updateSchool(Long id, CreateSchoolRequest request);
    void deleteSchool(Long id);
}
