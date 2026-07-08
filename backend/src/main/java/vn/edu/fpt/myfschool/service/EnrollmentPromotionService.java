package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.common.dto.PromoteResponse;

public interface EnrollmentPromotionService {
    PromoteResponse promoteAll(Long fromAcademicYearId, Long toAcademicYearId);
}
