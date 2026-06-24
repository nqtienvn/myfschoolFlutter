package vn.edu.fpt.myfschool.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.exception.BadRequestException;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.common.util.GradeCalculator;
import vn.edu.fpt.myfschool.entity.*;
import vn.edu.fpt.myfschool.repository.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class GradeService {

    private final GradeRepository gradeRepository;
    private final SemesterResultRepository semesterResultRepository;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final SemesterRepository semesterRepository;
    private final ClassSubjectRepository classSubjectRepository;
    private final ClassRepository classRepository;
    private final AttendanceRepository attendanceRepository;

    @Transactional(readOnly = true)
    public StudentSemesterGradesDto getStudentGrades(Long studentId, Long semesterId) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("Student", "id", studentId));
        Semester semester = semesterRepository.findById(semesterId)
            .orElseThrow(() -> new ResourceNotFoundException("Semester", "id", semesterId));

        List<Grade> grades = gradeRepository.findByStudentIdAndSemesterId(studentId, semesterId);
        List<GradeDto> gradeDtos = grades.stream().map(this::toGradeDto).collect(Collectors.toList());

        SemesterResult sr = semesterResultRepository.findByStudentIdAndSemesterId(studentId, semesterId).orElse(null);
        SemesterResultDto summary = sr != null ? toResultDto(sr, student, semester) : null;

        return new StudentSemesterGradesDto(
            studentId, student.getUser().getName(), student.getStudentCode(),
            semesterId, semester.getName(), semester.getAcademicYear(),
            gradeDtos, summary);
    }

    @Transactional(readOnly = true)
    public SubjectGradesDto getSubjectGrades(Long subjectId, Long semesterId, Long classId) {
        Subject subject = subjectRepository.findById(subjectId)
            .orElseThrow(() -> new ResourceNotFoundException("Subject", "id", subjectId));
        Semester semester = semesterRepository.findById(semesterId)
            .orElseThrow(() -> new ResourceNotFoundException("Semester", "id", semesterId));
        SchoolClass cls = classRepository.findById(classId)
            .orElseThrow(() -> new ResourceNotFoundException("Class", "id", classId));

        List<Student> students = studentRepository.findByCurrentClassId(classId);
        List<Grade> grades = gradeRepository.findBySubjectSemesterClass(subjectId, semesterId, classId);

        List<StudentGradeRowDto> rows = students.stream().map(s -> {
            Grade g = grades.stream().filter(gr -> gr.getStudent().getId().equals(s.getId())).findFirst().orElse(null);
            return new StudentGradeRowDto(
                s.getId(), s.getUser().getName(), s.getStudentCode(),
                g != null ? g.getOral() : null, g != null ? g.getQuiz15m() : null,
                g != null ? g.getMidTerm() : null, g != null ? g.getFinalScore() : null,
                g != null ? g.getAverage() : null);
        }).collect(Collectors.toList());

        return new SubjectGradesDto(subjectId, subject.getName(), subject.getCode(),
            semesterId, semester.getName(), classId, cls.getName(), rows);
    }

    public GradeDto updateGrade(UpdateGradeRequest request) {
        Student student = studentRepository.findById(request.studentId())
            .orElseThrow(() -> new ResourceNotFoundException("Student", "id", request.studentId()));
        Subject subject = subjectRepository.findById(request.subjectId())
            .orElseThrow(() -> new ResourceNotFoundException("Subject", "id", request.subjectId()));
        Semester semester = semesterRepository.findById(request.semesterId())
            .orElseThrow(() -> new ResourceNotFoundException("Semester", "id", request.semesterId()));

        Grade grade = gradeRepository.findByStudentIdAndSubjectIdAndSemesterId(
            request.studentId(), request.subjectId(), request.semesterId())
            .orElseGet(() -> {
                Grade g = new Grade();
                g.setStudent(student);
                g.setSubject(subject);
                g.setSemester(semester);
                return g;
            });

        grade.setOral(request.oral());
        grade.setQuiz15m(request.quiz15m());
        grade.setMidTerm(request.midTerm());
        grade.setFinalScore(request.finalScore());
        grade.setAverage(GradeCalculator.calculateAverage(request.oral(), request.quiz15m(), request.midTerm(), request.finalScore()));
        grade = gradeRepository.save(grade);

        recalculateSemesterResult(request.studentId(), request.semesterId());
        return toGradeDto(grade);
    }

    public List<GradeDto> batchUpdateGrades(BatchGradeUpdateRequest request) {
        List<GradeDto> results = new ArrayList<>();
        for (GradeEntry entry : request.grades()) {
            results.add(updateGrade(new UpdateGradeRequest(
                entry.studentId(), request.subjectId(), request.semesterId(),
                entry.oral(), entry.quiz15m(), entry.midTerm(), entry.finalScore())));
        }
        return results;
    }

    public SimulationResultDto simulateGrades(Long studentId, GradeSimulationRequest request) {
        List<Grade> existingGrades = gradeRepository.findByStudentIdAndSemesterId(studentId, request.semesterId());
        List<GradeDto> simulated = new ArrayList<>();

        for (SimulationEntry sim : request.simulations()) {
            BigDecimal avg = GradeCalculator.calculateAverage(sim.oral(), sim.quiz15m(), sim.midTerm(), sim.finalScore());
            Subject subject = subjectRepository.findById(sim.subjectId()).orElse(null);
            simulated.add(new GradeDto(null, sim.subjectId(),
                subject != null ? subject.getName() : null, subject != null ? subject.getCode() : null,
                sim.oral(), sim.quiz15m(), sim.midTerm(), sim.finalScore(), avg));
        }

        BigDecimal gpa = simulated.stream().map(GradeDto::average).filter(a -> a != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(Math.max(simulated.stream().map(GradeDto::average).filter(a -> a != null).count(), 1)), 2, java.math.RoundingMode.HALF_UP);

        return new SimulationResultDto(simulated, gpa,
            GradeCalculator.getAcademicAbility(gpa), null, null);
    }

    private void recalculateSemesterResult(Long studentId, Long semesterId) {
        Student student = studentRepository.findById(studentId).orElse(null);
        Semester semester = semesterRepository.findById(semesterId).orElse(null);
        if (student == null || semester == null) return;

        List<Grade> grades = gradeRepository.findByStudentIdAndSemesterId(studentId, semesterId);
        BigDecimal gpa = grades.stream().map(Grade::getAverage).filter(a -> a != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(Math.max(grades.stream().map(Grade::getAverage).filter(a -> a != null).count(), 1)), 2, java.math.RoundingMode.HALF_UP);

        SchoolClass cls = student.getCurrentClass();
        if (cls == null) return;

        SemesterResult sr = semesterResultRepository.findByStudentIdAndSemesterId(studentId, semesterId)
            .orElseGet(() -> {
                SemesterResult r = new SemesterResult();
                r.setStudent(student);
                r.setSemester(semester);
                r.setCls(cls);
                return r;
            });

        sr.setGpa(gpa);
        sr.setAcademicAbility(GradeCalculator.getAcademicAbility(gpa));

        List<SemesterResult> allResults = semesterResultRepository.findByClassIdAndSemesterIdOrderByRankAsc(cls.getId(), semesterId);
        int rank = 1;
        for (SemesterResult r : allResults) {
            if (r.getGpa() != null && r.getGpa().compareTo(gpa) > 0) rank++;
        }
        sr.setRank(rank);
        semesterResultRepository.save(sr);
    }

    private GradeDto toGradeDto(Grade g) {
        return new GradeDto(g.getId(), g.getSubject().getId(), g.getSubject().getName(),
            g.getSubject().getCode(), g.getOral(), g.getQuiz15m(), g.getMidTerm(),
            g.getFinalScore(), g.getAverage());
    }

    private SemesterResultDto toResultDto(SemesterResult sr, Student student, Semester semester) {
        return new SemesterResultDto(sr.getId(), student.getId(), student.getUser().getName(),
            semester.getId(), semester.getName(), sr.getCls().getId(), sr.getCls().getName(),
            sr.getGpa(), sr.getRank(), sr.getHonor(), sr.getConduct(), sr.getAcademicAbility());
    }
}
