package vn.edu.fpt.myfschool.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.CalculateSemesterResultResponse;
import vn.edu.fpt.myfschool.common.enums.AttendanceStatus;
import vn.edu.fpt.myfschool.common.enums.AssessmentType;
import vn.edu.fpt.myfschool.common.enums.ConductSource;
import vn.edu.fpt.myfschool.common.enums.EnrollmentStatus;
import vn.edu.fpt.myfschool.common.enums.PeriodicReportStatus;
import vn.edu.fpt.myfschool.common.enums.StudentEventStatus;
import vn.edu.fpt.myfschool.common.enums.StudentEventType;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.common.exception.ConflictException;
import vn.edu.fpt.myfschool.entity.*;
import vn.edu.fpt.myfschool.repository.*;
import vn.edu.fpt.myfschool.service.SemesterResultCalculationService;
import vn.edu.fpt.myfschool.service.StudentRiskService;

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
    private final StudentEventRepository studentEventRepository;
    private final StudentPeriodicReportRepository studentPeriodicReportRepository;

    @Override
    public CalculateSemesterResultResponse calculate(Long classId, Long semesterId) {
        SchoolClass cls = classRepository.findById(classId)
            .orElseThrow(() -> new ResourceNotFoundException("Class", "id", classId));
        Semester semester = semesterRepository.findById(semesterId)
            .orElseThrow(() -> new ResourceNotFoundException("Semester", "id", semesterId));
        if (!semester.getAcademicYear().getId().equals(cls.getAcademicYear().getId())) {
            throw new ConflictException("Học kỳ không thuộc năm học của lớp đã chọn");
        }

        List<Enrollment> enrollments = enrollmentRepository
            .findByClsIdAndAcademicYearIdAndStatus(classId, cls.getAcademicYear().getId(), EnrollmentStatus.ACTIVE);

        validateScoresBeforeCalculation(cls, semester, enrollments);

        int processed = 0, updated = 0, skipped = 0;
        List<String> warnings = new ArrayList<>();

        // Attendance records are the single source of truth for conduct.
        List<Attendance> attendanceRecords = attendanceRepository
            .findByClsIdAndDateBetween(classId, semester.getStartDate(), semester.getEndDate());
        List<StudentEvent> submittedViolations = studentEventRepository.findByClsIdAndSemesterId(classId, semesterId)
            .stream()
            .filter(event -> event.getEventType() == StudentEventType.VIOLATION)
            .filter(event -> event.getStatus() == StudentEventStatus.SUBMITTED)
            .toList();

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
            boolean wasPublished = result.getPublishedAt() != null;

            result.setStudent(sg.student());
            result.setSemester(semester);
            result.setCls(cls);
            result.setGpa(sg.gpa());
            result.setRank(rank);
            String suggestedHonor = calculateHonor(sg.gpa());
            String suggestedAcademicAbility = calculateAcademicAbility(sg.gpa());
            result.setSuggestedHonor(suggestedHonor);
            result.setSuggestedAcademicAbility(suggestedAcademicAbility);
            String suggestedConduct = calculateConduct(
                sg.student().getId(), attendanceRecords, submittedViolations);
            result.setSuggestedConduct(suggestedConduct);
            if (!Boolean.TRUE.equals(result.getResultOverridden())) {
                result.setHonor(suggestedHonor);
                result.setAcademicAbility(suggestedAcademicAbility);
                result.setConduct(suggestedConduct);
                result.setConductSource(ConductSource.SUGGESTED);
            }
            result.setPublishedAt(null);
            semesterResultRepository.save(result);
            if (wasPublished) {
                studentPeriodicReportRepository.findByStudentIdAndSemesterId(sg.student().getId(), semesterId)
                    .filter(report -> report.getStatus() == PeriodicReportStatus.PUBLISHED)
                    .ifPresent(report -> {
                        report.setStatus(PeriodicReportStatus.SUBMITTED);
                        report.setConduct(null);
                        report.setPublishedAt(null);
                        studentPeriodicReportRepository.save(report);
                    });
            }
            updated++;
        }

        studentRiskService.recalculateClass(cls.getAcademicYear().getId(), semesterId, classId);

        return new CalculateSemesterResultResponse(processed, updated, skipped, warnings);
    }

    private void validateScoresBeforeCalculation(SchoolClass cls, Semester semester, List<Enrollment> enrollments) {
        List<GradeBook> books = gradeBookRepository.findByClsIdAndSemesterId(cls.getId(), semester.getId());
        if (books.isEmpty()) throw new ConflictException("Không thể tính kết quả học kỳ vì lớp chưa có sổ điểm môn học");
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
     * Tính hạnh kiểm theo mức xấu hơn giữa vi phạm và chuyên cần.
     * Mỗi 3 vi phạm hạ một bậc; chuyên cần giữ các ngưỡng hiện hành.
     */
    private String calculateConduct(Long studentId, List<Attendance> attendanceRecords,
            List<StudentEvent> submittedViolations) {
        long violationCount = submittedViolations.stream()
            .filter(event -> event.getStudent().getId().equals(studentId))
            .count();
        int violationLevel = Math.min((int) (violationCount / 3), 3);
        long totalRecords = attendanceRecords.stream()
            .filter(record -> record.getStudent().getId().equals(studentId))
            .count();
        int attendanceLevel = 0;
        if (totalRecords > 0) {
            long absentWithoutLeave = attendanceRecords.stream()
                .filter(record -> record.getStudent().getId().equals(studentId))
                .filter(record -> record.getStatus() == AttendanceStatus.ABSENT_WITHOUT_LEAVE)
                .count();
            double absentRate = (double) absentWithoutLeave / totalRecords * 100.0;
            attendanceLevel = absentRate < 5 ? 0 : absentRate < 15 ? 1 : absentRate < 30 ? 2 : 3;
        }
        return List.of("Tốt", "Khá", "Trung bình", "Yếu")
            .get(Math.max(violationLevel, attendanceLevel));
    }

    private record StudentGPA(Student student, BigDecimal gpa) {}
}
