package vn.edu.fpt.myfschool.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.AcademicYearResultDto;
import vn.edu.fpt.myfschool.common.dto.AcademicYearResultRequest;
import vn.edu.fpt.myfschool.common.enums.AcademicYearStatus;
import vn.edu.fpt.myfschool.common.enums.AssessmentType;
import vn.edu.fpt.myfschool.common.enums.EnrollmentStatus;
import vn.edu.fpt.myfschool.common.enums.SemesterStatus;
import vn.edu.fpt.myfschool.common.exception.ConflictException;
import vn.edu.fpt.myfschool.common.exception.ForbiddenException;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.entity.AcademicYear;
import vn.edu.fpt.myfschool.entity.AcademicYearResult;
import vn.edu.fpt.myfschool.entity.GradeBook;
import vn.edu.fpt.myfschool.entity.GradeItem;
import vn.edu.fpt.myfschool.entity.SchoolClass;
import vn.edu.fpt.myfschool.entity.Semester;
import vn.edu.fpt.myfschool.entity.SemesterResult;
import vn.edu.fpt.myfschool.entity.Student;
import vn.edu.fpt.myfschool.entity.StudentScore;
import vn.edu.fpt.myfschool.repository.AcademicYearRepository;
import vn.edu.fpt.myfschool.repository.AcademicYearResultRepository;
import vn.edu.fpt.myfschool.repository.AcademicYearSubjectRepository;
import vn.edu.fpt.myfschool.repository.ClassRepository;
import vn.edu.fpt.myfschool.repository.EnrollmentRepository;
import vn.edu.fpt.myfschool.repository.GradeBookRepository;
import vn.edu.fpt.myfschool.repository.GradeItemRepository;
import vn.edu.fpt.myfschool.repository.SemesterRepository;
import vn.edu.fpt.myfschool.repository.SemesterResultRepository;
import vn.edu.fpt.myfschool.repository.StudentGuardianRepository;
import vn.edu.fpt.myfschool.repository.StudentScoreRepository;
import vn.edu.fpt.myfschool.service.AcademicYearResultService;
import vn.edu.fpt.myfschool.service.NotificationService;
import vn.edu.fpt.myfschool.service.ResultClassificationPolicy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service("academicYearResultService")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AcademicYearResultServiceImpl implements AcademicYearResultService {
    private final AcademicYearRepository academicYearRepository;
    private final AcademicYearResultRepository annualResults;
    private final AcademicYearSubjectRepository yearSubjects;
    private final ClassRepository classRepository;
    private final SemesterRepository semesterRepository;
    private final SemesterResultRepository semesterResults;
    private final EnrollmentRepository enrollments;
    private final GradeBookRepository gradeBooks;
    private final GradeItemRepository gradeItems;
    private final StudentScoreRepository studentScores;
    private final StudentGuardianRepository guardians;
    private final NotificationService notifications;

    @Override
    public List<AcademicYearResultDto> getResults(Long academicYearId, Long classId) {
        AnnualScope scope = requireScope(academicYearId, classId, false);
        return enrollments.findByClsIdAndAcademicYearIdAndStatus(
                        classId, academicYearId, EnrollmentStatus.ACTIVE).stream()
                .map(enrollment -> toDto(scope, enrollment.getStudent()))
                .sorted(Comparator.comparing(AcademicYearResultDto::studentCode))
                .toList();
    }

    @Override
    @Transactional
    public List<AcademicYearResultDto> calculate(AcademicYearResultRequest request) {
        AnnualScope scope = requireScope(request.academicYearId(), request.classId(), true);
        requireClosedSemesters(scope);
        List<Student> roster = enrollments.findByClsIdAndAcademicYearIdAndStatus(
                        request.classId(), request.academicYearId(), EnrollmentStatus.ACTIVE).stream()
                .map(enrollment -> enrollment.getStudent()).toList();
        if (roster.isEmpty()) throw new ConflictException("Lớp chưa có học sinh đang theo học");

        List<CalculatedAnnualResult> calculated = new ArrayList<>();
        for (Student student : roster) {
            SemesterResult first = requireSemesterResult(student, scope.semester1());
            SemesterResult second = requireSemesterResult(student, scope.semester2());
            List<BigDecimal> subjectAverages = annualSubjectAverages(student.getId(), scope);
            if (subjectAverages.isEmpty()) {
                throw new ConflictException("Không có điểm môn cả năm của " + student.getStudentCode());
            }
            int failedCommentSubjects = failedCommentSubjects(student.getId(), scope);
            BigDecimal average = subjectAverages.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(subjectAverages.size()), 2, RoundingMode.HALF_UP);
            String academicAbility = ResultClassificationPolicy.academicAbility(subjectAverages, failedCommentSubjects);
            String conduct = ResultClassificationPolicy.annualConduct(first.getConduct(), second.getConduct());
            long atLeastNine = subjectAverages.stream()
                    .filter(value -> value.compareTo(BigDecimal.valueOf(9)) >= 0).count();
            String honor = "Không";
            if ("Tốt".equals(conduct) && "Tốt".equals(academicAbility)) {
                honor = atLeastNine >= 6 ? "Học sinh Xuất sắc" : "Học sinh Giỏi";
            }
            calculated.add(new CalculatedAnnualResult(student, average, academicAbility, conduct, honor));
        }

        calculated.sort(Comparator.comparing(CalculatedAnnualResult::average).reversed()
                .thenComparing(item -> item.student().getStudentCode()));
        for (int index = 0; index < calculated.size(); index++) {
            CalculatedAnnualResult item = calculated.get(index);
            AcademicYearResult result = annualResults.findByStudentIdAndAcademicYearId(
                            item.student().getId(), request.academicYearId())
                    .orElseGet(AcademicYearResult::new);
            result.setStudent(item.student());
            result.setAcademicYear(scope.year());
            result.setCls(scope.cls());
            result.setGpa(item.average());
            result.setRank(index + 1);
            result.setAcademicAbility(item.academicAbility());
            result.setConduct(item.conduct());
            result.setHonor(item.honor());
            result.setPublishedAt(null);
            annualResults.save(result);
        }
        return getResults(request.academicYearId(), request.classId());
    }

    @Override
    @Transactional
    public List<AcademicYearResultDto> publish(AcademicYearResultRequest request) {
        AnnualScope scope = requireScope(request.academicYearId(), request.classId(), true);
        requireClosedSemesters(scope);
        List<Student> roster = enrollments.findByClsIdAndAcademicYearIdAndStatus(
                        request.classId(), request.academicYearId(), EnrollmentStatus.ACTIVE).stream()
                .map(enrollment -> enrollment.getStudent()).toList();
        LocalDateTime publishedAt = LocalDateTime.now();
        for (Student student : roster) {
            AcademicYearResult result = annualResults.findByStudentIdAndAcademicYearId(
                            student.getId(), request.academicYearId())
                    .orElseThrow(() -> new ConflictException(
                            "Chưa tính tổng kết năm cho " + student.getStudentCode()));
            if (result.getPublishedAt() == null) {
                result.setPublishedAt(publishedAt);
                annualResults.save(result);
                sendPublishedNotifications(student, scope.year(), result.getId());
            }
        }
        return getResults(request.academicYearId(), request.classId());
    }

    private AnnualScope requireScope(Long academicYearId, Long classId, boolean editable) {
        AcademicYear year = academicYearRepository.findById(academicYearId)
                .orElseThrow(() -> new ResourceNotFoundException("AcademicYear", "id", academicYearId));
        SchoolClass cls = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class", "id", classId));
        if (!cls.getAcademicYear().getId().equals(academicYearId)) {
            throw new ForbiddenException("Lớp không thuộc năm học đã chọn");
        }
        if (editable && year.getStatus() == AcademicYearStatus.COMPLETED) {
            throw new ConflictException("Năm học đã hoàn thành và chỉ còn quyền xem");
        }
        List<Semester> semesters = semesterRepository.findByAcademicYearIdOrderByOrderAsc(academicYearId);
        Semester first = semesters.stream().filter(item -> item.getOrder() == 1).findFirst()
                .orElseThrow(() -> new ConflictException("Thiếu Học kỳ 1"));
        Semester second = semesters.stream().filter(item -> item.getOrder() == 2).findFirst()
                .orElseThrow(() -> new ConflictException("Thiếu Học kỳ 2"));
        return new AnnualScope(year, cls, first, second);
    }

    private void requireClosedSemesters(AnnualScope scope) {
        if (scope.semester1().getStatus() != SemesterStatus.COMPLETED
                || scope.semester2().getStatus() != SemesterStatus.COMPLETED) {
            throw new ConflictException("Phải đóng cả Học kỳ 1 và Học kỳ 2 trước khi tổng kết năm học");
        }
    }

    private SemesterResult requireSemesterResult(Student student, Semester semester) {
        SemesterResult result = semesterResults.findByStudentIdAndSemesterId(student.getId(), semester.getId())
                .orElseThrow(() -> new ConflictException("Thiếu kết quả " + semester.getName()
                        + " của " + student.getStudentCode()));
        if (result.getPublishedAt() == null) {
            throw new ConflictException("Kết quả " + semester.getName() + " của "
                    + student.getStudentCode() + " chưa được công bố");
        }
        return result;
    }

    private List<BigDecimal> annualSubjectAverages(Long studentId, AnnualScope scope) {
        Map<Long, GradeBook> firstBooks = booksBySubject(scope.cls().getId(), scope.semester1().getId());
        Map<Long, GradeBook> secondBooks = booksBySubject(scope.cls().getId(), scope.semester2().getId());
        List<BigDecimal> result = new ArrayList<>();
        for (var configured : yearSubjects.findByAcademicYearId(scope.year().getId())) {
            GradeBook first = firstBooks.get(configured.getSubject().getId());
            GradeBook second = secondBooks.get(configured.getSubject().getId());
            if (first == null || second == null) {
                throw new ConflictException("Thiếu bảng điểm hai học kỳ của môn "
                        + configured.getSubject().getName());
            }
            BigDecimal firstAverage = subjectAverage(first, studentId);
            BigDecimal secondAverage = subjectAverage(second, studentId);
            if (firstAverage != null || secondAverage != null) {
                if (firstAverage == null || secondAverage == null) {
                    throw new ConflictException("Thiếu điểm trung bình một học kỳ của môn "
                            + configured.getSubject().getName());
                }
                result.add(firstAverage.add(secondAverage.multiply(BigDecimal.valueOf(2)))
                        .divide(BigDecimal.valueOf(3), 1, RoundingMode.HALF_UP));
            }
        }
        return result;
    }

    private int failedCommentSubjects(Long studentId, AnnualScope scope) {
        int failures = 0;
        for (GradeBook book : gradeBooks.findByClsIdAndSemesterId(scope.cls().getId(), scope.semester2().getId())) {
            List<GradeItem> items = gradeItems.findByGradeBookIdOrderByOrderAsc(book.getId());
            boolean hasNumeric = items.stream().anyMatch(item -> item.getAssessmentType() == AssessmentType.SCORE);
            if (hasNumeric) continue;
            boolean passed = items.stream().filter(item -> item.getAssessmentType() == AssessmentType.PASS_FAIL)
                    .allMatch(item -> studentScores.findByGradeItemIdAndStudentId(item.getId(), studentId)
                            .map(score -> "PASS".equalsIgnoreCase(score.getComment()))
                            .orElse(false));
            if (!passed) failures++;
        }
        return failures;
    }

    private Map<Long, GradeBook> booksBySubject(Long classId, Long semesterId) {
        Map<Long, GradeBook> result = new HashMap<>();
        gradeBooks.findByClsIdAndSemesterId(classId, semesterId)
                .forEach(book -> result.put(book.getSubject().getId(), book));
        return result;
    }

    private BigDecimal subjectAverage(GradeBook book, Long studentId) {
        BigDecimal weighted = BigDecimal.ZERO;
        int totalWeight = 0;
        for (GradeItem item : gradeItems.findByGradeBookIdOrderByOrderAsc(book.getId())) {
            if (item.getAssessmentType() != AssessmentType.SCORE) continue;
            StudentScore score = studentScores.findByGradeItemIdAndStudentId(item.getId(), studentId).orElse(null);
            if (score != null && Boolean.TRUE.equals(score.getIsGraded()) && score.getScore() != null) {
                weighted = weighted.add(score.getScore().multiply(BigDecimal.valueOf(item.getWeight())));
                totalWeight += item.getWeight();
            }
        }
        return totalWeight == 0 ? null
                : weighted.divide(BigDecimal.valueOf(totalWeight), 2, RoundingMode.HALF_UP);
    }

    private AcademicYearResultDto toDto(AnnualScope scope, Student student) {
        SemesterResult first = semesterResults.findByStudentIdAndSemesterId(
                student.getId(), scope.semester1().getId()).orElse(null);
        SemesterResult second = semesterResults.findByStudentIdAndSemesterId(
                student.getId(), scope.semester2().getId()).orElse(null);
        AcademicYearResult result = annualResults.findByStudentIdAndAcademicYearId(
                student.getId(), scope.year().getId()).orElse(null);
        return new AcademicYearResultDto(student.getId(), student.getUser().getName(), student.getStudentCode(),
                scope.year().getId(), scope.cls().getId(), scope.cls().getName(),
                first == null ? null : first.getGpa(), first == null ? null : first.getAcademicAbility(),
                first == null ? null : first.getConduct(), second == null ? null : second.getGpa(),
                second == null ? null : second.getAcademicAbility(), second == null ? null : second.getConduct(),
                result == null ? null : result.getGpa(), result == null ? null : result.getRank(),
                result == null ? null : result.getAcademicAbility(), result == null ? null : result.getConduct(),
                result == null ? null : result.getHonor(),
                result != null && result.getPublishedAt() != null ? "PUBLISHED" : "DRAFT",
                result == null ? null : result.getPublishedAt());
    }

    private void sendPublishedNotifications(Student student, AcademicYear year, Long resultId) {
        String title = "Kết quả năm học " + year.getName() + " đã được công bố";
        String body = "Nhà trường đã công bố kết quả học tập và rèn luyện cả năm của "
                + student.getUser().getName() + ".";
        notifications.createNotification(student.getUser().getId(), title, body,
                "ACADEMIC_RESULT", resultId, "ACADEMIC_YEAR_RESULT");
        guardians.findGuardiansByStudentId(student.getId()).forEach(parent ->
                notifications.createNotification(parent.getUser().getId(), title, body,
                        "ACADEMIC_RESULT", resultId, "ACADEMIC_YEAR_RESULT"));
    }

    private record AnnualScope(AcademicYear year, SchoolClass cls, Semester semester1, Semester semester2) {}
    private record CalculatedAnnualResult(Student student, BigDecimal average,
                                          String academicAbility, String conduct, String honor) {}
}
