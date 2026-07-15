package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.common.dto.PaymentConfigurationDto;
import vn.edu.fpt.myfschool.common.dto.PaymentConfigurationRequest;

public interface PaymentConfigurationService {

    PaymentConfigurationDto getByAcademicYear(Long academicYearId);

    PaymentConfigurationDto getBySemester(Long semesterId);

    PaymentConfigurationDto upsert(Long academicYearId, PaymentConfigurationRequest request);
}
