package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.enums.UserRole;

public interface SemesterResultService {
    SemesterResultDto getStudentSemesterResult(Long studentId, Long semesterId,
                                               Long requestUserId, UserRole requestRole);

    ClassRankingDto getClassRanking(Long classId, Long semesterId,
                                    Long requestUserId, UserRole requestRole);
}
