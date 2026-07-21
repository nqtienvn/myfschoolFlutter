package vn.edu.fpt.myfschool.service.impl;

import vn.edu.fpt.myfschool.entity.SchoolClass;
import vn.edu.fpt.myfschool.entity.Semester;
import vn.edu.fpt.myfschool.entity.SemesterResult;
import vn.edu.fpt.myfschool.entity.Student;
import vn.edu.fpt.myfschool.entity.Enrollment;
import vn.edu.fpt.myfschool.entity.HomeroomAssignment;
import vn.edu.fpt.myfschool.entity.Parent;
import vn.edu.fpt.myfschool.entity.Teacher;
import vn.edu.fpt.myfschool.entity.Attendance;
import vn.edu.fpt.myfschool.entity.StudentEvent;
import vn.edu.fpt.myfschool.entity.StudentPeriodicReport;
import vn.edu.fpt.myfschool.entity.GradeBook;
import vn.edu.fpt.myfschool.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.enums.*;
import vn.edu.fpt.myfschool.common.exception.BadRequestException;
import vn.edu.fpt.myfschool.common.exception.ConflictException;
import vn.edu.fpt.myfschool.common.exception.ForbiddenException;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.repository.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service("semesterResultService")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SemesterResultServiceImpl implements SemesterResultService {

    private final SemesterResultRepository semesterResultRepository;
    private final StudentRepository studentRepository;
    private final SemesterRepository semesterRepository;
    private final ClassRepository classRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ParentRepository parentRepository;
    private final StudentGuardianRepository studentGuardianRepository;
    private final TeacherRepository teacherRepository;
    private final HomeroomAssignmentRepository homeroomAssignmentRepository;
    private final AttendanceRepository attendanceRepository;
    private final StudentEventRepository studentEventRepository;
    private final StudentPeriodicReportRepository studentPeriodicReportRepository;
    private final PeriodicReviewService periodicReviewService;
    private final GradeBookRepository gradeBookRepository;
    private final AcademicYearSubjectRepository academicYearSubjectRepository;
    private final NotificationService notificationService;

    @Override
    public SemesterResultDto getStudentSemesterResult(
            Long studentId, Long semesterId, Long requestUserId, UserRole requestRole) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("Student", "id", studentId));
        Semester semester = semesterRepository.findById(semesterId)
            .orElseThrow(() -> new ResourceNotFoundException("Semester", "id", semesterId));
        SemesterResult sr = semesterResultRepository.findByStudentIdAndSemesterId(studentId, semesterId)
            .orElse(null);
        SchoolClass resultClass;
        if (sr != null) {
            if (!sr.getCls().getAcademicYear().getId()
                    .equals(semester.getAcademicYear().getId())) {
                throw new BadRequestException("Lớp của kết quả không thuộc năm học đã chọn");
            }
            boolean enrolledInResultClass = enrollmentRepository.findByStudentId(studentId)
                .stream()
                .anyMatch(enrollment -> enrollment.getCls().getId().equals(sr.getCls().getId())
                    && enrollment.getAcademicYear().getId()
                        .equals(semester.getAcademicYear().getId()));
            if (!enrolledInResultClass) {
                throw new BadRequestException("Kết quả học kỳ không khớp hồ sơ nhập học");
            }
            resultClass = sr.getCls();
        } else {
            Enrollment enrollment = enrollmentRepository
                .findFirstByStudentIdAndAcademicYearIdOrderByIdDesc(
                    studentId, semester.getAcademicYear().getId())
                .orElseThrow(() -> new BadRequestException(
                    "Học sinh không thuộc năm học của học kỳ đã chọn"));
            resultClass = enrollment.getCls();
        }
        authorizeStudentResult(
            student, resultClass, semester, requestUserId, requestRole);

        if (sr == null) return null;
        if ((requestRole == UserRole.PARENT || requestRole == UserRole.STUDENT)
                && sr.getPublishedAt() == null) return null;

        return new SemesterResultDto(sr.getId(), student.getId(), student.getUser().getName(),
            semester.getId(), semester.getName(), sr.getCls().getId(), sr.getCls().getName(),
            sr.getGpa(), sr.getRank(), sr.getHonor(), sr.getConduct(), sr.getAcademicAbility());
    }

    @Override
    public ClassRankingDto getClassRanking(
            Long classId, Long semesterId, Long requestUserId, UserRole requestRole) {
        SchoolClass cls = classRepository.findById(classId)
            .orElseThrow(() -> new ResourceNotFoundException("Class", "id", classId));
        Semester semester = semesterRepository.findById(semesterId)
            .orElseThrow(() -> new ResourceNotFoundException("Semester", "id", semesterId));
        if (!semester.getAcademicYear().getId().equals(cls.getAcademicYear().getId())) {
            throw new BadRequestException("Học kỳ không thuộc năm học của lớp đã chọn");
        }
        authorizeClassSummary(cls, semester, requestUserId, requestRole);

        List<SemesterResult> results = semesterResultRepository.findByClassIdAndSemesterIdOrderByRankAsc(classId, semesterId);
        List<ClassRankEntryDto> rankings = results.stream().map(sr ->
            new ClassRankEntryDto(sr.getRank(), sr.getStudent().getId(),
                sr.getStudent().getUser().getName(), sr.getStudent().getStudentCode(),
                sr.getGpa(), sr.getAcademicAbility(), sr.getConduct())
        ).collect(Collectors.toList());

        return new ClassRankingDto(classId, cls.getName(), semesterId, semester.getName(), rankings);
    }

    @Override
    public List<ResultSummaryDto> getResultSummary(Long academicYearId, Long semesterId, Long classId) {
        ResultScope scope = requireResultScope(academicYearId, semesterId, classId);
        List<Attendance> attendance = attendanceRepository.findByClsIdAndDateBetween(
                classId, scope.semester().getStartDate(), scope.semester().getEndDate());
        List<StudentEvent> violations = studentEventRepository.findByClsIdAndSemesterId(classId, semesterId)
                .stream()
                .filter(event -> event.getEventType() == StudentEventType.VIOLATION)
                .filter(event -> event.getStatus() == StudentEventStatus.SUBMITTED)
                .toList();
        return enrollmentRepository.findByClsIdAndAcademicYearIdAndStatus(
                        classId, academicYearId, EnrollmentStatus.ACTIVE).stream()
                .map(Enrollment::getStudent)
                .sorted(Comparator.comparing(Student::getStudentCode))
                .map(student -> toResultSummary(scope, student, attendance, violations))
                .toList();
    }

    @Override
    @Transactional
    public ResultSummaryDto overrideResult(Long studentId, ResultOverrideRequest request, Long adminUserId) {
        ResultScope scope = requireResultScope(request.academicYearId(), request.semesterId(), request.classId());
        requireEditable(scope);
        requireSummaryStudent(scope, studentId);
        validateClassification(request.academicAbility(), request.conduct());
        SemesterResult result = semesterResultRepository.findByStudentIdAndSemesterId(studentId, request.semesterId())
                .orElseThrow(() -> new ConflictException("Hãy tính kết quả học kỳ trước khi điều chỉnh"));
        if (!result.getCls().getId().equals(request.classId())) {
            throw new ForbiddenException("Kết quả không thuộc lớp đã chọn");
        }
        result.setAcademicAbility(request.academicAbility().trim());
        result.setConduct(request.conduct().trim());
        result.setHonor(request.honor().trim());
        result.setConductSource(ConductSource.ADMIN);
        result.setResultOverridden(true);
        result.setPublishedAt(null);
        semesterResultRepository.save(result);
        studentPeriodicReportRepository.findByStudentIdAndSemesterId(studentId, request.semesterId())
                .filter(report -> report.getStatus() == PeriodicReportStatus.PUBLISHED)
                .ifPresent(report -> {
                    report.setStatus(PeriodicReportStatus.SUBMITTED);
                    report.setConduct(null);
                    report.setPublishedAt(null);
                    studentPeriodicReportRepository.save(report);
                });
        return getResultSummary(request.academicYearId(), request.semesterId(), request.classId()).stream()
                .filter(item -> item.studentId().equals(studentId))
                .findFirst()
                .orElseThrow();
    }

    @Override
    @Transactional
    public List<ResultSummaryDto> publishResults(ResultPublishRequest request, Long adminUserId) {
        ResultScope scope = requireResultScope(request.academicYearId(), request.semesterId(), request.classId());
        requireEditable(scope);
        List<HomeroomReportDto> reports = periodicReviewService.getAdminReports(
                request.academicYearId(), request.semesterId(), request.classId(), null);
        Map<Long, HomeroomReportDto> reportByStudent = new HashMap<>();
        reports.forEach(report -> reportByStudent.put(report.studentId(), report));
        List<Student> roster = enrollmentRepository.findByClsIdAndAcademicYearIdAndStatus(
                        request.classId(), request.academicYearId(), EnrollmentStatus.ACTIVE).stream()
                .map(Enrollment::getStudent).toList();
        for (Student student : roster) {
            SemesterResult result = semesterResultRepository.findByStudentIdAndSemesterId(
                            student.getId(), request.semesterId())
                    .orElseThrow(() -> new ConflictException(
                            "Chưa tính kết quả học kỳ cho " + student.getStudentCode()));
            HomeroomReportDto report = reportByStudent.get(student.getId());
            if (report == null || report.status() != PeriodicReportStatus.SUBMITTED) {
                throw new ConflictException("GVCN chưa Submit nhận xét cho " + student.getStudentCode());
            }
            if (report.generalComment() == null || report.generalComment().isBlank()) {
                throw new ConflictException("Thiếu nhận xét GVCN cho " + student.getStudentCode());
            }
            if (!report.missingSubjects().isEmpty()) {
                throw new ConflictException("Thiếu nhận xét môn của " + student.getStudentCode()
                        + ": " + String.join(", ", report.missingSubjects()));
            }
            if (result.getAcademicAbility() == null || result.getConduct() == null || result.getHonor() == null) {
                throw new ConflictException("Kết quả cuối của " + student.getStudentCode() + " chưa đầy đủ");
            }
        }
        LocalDateTime publishedAt = LocalDateTime.now();
        for (Student student : roster) {
            SemesterResult result = semesterResultRepository.findByStudentIdAndSemesterId(
                    student.getId(), request.semesterId()).orElseThrow();
            boolean firstPublication = result.getPublishedAt() == null;
            result.setPublishedAt(publishedAt);
            semesterResultRepository.save(result);
            StudentPeriodicReport report = studentPeriodicReportRepository.findByStudentIdAndSemesterId(
                    student.getId(), request.semesterId()).orElseThrow();
            report.setConduct(result.getConduct());
            report.setStatus(PeriodicReportStatus.PUBLISHED);
            report.setPublishedAt(publishedAt);
            studentPeriodicReportRepository.save(report);
            if (firstPublication) sendPublishedNotifications(student, scope.semester(), result.getId());
        }
        return getResultSummary(request.academicYearId(), request.semesterId(), request.classId());
    }

    @Override
    @Transactional
    public void closeSemester(ResultCloseRequest request) {
        Semester semester = semesterRepository.findById(request.semesterId())
                .orElseThrow(() -> new ResourceNotFoundException("Semester", "id", request.semesterId()));
        if (!semester.getAcademicYear().getId().equals(request.academicYearId())) {
            throw new ForbiddenException("Học kỳ không thuộc năm học đã chọn");
        }
        if (semester.getAcademicYear().getStatus() != AcademicYearStatus.ACTIVE
                || semester.getStatus() != SemesterStatus.ACTIVE) {
            throw new ConflictException("Chỉ học kỳ ACTIVE của năm học ACTIVE mới được đóng");
        }
        Set<Long> configuredSubjectIds = academicYearSubjectRepository
                .findByAcademicYearId(request.academicYearId()).stream()
                .map(item -> item.getSubject().getId()).collect(Collectors.toSet());
        for (SchoolClass cls : classRepository.findByAcademicYearId(request.academicYearId())) {
            List<Student> roster = enrollmentRepository.findByClsIdAndAcademicYearIdAndStatus(
                            cls.getId(), request.academicYearId(), EnrollmentStatus.ACTIVE).stream()
                    .map(Enrollment::getStudent).toList();
            if (roster.isEmpty()) continue;
            List<GradeBook> books = gradeBookRepository
                    .findByClsIdAndSemesterId(cls.getId(), request.semesterId());
            Set<Long> bookSubjectIds = books.stream().map(book -> book.getSubject().getId())
                    .collect(Collectors.toSet());
            if (!bookSubjectIds.containsAll(configuredSubjectIds)) {
                throw new ConflictException("Lớp " + cls.getName()
                        + " chưa có đủ bảng điểm các môn đã cấu hình");
            }
            for (Student student : roster) {
                SemesterResult result = semesterResultRepository.findByStudentIdAndSemesterId(
                                student.getId(), request.semesterId())
                        .orElseThrow(() -> new ConflictException("Chưa tính kết quả cho "
                                + student.getStudentCode()));
                if (result.getPublishedAt() == null) {
                    throw new ConflictException("Chưa công bố kết quả cho " + student.getStudentCode());
                }
            }
            for (GradeBook book : books) {
                if (book.getStatus() != GradeBookStatus.PUBLISHED
                        && book.getStatus() != GradeBookStatus.LOCKED) {
                    throw new ConflictException("Bảng điểm " + cls.getName() + " - "
                            + book.getSubject().getName() + " chưa được công bố");
                }
                book.setStatus(GradeBookStatus.LOCKED);
                book.setIsFinalized(true);
                gradeBookRepository.save(book);
            }
        }
        semester.setStatus(SemesterStatus.COMPLETED);
        semester.setIsCurrent(false);
        semesterRepository.save(semester);
    }

    private ResultSummaryDto toResultSummary(ResultScope scope, Student student,
            List<Attendance> attendance, List<StudentEvent> violations) {
        SemesterResult result = semesterResultRepository.findByStudentIdAndSemesterId(
                student.getId(), scope.semester().getId()).orElse(null);
        StudentPeriodicReport report = studentPeriodicReportRepository.findByStudentIdAndSemesterId(
                student.getId(), scope.semester().getId()).orElse(null);
        long violationCount = violations.stream()
                .filter(event -> event.getStudent().getId().equals(student.getId())).count();
        long absentWithLeave = attendance.stream()
                .filter(row -> row.getStudent().getId().equals(student.getId()))
                .filter(row -> row.getStatus() == AttendanceStatus.ABSENT_WITH_LEAVE).count();
        long absentWithoutLeave = attendance.stream()
                .filter(row -> row.getStudent().getId().equals(student.getId()))
                .filter(row -> row.getStatus() == AttendanceStatus.ABSENT_WITHOUT_LEAVE).count();
        return new ResultSummaryDto(student.getId(), student.getUser().getName(), student.getStudentCode(),
                scope.cls().getAcademicYear().getId(), scope.semester().getId(), scope.cls().getId(),
                scope.cls().getName(), result == null ? null : result.getGpa(), result == null ? null : result.getRank(),
                violationCount, absentWithLeave, absentWithoutLeave,
                result == null ? null : firstNonBlank(result.getSuggestedAcademicAbility(), result.getAcademicAbility()),
                result == null ? null : result.getSuggestedConduct(), result == null ? null : result.getAcademicAbility(),
                result == null ? null : result.getConduct(), result == null ? null : result.getHonor(),
                report == null ? null : report.getGeneralComment(),
                report == null ? PeriodicReportStatus.DRAFT.name() : report.getStatus().name(),
                result != null && result.getPublishedAt() != null ? "PUBLISHED" : "DRAFT",
                result == null ? null : result.getPublishedAt());
    }

    private ResultScope requireResultScope(Long academicYearId, Long semesterId, Long classId) {
        SchoolClass cls = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class", "id", classId));
        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new ResourceNotFoundException("Semester", "id", semesterId));
        if (!cls.getAcademicYear().getId().equals(academicYearId)
                || !semester.getAcademicYear().getId().equals(academicYearId)) {
            throw new ForbiddenException("Năm học, học kỳ và lớp không cùng phạm vi");
        }
        return new ResultScope(cls, semester);
    }

    private Student requireSummaryStudent(ResultScope scope, Long studentId) {
        if (!enrollmentRepository.existsByStudentIdAndClsIdAndAcademicYearIdAndStatus(
                studentId, scope.cls().getId(), scope.cls().getAcademicYear().getId(), EnrollmentStatus.ACTIVE)) {
            throw new ForbiddenException("Học sinh không thuộc lớp/năm học đã chọn");
        }
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", studentId));
    }

    private void validateClassification(String academicAbility, String conduct) {
        Set<String> values = Set.of("Tốt", "Khá", "Đạt", "Chưa đạt", "Giỏi", "Trung bình", "Yếu");
        if (!values.contains(academicAbility.trim())) {
            throw new BadRequestException("Kết quả học tập cuối không hợp lệ");
        }
        if (!values.contains(conduct.trim())) {
            throw new BadRequestException("Kết quả rèn luyện cuối không hợp lệ");
        }
    }

    private void requireEditable(ResultScope scope) {
        if (scope.semester().getStatus() == SemesterStatus.COMPLETED
                || scope.cls().getAcademicYear().getStatus() == AcademicYearStatus.COMPLETED) {
            throw new ConflictException("Kết quả đã đóng và chỉ còn quyền xem");
        }
    }

    private void sendPublishedNotifications(Student student, Semester semester, Long resultId) {
        String title = "Kết quả " + semester.getName() + " đã được công bố";
        String body = "Nhà trường đã công bố điểm, kết quả học tập và rèn luyện của "
                + student.getUser().getName() + ".";
        notificationService.createNotification(student.getUser().getId(), title, body,
                "ACADEMIC_RESULT", resultId, "SEMESTER_RESULT");
        studentGuardianRepository.findGuardiansByStudentId(student.getId()).forEach(parent ->
                notificationService.createNotification(parent.getUser().getId(), title, body,
                        "ACADEMIC_RESULT", resultId, "SEMESTER_RESULT"));
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private record ResultScope(SchoolClass cls, Semester semester) {}

    private void authorizeStudentResult(
            Student student, SchoolClass cls, Semester semester,
            Long requestUserId, UserRole requestRole) {
        if (requestRole == UserRole.ADMIN) return;
        if (requestRole == UserRole.STUDENT) {
            if (!student.getUser().getId().equals(requestUserId)) {
                throw new ForbiddenException("Học sinh chỉ được xem kết quả của chính mình");
            }
            return;
        }
        if (requestRole == UserRole.PARENT) {
            Parent parent = parentRepository.findByUserId(requestUserId)
                .orElseThrow(() -> new ForbiddenException("Tài khoản không có hồ sơ phụ huynh"));
            if (!studentGuardianRepository.existsByStudentIdAndGuardianId(
                    student.getId(), parent.getId())) {
                throw new ForbiddenException("Phụ huynh không quản lý học sinh này");
            }
            return;
        }
        authorizeClassSummary(cls, semester, requestUserId, requestRole);
    }

    private void authorizeClassSummary(
            SchoolClass cls, Semester semester, Long requestUserId, UserRole requestRole) {
        if (requestRole == UserRole.ADMIN) return;
        if (requestRole != UserRole.TEACHER) {
            throw new ForbiddenException("Chỉ GVCN được xem tổng kết của cả lớp");
        }
        Teacher teacher = teacherRepository.findByUserId(requestUserId)
            .orElseThrow(() -> new ForbiddenException("Tài khoản không có hồ sơ giáo viên"));
        boolean assigned = homeroomAssignmentRepository
            .findByTeacherIdAndAcademicYearId(
                teacher.getId(), semester.getAcademicYear().getId())
            .stream()
            .filter(assignment -> assignment.getCls().getId().equals(cls.getId()))
            .anyMatch(assignment -> overlaps(assignment, semester));
        if (!assigned) {
            throw new ForbiddenException(
                "Giáo viên không phải GVCN của lớp trong học kỳ đã chọn");
        }
    }

    private boolean overlaps(HomeroomAssignment assignment, Semester semester) {
        return !assignment.getEffectiveFrom().isAfter(semester.getEndDate())
            && (assignment.getEffectiveTo() == null
                || !assignment.getEffectiveTo().isBefore(semester.getStartDate()));
    }
}
