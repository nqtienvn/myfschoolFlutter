package vn.edu.fpt.myfschool.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.CalculateSemesterResultResponse;
import vn.edu.fpt.myfschool.common.enums.AttendanceStatus;
import vn.edu.fpt.myfschool.common.enums.EnrollmentStatus;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.entity.*;
import vn.edu.fpt.myfschool.repository.*;
import vn.edu.fpt.myfschool.service.SemesterResultCalculationService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service("semesterResultCalculationService")
@RequiredArgsConstructor
@Transactional
public class SemesterResultCalculationServiceImpl implements SemesterResultCalculationService {

    private final SemesterResultRepository semesterResultRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final GradeRepository gradeRepository;
    private final AttendanceSessionRepository attendanceSessionRepository;
    private final AttendanceDetailRepository attendanceDetailRepository;
    private final ClassRepository classRepository;
    private final SemesterRepository semesterRepository;
    // Phase E: GradeBook dependencies
    private final GradeBookRepository gradeBookRepository;
    private final GradeItemRepository gradeItemRepository;
    private final StudentScoreRepository studentScoreRepository;

    @Override
    public CalculateSemesterResultResponse calculate(Long classId, Long semesterId) {
        SchoolClass cls = classRepository.findById(classId)
            .orElseThrow(() -> new ResourceNotFoundException("Class", "id", classId));
        Semester semester = semesterRepository.findById(semesterId)
            .orElseThrow(() -> new ResourceNotFoundException("Semester", "id", semesterId));

        List<Enrollment> enrollments = enrollmentRepository
            .findByClsIdAndAcademicYearIdAndStatus(classId, cls.getAcademicYear().getId(), EnrollmentStatus.ACTIVE);

        int processed = 0, updated = 0, skipped = 0;
        List<String> warnings = new ArrayList<>();

        // Preload attendance session IDs for conduct calculation
        List<AttendanceSession> sessions = attendanceSessionRepository
            .findByClsIdAndDateBetween(classId, semester.getStartDate(), semester.getEndDate());
        List<Long> sessionIds = sessions.stream().map(AttendanceSession::getId).toList();

        // Calculate GPA for each student
        List<StudentGPA> studentGPAs = new ArrayList<>();
        for (Enrollment enrollment : enrollments) {
            Student student = enrollment.getStudent();
            BigDecimal gpa = calculateGPA(student.getId(), classId, semesterId);
            if (gpa == null) {
                warnings.add(student.getStudentCode() + ": không có điểm");
                skipped++;
                continue;
            }
            studentGPAs.add(new StudentGPA(student, gpa));
            processed++;
        }

        // Sort by GPA descending for ranking
        studentGPAs.sort(Comparator.comparing(StudentGPA::gpa).reversed()
            .thenComparing(sg -> sg.student().getStudentCode()));

        // Save results
        for (int i = 0; i < studentGPAs.size(); i++) {
            StudentGPA sg = studentGPAs.get(i);
            int rank = i + 1;

            SemesterResult result = semesterResultRepository
                .findByStudentIdAndSemesterId(sg.student().getId(), semesterId)
                .orElseGet(SemesterResult::new);

            result.setStudent(sg.student());
            result.setSemester(semester);
            result.setCls(cls);
            result.setGpa(sg.gpa());
            result.setRank(rank);
            result.setHonor(calculateHonor(sg.gpa()));
            result.setAcademicAbility(calculateAcademicAbility(sg.gpa()));
            result.setConduct(calculateConduct(sg.student().getId(), sessionIds));
            semesterResultRepository.save(result);
            updated++;
        }

        return new CalculateSemesterResultResponse(processed, updated, skipped, warnings);
    }

    /**
     * Tính GPA cho học sinh: ưu tiên GradeBook (Phase E), fallback Grade legacy.
     * GPA = trung bình cộng điểm trung bình từng môn.
     * Điểm TB từng môn (GradeBook) = Σ(score × weight) / Σ(weight)
     */
    private BigDecimal calculateGPA(Long studentId, Long classId, Long semesterId) {
        // Try GradeBook first (Phase E)
        List<GradeBook> gradeBooks = gradeBookRepository.findByClsIdAndSemesterId(classId, semesterId);
        if (!gradeBooks.isEmpty()) {
            BigDecimal gpa = calculateGPAFromGradeBook(studentId, gradeBooks);
            if (gpa != null) return gpa;
        }

        // Fallback: Grade entity (legacy)
        return calculateGPAFromLegacyGrade(studentId, semesterId);
    }

    private BigDecimal calculateGPAFromGradeBook(Long studentId, List<GradeBook> gradeBooks) {
        BigDecimal subjectSum = BigDecimal.ZERO;
        int subjectCount = 0;

        for (GradeBook book : gradeBooks) {
            List<GradeItem> items = gradeItemRepository.findByGradeBookIdOrderByOrderAsc(book.getId());
            if (items.isEmpty()) continue;

            BigDecimal weightedSum = BigDecimal.ZERO;
            int totalWeight = 0;

            for (GradeItem item : items) {
                StudentScore score = studentScoreRepository
                    .findByGradeItemIdAndStudentId(item.getId(), studentId)
                    .orElse(null);
                if (score != null && score.getScore() != null && score.getIsGraded()) {
                    weightedSum = weightedSum.add(
                        score.getScore().multiply(BigDecimal.valueOf(item.getWeight())));
                    totalWeight += item.getWeight();
                }
            }

            if (totalWeight > 0) {
                BigDecimal subjectAvg = weightedSum.divide(
                    BigDecimal.valueOf(totalWeight), 2, RoundingMode.HALF_UP);
                subjectSum = subjectSum.add(subjectAvg);
                subjectCount++;
            }
        }

        if (subjectCount == 0) return null;
        return subjectSum.divide(BigDecimal.valueOf(subjectCount), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateGPAFromLegacyGrade(Long studentId, Long semesterId) {
        List<Grade> grades = gradeRepository.findByStudentIdAndSemesterId(studentId, semesterId);
        if (grades.isEmpty()) return null;

        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        for (Grade g : grades) {
            if (g.getAverage() != null) {
                sum = sum.add(g.getAverage());
                count++;
            }
        }
        if (count == 0) return null;
        return sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }

    /**
     * Xếp loại danh hiệu theo GPA.
     */
    private String calculateHonor(BigDecimal gpa) {
        if (gpa.compareTo(BigDecimal.valueOf(8.0)) >= 0) return "Giỏi";
        if (gpa.compareTo(BigDecimal.valueOf(6.5)) >= 0) return "Khá";
        if (gpa.compareTo(BigDecimal.valueOf(5.0)) >= 0) return "Trung bình";
        return "Yếu";
    }

    /**
     * Xếp loại học lực (cùng logic với danh hiệu).
     */
    private String calculateAcademicAbility(BigDecimal gpa) {
        return calculateHonor(gpa);
    }

    /**
     * Tính hạnh kiểm từ tỷ lệ vắng không phép trong học kỳ.
     * < 5%: Tốt | 5-15%: Khá | 15-30%: Trung bình | ≥ 30%: Yếu
     */
    private String calculateConduct(Long studentId, List<Long> sessionIds) {
        if (sessionIds.isEmpty()) return "Tốt";

        long totalRecords = attendanceDetailRepository
            .countByStudentIdAndSessionIdIn(studentId, sessionIds);
        if (totalRecords == 0) return "Tốt";

        long absentWithoutLeave = attendanceDetailRepository
            .countByStudentIdAndStatusAndSessionIdIn(
                studentId, AttendanceStatus.ABSENT_WITHOUT_LEAVE, sessionIds);

        double absentRate = (double) absentWithoutLeave / totalRecords * 100.0;

        if (absentRate < 5) return "Tốt";
        if (absentRate < 15) return "Khá";
        if (absentRate < 30) return "Trung bình";
        return "Yếu";
    }

    private record StudentGPA(Student student, BigDecimal gpa) {}
}