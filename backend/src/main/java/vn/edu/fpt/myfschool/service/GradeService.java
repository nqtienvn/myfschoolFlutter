package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.enums.UserRole;

import java.util.List;

public interface GradeService {
    StudentSemesterGradesDto getStudentGrades(Long studentId, Long semesterId);

    SubjectGradesDto getSubjectGrades(Long subjectId, Long semesterId, Long classId);

    GradeDto updateGrade(UpdateGradeRequest request);

    List<GradeDto> batchUpdateGrades(BatchGradeUpdateRequest request);

    SimulationResultDto simulateGrades(GradeSimulationRequest request, Long userId, UserRole role);
}
