package vn.edu.fpt.myfschool.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.controller.entity.*;
import vn.edu.fpt.myfschool.repository.*;
import vn.edu.fpt.myfschool.service.GradeBookService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service("gradeBookService")
@RequiredArgsConstructor
@Transactional
public class GradeBookServiceImpl implements GradeBookService {

    private final GradeBookRepository gradeBookRepository;
    private final GradeItemRepository gradeItemRepository;
    private final StudentScoreRepository studentScoreRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ClassRepository classRepository;
    private final SubjectRepository subjectRepository;
    private final SemesterRepository semesterRepository;
    private final StudentRepository studentRepository;

    @Override
    @Transactional(readOnly = true)
    public GradeBookDto getByClassSubjectSemester(Long classId, Long subjectId, Long semesterId) {
        GradeBook gb = gradeBookRepository.findByClsIdAndSubjectIdAndSemesterId(classId, subjectId, semesterId)
            .orElseThrow(() -> new ResourceNotFoundException("GradeBook not found for classId+subjectId+semesterId"));
        return toDto(gb);
    }

    @Override
    public GradeBookDto getOrCreate(Long classId, Long subjectId, Long semesterId) {
        return gradeBookRepository.findByClsIdAndSubjectIdAndSemesterId(classId, subjectId, semesterId)
            .map(this::toDto)
            .orElseGet(() -> {
                SchoolClass cls = classRepository.findById(classId)
                    .orElseThrow(() -> new ResourceNotFoundException("Class", "id", classId));
                Subject subject = subjectRepository.findById(subjectId)
                    .orElseThrow(() -> new ResourceNotFoundException("Subject", "id", subjectId));
                Semester semester = semesterRepository.findById(semesterId)
                    .orElseThrow(() -> new ResourceNotFoundException("Semester", "id", semesterId));
                GradeBook gb = new GradeBook();
                gb.setCls(cls);
                gb.setSubject(subject);
                gb.setSemester(semester);
                gb = gradeBookRepository.save(gb);
                return toDto(gb);
            });
    }

    @Override
    public GradeItemDto addItem(Long gradeBookId, String name, Integer weight, Integer order) {
        GradeBook gb = gradeBookRepository.findById(gradeBookId)
            .orElseThrow(() -> new ResourceNotFoundException("GradeBook", "id", gradeBookId));
        GradeItem item = new GradeItem();
        item.setGradeBook(gb);
        item.setName(name);
        item.setWeight(weight != null ? weight : 1);
        item.setOrder(order != null ? order : 0);
        item = gradeItemRepository.save(item);
        return new GradeItemDto(item.getId(), item.getName(), item.getWeight(), item.getMaxScore(), item.getOrder());
    }

    @Override
    public List<StudentScoreDto> updateScores(UpdateStudentScoreRequest request) {
        GradeItem item = gradeItemRepository.findById(request.gradeItemId())
            .orElseThrow(() -> new ResourceNotFoundException("GradeItem", "id", request.gradeItemId()));

        List<StudentScoreDto> results = new ArrayList<>();
        for (UpdateScoreEntry entry : request.entries()) {
            StudentScore score = studentScoreRepository
                .findByGradeItemIdAndStudentId(item.getId(), entry.studentId())
                .orElseGet(() -> {
                    StudentScore s = new StudentScore();
                    s.setGradeItem(item);
                    s.setStudent(studentRepository.findById(entry.studentId())
                        .orElseThrow(() -> new ResourceNotFoundException("Student", "id", entry.studentId())));
                    return s;
                });
            score.setScore(entry.score());
            score.setIsGraded(entry.isGraded() != null ? entry.isGraded() : (entry.score() != null));
            score.setNote(entry.note());
            score.setIsCommentBased(entry.isCommentBased() != null ? entry.isCommentBased() : false);
            score.setComment(entry.comment());
            score = studentScoreRepository.save(score);
            results.add(toScoreDto(score));
        }
        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateAverage(Long studentId, Long gradeBookId) {
        List<GradeItem> items = gradeItemRepository.findByGradeBookIdOrderByOrderAsc(gradeBookId);
        BigDecimal weightedSum = BigDecimal.ZERO;
        int totalWeight = 0;
        for (GradeItem item : items) {
            StudentScore score = studentScoreRepository.findByGradeItemIdAndStudentId(item.getId(), studentId)
                .orElse(null);
            if (score != null && score.getScore() != null && score.getIsGraded()) {
                weightedSum = weightedSum.add(score.getScore().multiply(BigDecimal.valueOf(item.getWeight())));
                totalWeight += item.getWeight();
            }
        }
        if (totalWeight == 0) return null;
        return weightedSum.divide(BigDecimal.valueOf(totalWeight), 2, RoundingMode.HALF_UP);
    }

    @Override
    public void finalize(Long gradeBookId) {
        GradeBook gb = gradeBookRepository.findById(gradeBookId)
            .orElseThrow(() -> new ResourceNotFoundException("GradeBook", "id", gradeBookId));
        gb.setIsFinalized(true);
        gradeBookRepository.save(gb);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentScoreDto> getStudentScores(Long gradeBookId) {
        List<GradeItem> items = gradeItemRepository.findByGradeBookIdOrderByOrderAsc(gradeBookId);
        List<Student> students = enrollmentRepository.findActiveStudentsByClassAndYear(
            gradeBookRepository.findById(gradeBookId).orElseThrow().getCls().getId(),
            gradeBookRepository.findById(gradeBookId).orElseThrow().getCls().getAcademicYear().getId());
        List<StudentScoreDto> results = new ArrayList<>();
        for (Student s : students) {
            for (GradeItem item : items) {
                StudentScore score = studentScoreRepository
                    .findByGradeItemIdAndStudentId(item.getId(), s.getId()).orElse(null);
                if (score != null) {
                    results.add(toScoreDto(score));
                }
            }
        }
        return results;
    }

    private GradeBookDto toDto(GradeBook gb) {
        List<GradeItemDto> items = gradeItemRepository.findByGradeBookIdOrderByOrderAsc(gb.getId())
            .stream().map(i -> new GradeItemDto(i.getId(), i.getName(), i.getWeight(), i.getMaxScore(), i.getOrder()))
            .toList();
        return new GradeBookDto(gb.getId(),
            gb.getCls().getId(), gb.getCls().getName(),
            gb.getSubject().getId(), gb.getSubject().getName(),
            gb.getSemester().getId(), gb.getSemester().getName(),
            gb.getIsFinalized(), items);
    }

    private StudentScoreDto toScoreDto(StudentScore s) {
        return new StudentScoreDto(
            s.getId(),
            s.getStudent().getId(), s.getStudent().getUser().getName(), s.getStudent().getStudentCode(),
            s.getGradeItem().getId(),
            s.getScore(), s.getIsGraded(), s.getNote(),
            s.getIsCommentBased(), s.getComment(),
            calculateAverage(s.getStudent().getId(), s.getGradeItem().getGradeBook().getId()));
    }
}