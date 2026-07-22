package vn.edu.fpt.myfschool.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.CalculateSemesterResultResponse;
import vn.edu.fpt.myfschool.common.enums.AttendanceStatus;
import vn.edu.fpt.myfschool.common.enums.AssessmentType;
import vn.edu.fpt.myfschool.common.enums.ConductSource;
import vn.edu.fpt.myfschool.common.enums.EnrollmentStatus;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.common.exception.ConflictException;
import vn.edu.fpt.myfschool.entity.*;
import vn.edu.fpt.myfschool.repository.*;
import vn.edu.fpt.myfschool.service.SemesterResultCalculationService;
import vn.edu.fpt.myfschool.service.StudentRiskService;
import vn.edu.fpt.myfschool.service.ResultClassificationPolicy;

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
    private final AttendanceRepository attendanceRepository;
    private final ClassRepository classRepository;
    private final SemesterRepository semesterRepository;
    // Phase E: GradeBook dependencies
    private final GradeBookRepository gradeBookRepository;
    private final GradeItemRepository gradeItemRepository;
    private final StudentScoreRepository studentScoreRepository;
    private final StudentRiskService studentRiskService;
    private final AcademicYearSubjectRepository academicYearSubjectRepository;

    @Override
    public CalculateSemesterResultResponse calculate(Long classId, Long semesterId) {
        SchoolClass cls = classRepository.findById(classId)
            .orElseThrow(() -> new ResourceNotFoundException("Class", "id", classId));
        Semester semester = semesterRepository.findById(semesterId)
            .orElseThrow(() -> new ResourceNotFoundException("Semester", "id", semesterId));
        if (!semester.getAcademicYear().getId().equals(cls.getAcademicYear().getId())) {
            throw new ConflictException("Học kỳ không thuộc năm học của lớp đã chọn");
        }
        if (semester.getStatus() == vn.edu.fpt.myfschool.common.enums.SemesterStatus.COMPLETED
                || cls.getAcademicYear().getStatus() == vn.edu.fpt.myfschool.common.enums.AcademicYearStatus.COMPLETED) {
            throw new ConflictException("Kết quả đã đóng và chỉ còn quyền xem");
        }

        List<Enrollment> enrollments = enrollmentRepository
            .findByClsIdAndAcademicYearIdAndStatus(classId, cls.getAcademicYear().getId(), EnrollmentStatus.ACTIVE);

        validateScoresBeforeCalculation(cls, semester, enrollments);

        int processed = 0, updated = 0, skipped = 0;
        List<String> warnings = new ArrayList<>();

        // Attendance records are the single source of truth for conduct.
        List<Attendance> attendanceRecords = attendanceRepository
            .findByClsIdAndDateBetween(classId, semester.getStartDate(), semester.getEndDate());

        // Calculate GPA for each student
        List<StudentGPA> studentGPAs = new ArrayList<>();
        for (Enrollment enrollment : enrollments) {
            Student student = enrollment.getStudent();
            List<GradeBook> books = gradeBookRepository.findByClsIdAndSemesterId(classId, semesterId);
            List<BigDecimal> subjectAverages = calculateSubjectAverages(student.getId(), books);
            BigDecimal gpa = average(subjectAverages);
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
            List<GradeBook> books = gradeBookRepository.findByClsIdAndSemesterId(classId, semesterId);
            List<BigDecimal> subjectAverages = calculateSubjectAverages(sg.student().getId(), books);
            int failedCommentSubjects = countFailedCommentSubjects(sg.student().getId(), books);
            String suggestedAcademicAbility = ResultClassificationPolicy.academicAbility(
                    subjectAverages, failedCommentSubjects);
            String suggestedHonor = "Không";
            result.setSuggestedHonor(suggestedHonor);
            result.setSuggestedAcademicAbility(suggestedAcademicAbility);
            String suggestedConduct = calculateConduct(sg.student().getId(), attendanceRecords);
            result.setSuggestedConduct(suggestedConduct);
            if (!Boolean.TRUE.equals(result.getResultOverridden())) {
                result.setHonor(suggestedHonor);
                result.setAcademicAbility(suggestedAcademicAbility);
                result.setConduct(suggestedConduct);
                result.setConductSource(ConductSource.SUGGESTED);
            }
            result.setPublishedAt(null);
            semesterResultRepository.save(result);
            updated++;
        }

        studentRiskService.recalculateClass(cls.getAcademicYear().getId(), semesterId, classId);

        return new CalculateSemesterResultResponse(processed, updated, skipped, warnings);
    }

    @Override
    public CalculateSemesterResultResponse calculateSchool(Long academicYearId, Long semesterId) {
        Semester semester = semesterRepository.findById(semesterId)
            .orElseThrow(() -> new ResourceNotFoundException("Semester", "id", semesterId));
        if (!semester.getAcademicYear().getId().equals(academicYearId)) {
            throw new ConflictException("Học kỳ không thuộc năm học đã chọn");
        }

        int processed = 0, updated = 0, skipped = 0;
        List<String> warnings = new ArrayList<>();
        List<SchoolClass> classes = classRepository.findByAcademicYearId(academicYearId).stream()
            .sorted(Comparator.comparing(SchoolClass::getName, String.CASE_INSENSITIVE_ORDER))
            .toList();
        boolean hasActiveStudents = false;
        for (SchoolClass cls : classes) {
            if (enrollmentRepository.findByClsIdAndAcademicYearIdAndStatus(
                    cls.getId(), academicYearId, EnrollmentStatus.ACTIVE).isEmpty()) {
                continue;
            }
            hasActiveStudents = true;
            try {
                CalculateSemesterResultResponse result = calculate(cls.getId(), semesterId);
                processed += result.processed();
                updated += result.updated();
                skipped += result.skipped();
                result.warnings().forEach(warning -> warnings.add(cls.getName() + ": " + warning));
            } catch (ConflictException exception) {
                throw new ConflictException("Không thể tính kết quả toàn trường vì lớp "
                        + cls.getName() + ": " + exception.getMessage());
            }
        }
        if (!hasActiveStudents) {
            throw new ConflictException("Năm học chưa có học sinh đang theo học để tính kết quả");
        }
        return new CalculateSemesterResultResponse(processed, updated, skipped, warnings);
    }

    private void validateScoresBeforeCalculation(SchoolClass cls, Semester semester, List<Enrollment> enrollments) {
        List<GradeBook> books = gradeBookRepository.findByClsIdAndSemesterId(cls.getId(), semester.getId());
        if (books.isEmpty()) throw new ConflictException("Không thể tính kết quả học kỳ vì lớp chưa có sổ điểm môn học");
        java.util.Set<Long> configuredSubjects = academicYearSubjectRepository
                .findByAcademicYearId(cls.getAcademicYear().getId()).stream()
                .map(item -> item.getSubject().getId()).collect(java.util.stream.Collectors.toSet());
        java.util.Set<Long> availableSubjects = books.stream().map(book -> book.getSubject().getId())
                .collect(java.util.stream.Collectors.toSet());
        if (!availableSubjects.containsAll(configuredSubjects)) {
            throw new ConflictException("Không thể tính kết quả học kỳ vì chưa có đủ bảng điểm các môn đã cấu hình");
        }
        List<String> missing = new ArrayList<>();
        for (GradeBook book : books) {
            List<GradeItem> requiredItems = gradeItemRepository.findByGradeBookIdOrderByOrderAsc(book.getId())
                .stream().filter(GradeItem::getRequiredEntry).toList();
            for (Enrollment enrollment : enrollments) for (GradeItem item : requiredItems) {
                Student student = enrollment.getStudent();
                StudentScore score = studentScoreRepository.findByGradeItemIdAndStudentId(item.getId(), student.getId()).orElse(null);
                boolean hasRequiredValue = score != null && Boolean.TRUE.equals(score.getIsGraded())
                    && (item.getAssessmentType() == AssessmentType.SCORE
                        ? score.getScore() != null
                        : score.getComment() != null && !score.getComment().isBlank());
                if (!hasRequiredValue)
                    missing.add(student.getStudentCode() + " - " + book.getSubject().getName() + " - " + item.getName());
            }
        }
        if (!missing.isEmpty()) {
            String details = missing.stream().limit(10).collect(java.util.stream.Collectors.joining(", "));
            if (missing.size() > 10) details += ", ... và " + (missing.size() - 10) + " điểm khác";
            throw new ConflictException("Không thể tính kết quả học kỳ vì còn " + missing.size() + " điểm bắt buộc chưa nhập: " + details);
        }
    }

    /**
     * Tính GPA cho học sinh: ưu tiên GradeBook (Phase E), fallback Grade legacy.
     * GPA = trung bình cộng điểm trung bình từng môn.
     * Điểm TB từng môn (GradeBook) = Σ(score × weight) / Σ(weight)
     */
    private BigDecimal calculateGPA(Long studentId, Long classId, Long semesterId) {
        List<GradeBook> gradeBooks = gradeBookRepository.findByClsIdAndSemesterId(classId, semesterId);
        return calculateGPAFromGradeBook(studentId, gradeBooks);
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
                if (item.getAssessmentType() != AssessmentType.SCORE) continue;
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

    /** Tính hạnh kiểm từ số buổi vắng không phép trong học kỳ. */
    private String calculateConduct(Long studentId, List<Attendance> attendanceRecords) {
        long absentWithoutLeave = attendanceRecords.stream()
            .filter(record -> record.getStudent().getId().equals(studentId))
            .filter(record -> record.getStatus() == AttendanceStatus.ABSENT_WITHOUT_LEAVE)
            .count();
        return ResultClassificationPolicy.suggestedConduct(absentWithoutLeave);
    }

    private List<BigDecimal> calculateSubjectAverages(Long studentId, List<GradeBook> books) {
        List<BigDecimal> result = new ArrayList<>();
        for (GradeBook book : books) {
            BigDecimal weighted = BigDecimal.ZERO;
            int totalWeight = 0;
            for (GradeItem item : gradeItemRepository.findByGradeBookIdOrderByOrderAsc(book.getId())) {
                if (item.getAssessmentType() != AssessmentType.SCORE) continue;
                StudentScore score = studentScoreRepository
                        .findByGradeItemIdAndStudentId(item.getId(), studentId).orElse(null);
                if (score != null && score.getScore() != null && Boolean.TRUE.equals(score.getIsGraded())) {
                    weighted = weighted.add(score.getScore().multiply(BigDecimal.valueOf(item.getWeight())));
                    totalWeight += item.getWeight();
                }
            }
            if (totalWeight > 0) result.add(weighted.divide(
                    BigDecimal.valueOf(totalWeight), 2, RoundingMode.HALF_UP));
        }
        return result;
    }

    private int countFailedCommentSubjects(Long studentId, List<GradeBook> books) {
        int failures = 0;
        for (GradeBook book : books) {
            List<GradeItem> items = gradeItemRepository.findByGradeBookIdOrderByOrderAsc(book.getId());
            if (items.stream().anyMatch(item -> item.getAssessmentType() == AssessmentType.SCORE)) continue;
            boolean passed = items.stream().filter(item -> item.getAssessmentType() == AssessmentType.PASS_FAIL)
                    .allMatch(item -> studentScoreRepository.findByGradeItemIdAndStudentId(item.getId(), studentId)
                            .map(score -> "PASS".equalsIgnoreCase(score.getComment())).orElse(false));
            if (!passed) failures++;
        }
        return failures;
    }

    private BigDecimal average(List<BigDecimal> values) {
        if (values.isEmpty()) return null;
        return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }

    private record StudentGPA(Student student, BigDecimal gpa) {}
}
