package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.common.dto.*;
import java.math.BigDecimal;
import java.util.List;

public interface GradeBookService {
    GradeBookDto getByClassSubjectSemester(Long classId, Long subjectId, Long semesterId);
    GradeBookDto getOrCreate(Long classId, Long subjectId, Long semesterId);
    GradeItemDto addItem(Long gradeBookId, String name, Integer weight, Integer order);
    List<StudentScoreDto> updateScores(UpdateStudentScoreRequest request);
    BigDecimal calculateAverage(Long studentId, Long gradeBookId);
    void finalize(Long gradeBookId);
    List<StudentScoreDto> getStudentScores(Long gradeBookId);
}