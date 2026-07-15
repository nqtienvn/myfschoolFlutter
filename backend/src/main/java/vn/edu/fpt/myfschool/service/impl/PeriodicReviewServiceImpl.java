package vn.edu.fpt.myfschool.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.enums.*;
import vn.edu.fpt.myfschool.common.exception.BadRequestException;
import vn.edu.fpt.myfschool.common.exception.ConflictException;
import vn.edu.fpt.myfschool.common.exception.ForbiddenException;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.entity.*;
import vn.edu.fpt.myfschool.repository.*;
import vn.edu.fpt.myfschool.service.PeriodicReviewService;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class PeriodicReviewServiceImpl implements PeriodicReviewService {
    private static final Set<String> CONDUCT_VALUES = Set.of("Tốt", "Khá", "Trung bình", "Yếu");

    private final AcademicYearRepository academicYears;
    private final SemesterRepository semesters;
    private final ClassRepository classes;
    private final StudentRepository students;
    private final TeacherRepository teachers;
    private final ParentRepository parents;
    private final StudentGuardianRepository guardians;
    private final EnrollmentRepository enrollments;
    private final TeachingAssignmentRepository assignments;
    private final HomeroomAssignmentRepository homeroomAssignments;
    private final SubjectStudentReviewRepository subjectReviews;
    private final StudentPeriodicReportRepository reports;
    private final SemesterResultRepository semesterResults;
    private final StudentReviewAuditRepository audits;
    private final UserRepository users;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ReviewAssignmentDto> getAssignments(Long academicYearId, Long teacherUserId) {
        requireYear(academicYearId);
        return assignments.findActiveByTeacherUserAndYear(teacherUserId, academicYearId).stream()
                .map(item -> new ReviewAssignmentDto(academicYearId, item.getCls().getId(), item.getCls().getName(),
                        item.getSubject().getId(), item.getSubject().getName()))
                .distinct()
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubjectReviewDto> getSubjectReviews(Long academicYearId, Long semesterId, Long classId,
            Long subjectId, Long teacherUserId) {
        Scope scope = requireScope(academicYearId, semesterId, classId);
        TeachingAssignment assignment = requireSubjectAssignment(scope, subjectId, teacherUserId);
        Map<Long, SubjectStudentReview> existing = new HashMap<>();
        subjectReviews.findByClsIdAndSubjectIdAndSemesterIdOrderByStudentStudentCode(classId, subjectId, semesterId)
                .forEach(review -> existing.put(review.getStudent().getId(), review));
        return roster(scope).stream()
                .map(student -> existing.containsKey(student.getId())
                        ? toSubjectDto(existing.get(student.getId()))
                        : emptySubjectDto(scope, assignment, student))
                .toList();
    }

    @Override
    public SubjectReviewDto saveSubjectReview(Long studentId, SaveSubjectReviewRequest request, Long teacherUserId) {
        Scope scope = requireScope(request.academicYearId(), request.semesterId(), request.classId());
        TeachingAssignment assignment = requireSubjectAssignment(scope, request.subjectId(), teacherUserId);
        Student student = requireRosterStudent(scope, studentId);
        User actor = requireUser(teacherUserId);
        SubjectStudentReview review = subjectReviews
                .findByStudentIdAndSubjectIdAndSemesterId(studentId, request.subjectId(), request.semesterId())
                .orElseGet(SubjectStudentReview::new);
        if (review.getId() != null && review.getStatus() == SubjectReviewStatus.SUBMITTED) {
            throw new ConflictException("Nhận xét đã gửi GVCN và chưa được trả lại");
        }
        Map<String, Object> oldValue = reviewSnapshot(review);
        review.setAcademicYear(scope.year());
        review.setSemester(scope.semester());
        review.setCls(scope.cls());
        review.setStudent(student);
        review.setSubject(assignment.getSubject());
        review.setSubjectTeacher(assignment.getTeacher());
        review.setComment(trim(request.comment()));
        review.setStrengths(trim(request.strengths()));
        review.setImprovements(trim(request.improvements()));
        review.setStatus(SubjectReviewStatus.DRAFT);
        review.setReturnReason(null);
        review.setSubmittedAt(null);
        review = subjectReviews.save(review);
        audit("SUBJECT_REVIEW", review.getId(), oldValue, reviewSnapshot(review), actor, "SAVE_DRAFT");
        return toSubjectDto(review);
    }

    @Override
    public List<SubjectReviewDto> submitSubjectReviews(SubmitSubjectReviewsRequest request, Long teacherUserId) {
        Scope scope = requireScope(request.academicYearId(), request.semesterId(), request.classId());
        requireSubjectAssignment(scope, request.subjectId(), teacherUserId);
        User actor = requireUser(teacherUserId);
        List<SubjectReviewDto> result = new ArrayList<>();
        for (Long studentId : new LinkedHashSet<>(request.studentIds())) {
            requireRosterStudent(scope, studentId);
            SubjectStudentReview review = subjectReviews
                    .findByStudentIdAndSubjectIdAndSemesterId(studentId, request.subjectId(), request.semesterId())
                    .orElseThrow(() -> new ConflictException("Học sinh chưa có nhận xét để gửi"));
            if (review.getStatus() == SubjectReviewStatus.SUBMITTED) {
                throw new ConflictException("Nhận xét đã được gửi trước đó");
            }
            if (trim(review.getComment()) == null) {
                throw new ConflictException("Phải nhập nhận xét cho mọi học sinh trước khi gửi");
            }
            Map<String, Object> oldValue = reviewSnapshot(review);
            review.setStatus(SubjectReviewStatus.SUBMITTED);
            review.setReturnReason(null);
            review.setSubmittedAt(LocalDateTime.now());
            review = subjectReviews.save(review);
            audit("SUBJECT_REVIEW", review.getId(), oldValue, reviewSnapshot(review), actor,
                    "SUBMIT_TO_HOMEROOM");
            result.add(toSubjectDto(review));
        }
        return result;
    }

    @Override
    public SubjectReviewDto returnSubjectReview(Long reviewId, ReturnSubjectReviewRequest request,
            Long teacherUserId) {
        SubjectStudentReview review = subjectReviews.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("SubjectStudentReview", "id", reviewId));
        if (!review.getAcademicYear().getId().equals(request.academicYearId())) {
            throw new ForbiddenException("Nhận xét không thuộc năm học đã chọn");
        }
        requireHomeroomAssignment(review.getAcademicYear().getId(), review.getCls().getId(), teacherUserId);
        if (review.getStatus() != SubjectReviewStatus.SUBMITTED) {
            throw new ConflictException("Chỉ có thể trả lại nhận xét đã gửi");
        }
        reports.findByStudentIdAndSemesterId(review.getStudent().getId(), review.getSemester().getId())
                .filter(report -> report.getStatus() == PeriodicReportStatus.PUBLISHED)
                .ifPresent(report -> {
                    throw new ConflictException("Báo cáo đã công bố; Admin phải mở lại trước khi trả nhận xét môn");
                });
        User actor = requireUser(teacherUserId);
        Map<String, Object> oldValue = reviewSnapshot(review);
        review.setStatus(SubjectReviewStatus.RETURNED);
        review.setReturnReason(request.reason().trim());
        review = subjectReviews.save(review);
        audit("SUBJECT_REVIEW", review.getId(), oldValue, reviewSnapshot(review), actor,
                request.reason().trim());
        return toSubjectDto(review);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HomeroomReportDto> getHomeroomReports(Long academicYearId, Long semesterId, Long classId,
            Long teacherUserId) {
        Scope scope = requireScope(academicYearId, semesterId, classId);
        Teacher homeroomTeacher = requireHomeroomAssignment(academicYearId, classId, teacherUserId);
        return roster(scope).stream().map(student -> buildReport(scope, student, homeroomTeacher)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public HomeroomReportDto getHomeroomReport(Long studentId, Long academicYearId, Long semesterId,
            Long classId, Long teacherUserId) {
        Scope scope = requireScope(academicYearId, semesterId, classId);
        Teacher homeroomTeacher = requireHomeroomAssignment(academicYearId, classId, teacherUserId);
        return buildReport(scope, requireRosterStudent(scope, studentId), homeroomTeacher);
    }

    @Override
    public HomeroomReportDto saveHomeroomReport(Long studentId, SavePeriodicReportRequest request,
            Long teacherUserId) {
        Scope scope = requireScope(request.academicYearId(), request.semesterId(), request.classId());
        Teacher homeroomTeacher = requireHomeroomAssignment(request.academicYearId(), request.classId(), teacherUserId);
        Student student = requireRosterStudent(scope, studentId);
        String conduct = trim(request.conduct());
        if (conduct != null && !CONDUCT_VALUES.contains(conduct)) {
            throw new BadRequestException("Hạnh kiểm không hợp lệ");
        }
        StudentPeriodicReport report = reports.findByStudentIdAndSemesterId(studentId, request.semesterId())
                .orElseGet(StudentPeriodicReport::new);
        if (report.getId() != null && report.getStatus() == PeriodicReportStatus.PUBLISHED) {
            throw new ConflictException("Báo cáo đã công bố; Admin phải mở lại trước khi sửa");
        }
        Map<String, Object> oldValue = reportSnapshot(report);
        report.setAcademicYear(scope.year());
        report.setSemester(scope.semester());
        report.setCls(scope.cls());
        report.setStudent(student);
        report.setHomeroomTeacher(homeroomTeacher);
        report.setGeneralComment(trim(request.generalComment()));
        report.setConduct(conduct);
        report.setStatus(PeriodicReportStatus.DRAFT);
        report = reports.save(report);
        audit("PERIODIC_REPORT", report.getId(), oldValue, reportSnapshot(report), requireUser(teacherUserId),
                "SAVE_DRAFT");
        return buildReport(scope, student, homeroomTeacher);
    }

    @Override
    public HomeroomReportDto publishStudent(Long studentId, ReportScopeRequest request, Long teacherUserId) {
        Scope scope = requireScope(request.academicYearId(), request.semesterId(), request.classId());
        Teacher homeroomTeacher = requireHomeroomAssignment(request.academicYearId(), request.classId(), teacherUserId);
        Student student = requireRosterStudent(scope, studentId);
        publish(scope, student, homeroomTeacher, requireUser(teacherUserId));
        return buildReport(scope, student, homeroomTeacher);
    }

    @Override
    public List<HomeroomReportDto> publishClass(ReportScopeRequest request, Long teacherUserId) {
        Scope scope = requireScope(request.academicYearId(), request.semesterId(), request.classId());
        Teacher homeroomTeacher = requireHomeroomAssignment(request.academicYearId(), request.classId(), teacherUserId);
        User actor = requireUser(teacherUserId);
        List<Student> students = roster(scope);
        students.forEach(student -> validatePublish(scope, student));
        students.forEach(student -> publish(scope, student, homeroomTeacher, actor));
        return students.stream().map(student -> buildReport(scope, student, homeroomTeacher)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public HomeroomReportDto getPublishedReport(Long studentId, Long academicYearId, Long semesterId,
            Long requesterId, UserRole requesterRole) {
        requireYearSemester(academicYearId, semesterId);
        authorizeStudent(requesterId, requesterRole, studentId);
        StudentPeriodicReport report = reports
                .findByStudentIdAndSemesterIdAndStatus(studentId, semesterId, PeriodicReportStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("PublishedPeriodicReport", "studentId", studentId));
        if (!report.getAcademicYear().getId().equals(academicYearId)) {
            throw new ForbiddenException("Báo cáo không thuộc năm học đã chọn");
        }
        return buildReport(new Scope(report.getAcademicYear(), report.getSemester(), report.getCls()),
                report.getStudent(), report.getHomeroomTeacher());
    }

    @Override
    @Transactional(readOnly = true)
    public List<HomeroomReportDto> getAdminReports(Long academicYearId, Long semesterId, Long classId,
            PeriodicReportStatus status) {
        requireYearSemester(academicYearId, semesterId);
        List<SchoolClass> selectedClasses;
        if (classId == null) {
            selectedClasses = classes.findByAcademicYearId(academicYearId);
        } else {
            SchoolClass cls = requireClass(classId);
            if (!cls.getAcademicYear().getId().equals(academicYearId)) {
                throw new ForbiddenException("Lớp không thuộc năm học đã chọn");
            }
            selectedClasses = List.of(cls);
        }
        List<HomeroomReportDto> result = new ArrayList<>();
        Set<Long> seenStudents = new HashSet<>();
        for (SchoolClass cls : selectedClasses) {
            Scope scope = requireScope(academicYearId, semesterId, cls.getId());
            Teacher homeroom = homeroomAssignments.findActiveByClassAndYear(cls.getId(), academicYearId)
                    .map(HomeroomAssignment::getTeacher).orElse(null);
            for (Student student : roster(scope)) {
                HomeroomReportDto dto = buildReport(scope, student, homeroom);
                if (status == null || dto.status() == status) result.add(dto);
                seenStudents.add(student.getId());
            }
        }
        for (StudentPeriodicReport report : reports.findByAcademicYearIdAndSemesterId(academicYearId, semesterId)) {
            if ((classId == null || report.getCls().getId().equals(classId)) && !seenStudents.contains(report.getStudent().getId())) {
                HomeroomReportDto dto = buildReport(
                        new Scope(report.getAcademicYear(), report.getSemester(), report.getCls()),
                        report.getStudent(), report.getHomeroomTeacher());
                if (status == null || dto.status() == status) result.add(dto);
            }
        }
        return result.stream().sorted(Comparator.comparing(HomeroomReportDto::className)
                .thenComparing(HomeroomReportDto::studentCode)).toList();
    }

    @Override
    public HomeroomReportDto reopenReport(Long reportId, ReopenPeriodicReportRequest request, Long adminUserId) {
        StudentPeriodicReport report = reports.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("StudentPeriodicReport", "id", reportId));
        if (!report.getAcademicYear().getId().equals(request.academicYearId())) {
            throw new ForbiddenException("Báo cáo không thuộc năm học đã chọn");
        }
        if (report.getStatus() != PeriodicReportStatus.PUBLISHED) {
            throw new ConflictException("Chỉ có thể mở lại báo cáo đã công bố");
        }
        Map<String, Object> oldValue = reportSnapshot(report);
        report.setStatus(PeriodicReportStatus.DRAFT);
        report.setPublishedAt(null);
        report = reports.save(report);
        semesterResults.findByStudentIdAndSemesterId(report.getStudent().getId(), report.getSemester().getId())
                .ifPresent(result -> {
                    result.setConduct(result.getSuggestedConduct());
                    result.setConductSource(ConductSource.SUGGESTED);
                    semesterResults.save(result);
                });
        audit("PERIODIC_REPORT", report.getId(), oldValue, reportSnapshot(report), requireUser(adminUserId),
                request.reason().trim());
        return buildReport(new Scope(report.getAcademicYear(), report.getSemester(), report.getCls()),
                report.getStudent(), report.getHomeroomTeacher());
    }

    private void publish(Scope scope, Student student, Teacher homeroomTeacher, User actor) {
        validatePublish(scope, student);
        StudentPeriodicReport report = reports.findByStudentIdAndSemesterId(student.getId(), scope.semester().getId())
                .orElseThrow(() -> new ConflictException("Chưa có nhận xét chung và hạnh kiểm"));
        if (report.getStatus() == PeriodicReportStatus.PUBLISHED) return;
        Map<String, Object> oldValue = reportSnapshot(report);
        report.setStatus(PeriodicReportStatus.PUBLISHED);
        report.setPublishedAt(LocalDateTime.now());
        report = reports.save(report);
        SemesterResult result = semesterResults.findByStudentIdAndSemesterId(student.getId(), scope.semester().getId())
                .orElseGet(SemesterResult::new);
        result.setStudent(student);
        result.setSemester(scope.semester());
        result.setCls(scope.cls());
        result.setConduct(report.getConduct());
        result.setConductSource(ConductSource.HOMEROOM);
        semesterResults.save(result);
        audit("PERIODIC_REPORT", report.getId(), oldValue, reportSnapshot(report), actor, "PUBLISH");
    }

    private void validatePublish(Scope scope, Student student) {
        StudentPeriodicReport report = reports.findByStudentIdAndSemesterId(student.getId(), scope.semester().getId())
                .orElseThrow(() -> new ConflictException("Chưa có nhận xét chung và hạnh kiểm cho "
                        + student.getStudentCode()));
        if (trim(report.getGeneralComment()) == null || trim(report.getConduct()) == null) {
            throw new ConflictException("Chưa đủ nhận xét chung hoặc hạnh kiểm cho " + student.getStudentCode());
        }
        List<TeachingAssignment> required = requiredAssignments(scope);
        if (required.isEmpty()) throw new ConflictException("Lớp chưa có phân công môn học");
        Set<Long> submittedSubjectIds = subjectReviews
                .findByStudentIdAndSemesterId(student.getId(), scope.semester().getId()).stream()
                .filter(review -> review.getStatus() == SubjectReviewStatus.SUBMITTED)
                .filter(review -> review.getCls().getId().equals(scope.cls().getId()))
                .map(review -> review.getSubject().getId()).collect(java.util.stream.Collectors.toSet());
        List<String> missing = required.stream().filter(item -> !submittedSubjectIds.contains(item.getSubject().getId()))
                .map(item -> item.getSubject().getName()).distinct().toList();
        if (!missing.isEmpty()) throw new ConflictException("Còn thiếu nhận xét môn: " + String.join(", ", missing));
    }

    private HomeroomReportDto buildReport(Scope scope, Student student, Teacher homeroomTeacher) {
        StudentPeriodicReport report = reports.findByStudentIdAndSemesterId(student.getId(), scope.semester().getId())
                .orElse(null);
        List<TeachingAssignment> required = requiredAssignments(scope);
        Map<Long, SubjectStudentReview> reviewsBySubject = new LinkedHashMap<>();
        subjectReviews.findByStudentIdAndSemesterId(student.getId(), scope.semester().getId()).stream()
                .filter(review -> review.getCls().getId().equals(scope.cls().getId()))
                .forEach(review -> reviewsBySubject.put(review.getSubject().getId(), review));
        List<SubjectReviewDto> reviewDtos = required.stream().map(assignment -> {
            SubjectStudentReview review = reviewsBySubject.get(assignment.getSubject().getId());
            return review == null ? emptySubjectDto(scope, assignment, student) : toSubjectDto(review);
        }).toList();
        int submitted = (int) reviewDtos.stream().filter(item -> item.status() == SubjectReviewStatus.SUBMITTED).count();
        List<String> missing = reviewDtos.stream().filter(item -> item.status() != SubjectReviewStatus.SUBMITTED)
                .map(SubjectReviewDto::subjectName).distinct().toList();
        String suggested = semesterResults.findByStudentIdAndSemesterId(student.getId(), scope.semester().getId())
                .map(SemesterResult::getSuggestedConduct).orElse(null);
        PeriodicReportStatus status = report == null ? PeriodicReportStatus.DRAFT : report.getStatus();
        Teacher owner = report == null ? homeroomTeacher : report.getHomeroomTeacher();
        return new HomeroomReportDto(report == null ? null : report.getId(), scope.year().getId(),
                scope.semester().getId(), scope.cls().getId(), scope.cls().getName(), student.getId(),
                student.getUser().getName(), student.getStudentCode(), owner == null ? null : owner.getId(),
                owner == null ? null : owner.getUser().getName(), report == null ? null : report.getGeneralComment(),
                report == null ? null : report.getConduct(), suggested, status,
                report == null ? null : report.getPublishedAt(), submitted, required.size(), missing, reviewDtos);
    }

    private List<TeachingAssignment> requiredAssignments(Scope scope) {
        return assignments.findByClsIdAndStatus(scope.cls().getId(), AssignmentStatus.ACTIVE).stream()
                .filter(item -> overlaps(item, scope.semester()))
                .sorted(Comparator.comparing(item -> item.getSubject().getName()))
                .toList();
    }

    private TeachingAssignment requireSubjectAssignment(Scope scope, Long subjectId, Long teacherUserId) {
        Teacher teacher = teachers.findByUserId(teacherUserId)
                .orElseThrow(() -> new ForbiddenException("Không tìm thấy hồ sơ giáo viên"));
        TeachingAssignment assignment = assignments.findByClsIdAndSubjectId(scope.cls().getId(), subjectId)
                .orElseThrow(() -> new ForbiddenException("Giáo viên chưa được phân công lớp/môn này"));
        if (assignment.getStatus() != AssignmentStatus.ACTIVE
                || !assignment.getTeacher().getId().equals(teacher.getId())
                || !overlaps(assignment, scope.semester())) {
            throw new ForbiddenException("Giáo viên chưa được phân công lớp/môn trong học kỳ này");
        }
        return assignment;
    }

    private Teacher requireHomeroomAssignment(Long academicYearId, Long classId, Long teacherUserId) {
        Teacher teacher = teachers.findByUserId(teacherUserId)
                .orElseThrow(() -> new ForbiddenException("Không tìm thấy hồ sơ giáo viên"));
        HomeroomAssignment assignment = homeroomAssignments.findActiveByClassAndYear(classId, academicYearId)
                .orElseThrow(() -> new ForbiddenException("Lớp chưa có giáo viên chủ nhiệm đang phụ trách"));
        if (!assignment.getTeacher().getId().equals(teacher.getId())) {
            throw new ForbiddenException("Giáo viên chỉ được xem báo cáo lớp chủ nhiệm");
        }
        return teacher;
    }

    private Scope requireScope(Long academicYearId, Long semesterId, Long classId) {
        AcademicYear year = requireYear(academicYearId);
        Semester semester = requireSemester(semesterId);
        SchoolClass cls = requireClass(classId);
        if (!semester.getAcademicYear().getId().equals(academicYearId)
                || !cls.getAcademicYear().getId().equals(academicYearId)) {
            throw new ForbiddenException("Năm học, học kỳ và lớp không cùng phạm vi");
        }
        return new Scope(year, semester, cls);
    }

    private void requireYearSemester(Long academicYearId, Long semesterId) {
        requireYear(academicYearId);
        if (!requireSemester(semesterId).getAcademicYear().getId().equals(academicYearId)) {
            throw new ForbiddenException("Học kỳ không thuộc năm học đã chọn");
        }
    }

    private AcademicYear requireYear(Long id) {
        return academicYears.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AcademicYear", "id", id));
    }

    private Semester requireSemester(Long id) {
        return semesters.findById(id).orElseThrow(() -> new ResourceNotFoundException("Semester", "id", id));
    }

    private SchoolClass requireClass(Long id) {
        return classes.findById(id).orElseThrow(() -> new ResourceNotFoundException("Class", "id", id));
    }

    private Student requireRosterStudent(Scope scope, Long studentId) {
        if (!enrollments.existsByStudentIdAndClsIdAndAcademicYearIdAndStatus(studentId, scope.cls().getId(),
                scope.year().getId(), EnrollmentStatus.ACTIVE)) {
            throw new ForbiddenException("Học sinh không thuộc lớp trong năm học đã chọn");
        }
        return students.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", studentId));
    }

    private List<Student> roster(Scope scope) {
        return enrollments.findActiveStudentsByClassAndYear(scope.cls().getId(), scope.year().getId()).stream()
                .sorted(Comparator.comparing(Student::getStudentCode)).toList();
    }

    private void authorizeStudent(Long requesterId, UserRole requesterRole, Long studentId) {
        if (requesterRole == UserRole.STUDENT) {
            Student student = students.findByUserId(requesterId)
                    .orElseThrow(() -> new ForbiddenException("Không tìm thấy hồ sơ học sinh"));
            if (!student.getId().equals(studentId)) throw new ForbiddenException("Chỉ được xem báo cáo của chính mình");
            return;
        }
        if (requesterRole == UserRole.PARENT) {
            Parent parent = parents.findByUserId(requesterId)
                    .orElseThrow(() -> new ForbiddenException("Không tìm thấy hồ sơ phụ huynh"));
            if (!guardians.existsByStudentIdAndGuardianId(studentId, parent.getId())) {
                throw new ForbiddenException("Học sinh chưa liên kết với phụ huynh");
            }
            return;
        }
        throw new ForbiddenException("Chức năng chỉ dành cho phụ huynh và học sinh");
    }

    private boolean overlaps(TeachingAssignment assignment, Semester semester) {
        return !assignment.getEffectiveFrom().isAfter(semester.getEndDate())
                && (assignment.getEffectiveTo() == null || !assignment.getEffectiveTo().isBefore(semester.getStartDate()));
    }

    private SubjectReviewDto emptySubjectDto(Scope scope, TeachingAssignment assignment, Student student) {
        return new SubjectReviewDto(null, scope.year().getId(), scope.semester().getId(), scope.cls().getId(),
                scope.cls().getName(), student.getId(), student.getUser().getName(), student.getStudentCode(),
                assignment.getSubject().getId(), assignment.getSubject().getName(), assignment.getTeacher().getId(),
                assignment.getTeacher().getUser().getName(), null, null, null, SubjectReviewStatus.DRAFT,
                null, null, null);
    }

    private SubjectReviewDto toSubjectDto(SubjectStudentReview review) {
        return new SubjectReviewDto(review.getId(), review.getAcademicYear().getId(), review.getSemester().getId(),
                review.getCls().getId(), review.getCls().getName(), review.getStudent().getId(),
                review.getStudent().getUser().getName(), review.getStudent().getStudentCode(),
                review.getSubject().getId(), review.getSubject().getName(), review.getSubjectTeacher().getId(),
                review.getSubjectTeacher().getUser().getName(), review.getComment(), review.getStrengths(),
                review.getImprovements(), review.getStatus(), review.getReturnReason(), review.getSubmittedAt(),
                review.getUpdatedAt());
    }

    private User requireUser(Long id) {
        return users.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    private String trim(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private Map<String, Object> reviewSnapshot(SubjectStudentReview review) {
        if (review.getId() == null) return Map.of();
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("comment", review.getComment());
        value.put("strengths", review.getStrengths());
        value.put("improvements", review.getImprovements());
        value.put("status", review.getStatus());
        value.put("returnReason", review.getReturnReason());
        return value;
    }

    private Map<String, Object> reportSnapshot(StudentPeriodicReport report) {
        if (report.getId() == null) return Map.of();
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("generalComment", report.getGeneralComment());
        value.put("conduct", report.getConduct());
        value.put("status", report.getStatus());
        value.put("publishedAt", report.getPublishedAt());
        return value;
    }

    private void audit(String type, Long entityId, Object oldValue, Object newValue, User actor, String reason) {
        StudentReviewAudit audit = new StudentReviewAudit();
        audit.setEntityType(type);
        audit.setEntityId(entityId);
        audit.setOldValueJson(json(oldValue));
        audit.setNewValueJson(json(newValue));
        audit.setChangedBy(actor);
        audit.setReason(reason);
        audit.setChangedAt(LocalDateTime.now());
        audits.save(audit);
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Không thể tạo dữ liệu audit", exception);
        }
    }

    private record Scope(AcademicYear year, Semester semester, SchoolClass cls) {}
}
