package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.common.dto.AcademicYearMasterDataConfigDto;
import vn.edu.fpt.myfschool.common.dto.UpdateAcademicYearMasterDataRequest;

public interface AcademicYearMasterDataService {
    AcademicYearMasterDataConfigDto get(Long academicYearId);
    AcademicYearMasterDataConfigDto update(Long academicYearId, UpdateAcademicYearMasterDataRequest request);
}
