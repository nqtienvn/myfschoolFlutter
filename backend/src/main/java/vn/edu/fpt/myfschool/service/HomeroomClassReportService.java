package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.common.dto.ClassSummaryDto;
import vn.edu.fpt.myfschool.common.enums.UserRole;

import java.util.List;

public interface HomeroomClassReportService {
    List<ClassSummaryDto> getSummaries(Long academicYearId, Long semesterId, Long classId,
            Integer gradeLevel, Long requesterId, UserRole requesterRole);
}
