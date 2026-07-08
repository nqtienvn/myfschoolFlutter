package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.common.dto.*;

public interface SemesterResultService {
    SemesterResultDto getStudentSemesterResult(Long studentId, Long semesterId);

    ClassRankingDto getClassRanking(Long classId, Long semesterId);
}
