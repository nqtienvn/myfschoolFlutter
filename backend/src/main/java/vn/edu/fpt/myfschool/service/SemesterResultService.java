package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.enums.UserRole;

import java.util.List;

public interface SemesterResultService {
    SemesterResultDto getStudentSemesterResult(Long studentId, Long semesterId,
                                               Long requestUserId, UserRole requestRole);

    ClassRankingDto getClassRanking(Long classId, Long semesterId,
                                    Long requestUserId, UserRole requestRole);

    List<ResultSummaryDto> getResultSummary(Long academicYearId, Long semesterId, Long classId);

    ResultSummaryDto overrideResult(Long studentId, ResultOverrideRequest request, Long adminUserId);

    List<ResultSummaryDto> publishResults(ResultPublishRequest request, Long adminUserId);

    void publishSchoolResults(SchoolSemesterResultRequest request, Long adminUserId);

    void closeSemester(ResultCloseRequest request);
}
