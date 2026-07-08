package vn.edu.fpt.myfschool.service.impl;

import vn.edu.fpt.myfschool.entity.SchoolClass;
import vn.edu.fpt.myfschool.entity.Semester;
import vn.edu.fpt.myfschool.entity.SemesterResult;
import vn.edu.fpt.myfschool.entity.Student;
import vn.edu.fpt.myfschool.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.repository.*;

import java.util.List;
import java.util.stream.Collectors;

@Service("semesterResultService")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SemesterResultServiceImpl implements SemesterResultService {

    private final SemesterResultRepository semesterResultRepository;
    private final StudentRepository studentRepository;
    private final SemesterRepository semesterRepository;
    private final ClassRepository classRepository;

    @Override
    public SemesterResultDto getStudentSemesterResult(Long studentId, Long semesterId) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("Student", "id", studentId));
        Semester semester = semesterRepository.findById(semesterId)
            .orElseThrow(() -> new ResourceNotFoundException("Semester", "id", semesterId));

        SemesterResult sr = semesterResultRepository.findByStudentIdAndSemesterId(studentId, semesterId)
            .orElse(null);
        if (sr == null) return null;

        return new SemesterResultDto(sr.getId(), student.getId(), student.getUser().getName(),
            semester.getId(), semester.getName(), sr.getCls().getId(), sr.getCls().getName(),
            sr.getGpa(), sr.getRank(), sr.getHonor(), sr.getConduct(), sr.getAcademicAbility());
    }

    @Override
    public ClassRankingDto getClassRanking(Long classId, Long semesterId) {
        SchoolClass cls = classRepository.findById(classId)
            .orElseThrow(() -> new ResourceNotFoundException("Class", "id", classId));
        Semester semester = semesterRepository.findById(semesterId)
            .orElseThrow(() -> new ResourceNotFoundException("Semester", "id", semesterId));

        List<SemesterResult> results = semesterResultRepository.findByClassIdAndSemesterIdOrderByRankAsc(classId, semesterId);
        List<ClassRankEntryDto> rankings = results.stream().map(sr ->
            new ClassRankEntryDto(sr.getRank(), sr.getStudent().getId(),
                sr.getStudent().getUser().getName(), sr.getStudent().getStudentCode(),
                sr.getGpa(), sr.getAcademicAbility(), sr.getConduct())
        ).collect(Collectors.toList());

        return new ClassRankingDto(classId, cls.getName(), semesterId, semester.getName(), rankings);
    }
}
