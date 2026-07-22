package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.common.dto.*;
import java.math.BigDecimal;
import java.util.List;

public interface GradeBookService {
    GradeBookDto getByClassSubjectSemester(Long classId, Long subjectId, Long semesterId);
    GradeBookDto getOrCreate(Long classId, Long subjectId, Long semesterId);
    List<StudentScoreDto> updateScores(UpdateStudentScoreRequest request);
    BigDecimal calculateAverage(Long studentId, Long gradeBookId);
    void changeStatus(Long gradeBookId, vn.edu.fpt.myfschool.common.enums.GradeBookStatus status);
    List<StudentScoreDto> getStudentScores(Long gradeBookId);
    List<GradeCalculationDto> calculateSubjectAverages(Long gradeBookId);
    GradeComponentOverviewDto getComponentOverview(Long academicYearId, Long semesterId, Long classId, Long subjectId);
}
