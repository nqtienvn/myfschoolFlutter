package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.enums.RiskStatus;
import vn.edu.fpt.myfschool.common.enums.UserRole;

import java.time.LocalDate;
import java.util.List;

public interface StudentRiskService {
    StudentRiskConfigDto getConfig(Long academicYearId);
    StudentRiskConfigDto updateConfig(UpdateStudentRiskConfigRequest request);
    List<StudentRiskFlagDto> getRisks(Long academicYearId, Long semesterId, Long classId,
            RiskStatus status, Long requesterId, UserRole requesterRole);
    StudentRiskFlagDto acknowledge(Long id, Long requesterId, UserRole requesterRole);
    StudentRiskFlagDto resolve(Long id, Long requesterId, UserRole requesterRole);
    List<StudentRiskFlagDto> recalculateClass(Long academicYearId, Long semesterId, Long classId);
    void recalculateForDate(Long academicYearId, Long classId, LocalDate date);
}
