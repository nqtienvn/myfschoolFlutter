package vn.edu.fpt.myfschool.service.impl;

import vn.edu.fpt.myfschool.entity.Attendance;
import vn.edu.fpt.myfschool.entity.AttendanceDetail;
import vn.edu.fpt.myfschool.entity.LeaveRequest;
import vn.edu.fpt.myfschool.entity.Parent;
import vn.edu.fpt.myfschool.entity.SchoolClass;
import vn.edu.fpt.myfschool.entity.Student;
import vn.edu.fpt.myfschool.entity.Teacher;
import vn.edu.fpt.myfschool.entity.Enrollment;
import vn.edu.fpt.myfschool.entity.Semester;
import vn.edu.fpt.myfschool.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.enums.LeaveStatus;
import vn.edu.fpt.myfschool.common.enums.LeaveShift;
import vn.edu.fpt.myfschool.common.exception.BadRequestException;
import vn.edu.fpt.myfschool.common.exception.ForbiddenException;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.repository.*;

import vn.edu.fpt.myfschool.entity.HomeroomAssignment;
import vn.edu.fpt.myfschool.entity.User;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.time.LocalDate;
import java.util.stream.Collectors;

@Service("leaveRequestService")
@RequiredArgsConstructor
@Transactional
public class LeaveRequestServiceImpl implements LeaveRequestService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;
    private final TeacherRepository teacherRepository;
    private final StudentGuardianRepository studentGuardianRepository;
    private final AttendanceRepository attendanceRepository;
    private final AttendanceSessionRepository attendanceSessionRepository;
    private final AttendanceDetailRepository attendanceDetailRepository;
    private final ClassRepository classRepository;
    private final HomeroomAssignmentRepository homeroomAssignmentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AcademicYearShiftRepository academicYearShiftRepository;
    private final NotificationService notificationService;
    private final SemesterRepository semesterRepository;

    @Override
    public LeaveRequestDto createLeaveRequest(CreateLeaveRequestRequest request, Long parentUserId) {
        Parent parent = requireParent(parentUserId);
        Student student = studentRepository.findById(request.studentId())
            .orElseThrow(() -> new ResourceNotFoundException("Student", "id", request.studentId()));
        assertParentOwnsStudent(parent, student.getId());

        if (request.dateFrom().isAfter(request.dateTo())) {
            throw new BadRequestException("Ngày bắt đầu phải trước ngày kết thúc");
        }

        List<Enrollment> matchingEnrollments = enrollmentRepository.findActiveForStudentAndDateRange(
            student.getId(), request.dateFrom(), request.dateTo());
        if (matchingEnrollments.size() != 1) {
            throw new BadRequestException("Khoảng ngày nghỉ không thuộc đúng một năm học đang hoạt động");
        }
        Enrollment enrollment = matchingEnrollments.get(0);
        SchoolClass cls = enrollment.getCls();
        validateLeaveShiftConfigured(enrollment, request.shift());
        HomeroomAssignment reviewingAssignment = homeroomAssignmentRepository
            .findByClsIdAndAcademicYearId(
                cls.getId(), enrollment.getAcademicYear().getId())
            .stream()
            .filter(assignment -> !assignment.getEffectiveFrom().isAfter(request.dateFrom()))
            .filter(assignment -> assignment.getEffectiveTo() == null
                || !assignment.getEffectiveTo().isBefore(request.dateTo()))
            .max(Comparator.comparing(HomeroomAssignment::getEffectiveFrom))
            .orElseThrow(() -> new BadRequestException(
                "Khoảng ngày nghỉ phải nằm trọn trong thời gian phân công của một giáo viên chủ nhiệm"));

        long overlapping = leaveRequestRepository.countOverlappingPending(
            request.studentId(), request.dateFrom(), request.dateTo());
        if (overlapping > 0) {
            throw new BadRequestException("Học sinh đã có đơn chờ duyệt cho khoảng ngày này");
        }

        LeaveRequest lr = new LeaveRequest();
        lr.setStudent(student);
        lr.setParent(parent);
        lr.setCls(cls);
        lr.setAcademicYear(enrollment.getAcademicYear());
        lr.setDateFrom(request.dateFrom());
        lr.setDateTo(request.dateTo());
        lr.setShift(request.shift());
        lr.setReason(request.reason());
        lr.setStatus(LeaveStatus.PENDING);
        lr = leaveRequestRepository.save(lr);

        // Notify the same homeroom teacher who is allowed to review the full request range.
        User teacherUser = reviewingAssignment.getTeacher().getUser();
        notificationService.createNotification(
            teacherUser.getId(),
            "Đơn xin nghỉ học mới",
            "Học sinh " + student.getUser().getName() + " có đơn xin nghỉ từ ngày " + request.dateFrom() + " đến " + request.dateTo(),
            "Đơn xin nghỉ", lr.getId(), "LEAVE_REQUEST"
        );

        return toDto(lr);
    }

    @Transactional(readOnly = true)
    @Override
    public List<LeaveRequestDto> getParentLeaveRequests(
            Long parentUserId, Long studentId, Long semesterId) {
        Parent parent = requireParent(parentUserId);
        if (studentId != null) {
            assertParentOwnsStudent(parent, studentId);
        }
        Semester semester = semesterId == null ? null : semesterRepository.findById(semesterId)
            .orElseThrow(() -> new ResourceNotFoundException("Semester", "id", semesterId));
        if (semester != null && studentId != null
                && enrollmentRepository.findFirstByStudentIdAndAcademicYearIdOrderByIdDesc(
                    studentId, semester.getAcademicYear().getId()).isEmpty()) {
            throw new BadRequestException("Học sinh không thuộc năm học của học kỳ đã chọn");
        }
        return leaveRequestRepository.findByParentIdOrderByCreatedAtDesc(parent.getId())
            .stream()
            .filter(request -> studentId == null || request.getStudent().getId().equals(studentId))
            .filter(request -> semester == null
                || (request.getAcademicYear().getId().equals(semester.getAcademicYear().getId())
                    && !request.getDateTo().isBefore(semester.getStartDate())
                    && !request.getDateFrom().isAfter(semester.getEndDate())))
            .map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Override
    public List<LeaveRequestDto> getStudentLeaveRequests(Long studentUserId) {
        Student student = studentRepository.findByUserId(studentUserId)
            .orElseThrow(() -> new ResourceNotFoundException("Student", "userId", studentUserId));
        return leaveRequestRepository.findByStudentIdOrderByCreatedAtDesc(student.getId())
            .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Override
    public List<LeaveRequestDto> getPendingLeaveRequests(
            Long teacherUserId, Long academicYearId, Long semesterId) {
        Teacher teacher = requireTeacher(teacherUserId);
        TeacherLeaveScope scope = resolveTeacherLeaveScope(
            teacher, academicYearId, semesterId);
        if (scope != null) {
            return findRequestsInScope(scope, List.of(LeaveStatus.PENDING), false)
                .stream().map(this::toDto).toList();
        }
        List<HomeroomAssignment> assignments = getTeacherAssignments(teacher);
        List<Long> classIds = assignmentClassIds(assignments);

        return leaveRequestRepository.findByClassIdsAndStatusOrderByCreatedAtDesc(classIds, LeaveStatus.PENDING)
            .stream()
            .filter(request -> isRequestWithinAssignments(request, assignments))
            .map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Override
    public List<LeaveRequestDto> getReviewedLeaveRequests(
            Long teacherUserId, Long academicYearId, Long semesterId) {
        Teacher teacher = requireTeacher(teacherUserId);
        TeacherLeaveScope scope = resolveTeacherLeaveScope(
            teacher, academicYearId, semesterId);
        if (scope != null) {
            return findRequestsInScope(
                    scope, List.of(LeaveStatus.APPROVED, LeaveStatus.REJECTED), true)
                .stream().map(this::toDto).toList();
        }
        List<HomeroomAssignment> assignments = getTeacherAssignments(teacher);
        List<Long> classIds = assignmentClassIds(assignments);

        return leaveRequestRepository.findReviewedInActiveYear(
                classIds, List.of(LeaveStatus.APPROVED, LeaveStatus.REJECTED))
            .stream()
            .filter(request -> isRequestWithinAssignments(request, assignments))
            .map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Override
    public List<LeaveRequestDto> getClassLeaveRequests(Long classId, LeaveStatus status, Long teacherUserId) {
        Teacher teacher = requireTeacher(teacherUserId);
        SchoolClass cls = assertTeacherCanAccessClass(
            teacher, classId, LocalDate.now());

        List<LeaveRequest> requests = status != null
            ? leaveRequestRepository.findByClassIdAndStatusOrderByCreatedAtDesc(classId, status)
            : leaveRequestRepository.findByClassIdOrderByCreatedAtDesc(classId);
        List<HomeroomAssignment> assignments = homeroomAssignmentRepository
            .findByTeacherIdAndAcademicYearId(
                teacher.getId(), cls.getAcademicYear().getId());
        return requests.stream()
            .filter(request -> isRequestWithinAssignments(request, assignments))
            .map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public LeaveRequestDto approveLeaveRequest(Long leaveRequestId, String response, Long teacherUserId) {
        LeaveRequest lr = leaveRequestRepository.findById(leaveRequestId)
            .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", "id", leaveRequestId));
        if (lr.getStatus() != LeaveStatus.PENDING) {
            throw new BadRequestException("Đơn đã được xử lý");
        }

        Teacher teacher = requireTeacher(teacherUserId);
        assertTeacherCanReviewRequest(teacher, lr);

        lr.setStatus(LeaveStatus.APPROVED);
        lr.setApprovedBy(teacher);
        lr.setResponse(response);
        lr.setApprovedAt(LocalDateTime.now());
        lr = leaveRequestRepository.save(lr);

        // Auto-update attendance
        autoUpdateAttendance(lr);

        // Notify Parent
        notificationService.createNotification(
            lr.getParent().getUser().getId(),
            "Đơn xin nghỉ học được phê duyệt",
            "Đơn xin nghỉ học của " + lr.getStudent().getUser().getName() + " từ ngày " + lr.getDateFrom() + " đã được phê duyệt.",
            "Đơn xin nghỉ", lr.getId(), "LEAVE_REQUEST"
        );
        // Notify Student
        notificationService.createNotification(
            lr.getStudent().getUser().getId(),
            "Đơn xin nghỉ học được phê duyệt",
            "Đơn xin nghỉ học từ ngày " + lr.getDateFrom() + " của bạn đã được phê duyệt.",
            "Đơn xin nghỉ", lr.getId(), "LEAVE_REQUEST"
        );

        return toDto(lr);
    }

    @Override
    public LeaveRequestDto rejectLeaveRequest(Long leaveRequestId, String response, Long teacherUserId) {
        LeaveRequest lr = leaveRequestRepository.findById(leaveRequestId)
            .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", "id", leaveRequestId));
        if (lr.getStatus() != LeaveStatus.PENDING) {
            throw new BadRequestException("Đơn đã được xử lý");
        }

        Teacher teacher = requireTeacher(teacherUserId);
        assertTeacherCanReviewRequest(teacher, lr);

        lr.setStatus(LeaveStatus.REJECTED);
        lr.setApprovedBy(teacher);
        lr.setResponse(response);
        lr.setApprovedAt(LocalDateTime.now());
        lr = leaveRequestRepository.save(lr);

        // Notify Parent
        notificationService.createNotification(
            lr.getParent().getUser().getId(),
            "Đơn xin nghỉ học bị từ chối",
            "Đơn xin nghỉ học của " + lr.getStudent().getUser().getName() + " từ ngày " + lr.getDateFrom() + " đã bị từ chối. Lý do: " + (response != null ? response : "Không có lý do cụ thể"),
            "Đơn xin nghỉ", lr.getId(), "LEAVE_REQUEST"
        );
        // Notify Student
        notificationService.createNotification(
            lr.getStudent().getUser().getId(),
            "Đơn xin nghỉ học bị từ chối",
            "Đơn xin nghỉ học từ ngày " + lr.getDateFrom() + " của bạn đã bị từ chối.",
            "Đơn xin nghỉ", lr.getId(), "LEAVE_REQUEST"
        );

        return toDto(lr);
    }

    @Override
    public void cancelLeaveRequest(Long leaveRequestId, Long parentUserId) {
        Parent parent = requireParent(parentUserId);
        LeaveRequest lr = leaveRequestRepository.findById(leaveRequestId)
            .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", "id", leaveRequestId));
        if (!lr.getParent().getId().equals(parent.getId())) {
            throw new ForbiddenException("Phụ huynh không có quyền hủy đơn này");
        }
        if (lr.getStatus() != LeaveStatus.PENDING) {
            throw new BadRequestException("Chỉ có thể hủy đơn đang chờ duyệt");
        }
        leaveRequestRepository.delete(lr);
    }

    @Transactional(readOnly = true)
    @Override
    public long getPendingCount(
            Long teacherUserId, Long academicYearId, Long semesterId) {
        Teacher teacher = requireTeacher(teacherUserId);
        TeacherLeaveScope scope = resolveTeacherLeaveScope(
            teacher, academicYearId, semesterId);
        if (scope != null) {
            return findRequestsInScope(scope, List.of(LeaveStatus.PENDING), false).size();
        }
        List<HomeroomAssignment> assignments = getTeacherAssignments(teacher);
        return leaveRequestRepository.findByClassIdsAndStatusOrderByCreatedAtDesc(
                assignmentClassIds(assignments), LeaveStatus.PENDING)
            .stream()
            .filter(request -> isRequestWithinAssignments(request, assignments))
            .count();
    }

    private TeacherLeaveScope resolveTeacherLeaveScope(
            Teacher teacher, Long academicYearId, Long semesterId) {
        if (academicYearId == null && semesterId == null) return null;
        if (academicYearId == null || semesterId == null) {
            throw new BadRequestException(
                "academicYearId và semesterId phải được cung cấp cùng nhau");
        }

        Semester semester = semesterRepository.findById(semesterId)
            .orElseThrow(() -> new ResourceNotFoundException("Semester", "id", semesterId));
        if (!semester.getAcademicYear().getId().equals(academicYearId)) {
            throw new BadRequestException("Học kỳ không thuộc năm học đã chọn");
        }

        List<HomeroomAssignment> assignments = homeroomAssignmentRepository
            .findByTeacherIdAndAcademicYearId(teacher.getId(), academicYearId)
            .stream()
            .filter(assignment -> !assignment.getEffectiveFrom().isAfter(semester.getEndDate()))
            .filter(assignment -> assignment.getEffectiveTo() == null
                || !assignment.getEffectiveTo().isBefore(semester.getStartDate()))
            .toList();
        if (assignments.isEmpty()) {
            throw new ForbiddenException(
                "Giáo viên không có lớp chủ nhiệm hiệu lực trong học kỳ đã chọn");
        }
        return new TeacherLeaveScope(
            academicYearId, semester.getStartDate(), semester.getEndDate(), assignments);
    }

    private List<LeaveRequest> findRequestsInScope(
            TeacherLeaveScope scope, List<LeaveStatus> statuses, boolean reviewed) {
        Comparator<LeaveRequest> ordering = reviewed
            ? Comparator.comparing(
                    LeaveRequest::getApprovedAt,
                    Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(
                    LeaveRequest::getCreatedAt,
                    Comparator.nullsLast(Comparator.reverseOrder()))
            : Comparator.comparing(
                LeaveRequest::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder()));

        return assignmentClassIds(scope.assignments()).stream()
            .flatMap(classId -> leaveRequestRepository
                .findByClassIdOrderByCreatedAtDesc(classId).stream())
            .filter(request -> request.getAcademicYear().getId().equals(scope.academicYearId()))
            .filter(request -> statuses.contains(request.getStatus()))
            .filter(request -> !request.getDateTo().isBefore(scope.startDate())
                && !request.getDateFrom().isAfter(scope.endDate()))
            .filter(request -> isRequestWithinAssignments(request, scope.assignments()))
            .sorted(ordering)
            .toList();
    }

    private void autoUpdateAttendance(LeaveRequest lr) {
        java.time.LocalDate current = lr.getDateFrom();
        while (!current.isAfter(lr.getDateTo())) {
            if (lr.getShift() == vn.edu.fpt.myfschool.common.enums.LeaveShift.FULL_DAY ||
                lr.getShift() == vn.edu.fpt.myfschool.common.enums.LeaveShift.MORNING) {
                reconcileExistingAttendance(lr, current, vn.edu.fpt.myfschool.common.enums.Shift.MORNING);
            }
            if (lr.getShift() == vn.edu.fpt.myfschool.common.enums.LeaveShift.FULL_DAY ||
                lr.getShift() == vn.edu.fpt.myfschool.common.enums.LeaveShift.AFTERNOON) {
                reconcileExistingAttendance(lr, current, vn.edu.fpt.myfschool.common.enums.Shift.AFTERNOON);
            }
            current = current.plusDays(1);
        }
    }

    private void reconcileExistingAttendance(
            LeaveRequest lr, java.time.LocalDate date,
            vn.edu.fpt.myfschool.common.enums.Shift shift) {
        attendanceRepository.findByStudentIdAndDateAndShift(lr.getStudent().getId(), date, shift)
            .ifPresent(attendance -> {
                if (attendance.getStatus()
                        == vn.edu.fpt.myfschool.common.enums.AttendanceStatus.ABSENT_WITHOUT_LEAVE) {
                    attendance.setStatus(
                        vn.edu.fpt.myfschool.common.enums.AttendanceStatus.ABSENT_WITH_LEAVE);
                    attendance.setLeaveRequest(lr);
                    attendanceRepository.save(attendance);
                } else if (attendance.getStatus()
                        == vn.edu.fpt.myfschool.common.enums.AttendanceStatus.ABSENT_WITH_LEAVE
                        && attendance.getLeaveRequest() == null) {
                    attendance.setLeaveRequest(lr);
                    attendanceRepository.save(attendance);
                }
                synchronizeSessionDetailFromCanonical(attendance, date, shift);
            });
    }

    private void synchronizeSessionDetailFromCanonical(
            Attendance attendance, LocalDate date,
            vn.edu.fpt.myfschool.common.enums.Shift shift) {
        attendanceSessionRepository.findByClsIdAndDateAndShift(
                attendance.getCls().getId(), date, shift).forEach(session -> {
            AttendanceDetail detail = attendanceDetailRepository
                .findBySessionIdAndStudentId(
                    session.getId(), attendance.getStudent().getId())
                .orElseGet(() -> {
                    AttendanceDetail created = new AttendanceDetail();
                    created.setSession(session);
                    created.setStudent(attendance.getStudent());
                    return created;
                });
            detail.setStatus(attendance.getStatus());
            if (attendance.getStatus()
                    == vn.edu.fpt.myfschool.common.enums.AttendanceStatus.ABSENT_WITH_LEAVE
                    && attendance.getLeaveRequest() != null) {
                detail.setNote(
                    "Nghỉ có phép - đơn #" + attendance.getLeaveRequest().getId() + " đã duyệt");
            }
            attendanceDetailRepository.save(detail);

            List<AttendanceDetail> details =
                attendanceDetailRepository.findBySessionId(session.getId());
            int present = (int) details.stream()
                .filter(item -> item.getStatus()
                    == vn.edu.fpt.myfschool.common.enums.AttendanceStatus.PRESENT)
                .count();
            session.setTotal(details.size());
            session.setPresent(present);
            session.setAbsent(details.size() - present);
            attendanceSessionRepository.save(session);
        });
    }

    private Parent requireParent(Long parentUserId) {
        return parentRepository.findByUserId(parentUserId)
            .orElseThrow(() -> new ResourceNotFoundException("Parent", "userId", parentUserId));
    }

    private Teacher requireTeacher(Long teacherUserId) {
        return teacherRepository.findByUserId(teacherUserId)
            .orElseThrow(() -> new ResourceNotFoundException("Teacher", "userId", teacherUserId));
    }

    private void assertParentOwnsStudent(Parent parent, Long studentId) {
        if (!studentGuardianRepository.existsByStudentIdAndGuardianId(studentId, parent.getId())) {
            throw new ForbiddenException("Phụ huynh không có quyền thao tác với học sinh này");
        }
    }

    private void validateLeaveShiftConfigured(Enrollment enrollment, LeaveShift leaveShift) {
        Long academicYearId = enrollment.getAcademicYear().getId();
        if (leaveShift == LeaveShift.MORNING
                && !academicYearShiftRepository.existsByAcademicYearIdAndShiftCode(
                    academicYearId, "MORNING")) {
            throw new BadRequestException("Năm học chưa cấu hình buổi sáng");
        }
        if (leaveShift == LeaveShift.AFTERNOON
                && !academicYearShiftRepository.existsByAcademicYearIdAndShiftCode(
                    academicYearId, "AFTERNOON")) {
            throw new BadRequestException("Năm học chưa cấu hình buổi chiều");
        }
        if (leaveShift == LeaveShift.FULL_DAY
                && academicYearShiftRepository.findByAcademicYearId(academicYearId).isEmpty()) {
            throw new BadRequestException("Năm học chưa cấu hình buổi học");
        }
    }

    private List<HomeroomAssignment> getTeacherAssignments(Teacher teacher) {
        List<HomeroomAssignment> assignments = homeroomAssignmentRepository
            .findByTeacherInActiveAcademicYear(teacher.getId());
        if (assignments.isEmpty()) {
            throw new ForbiddenException("Chỉ giáo viên chủ nhiệm mới được truy cập chức năng duyệt đơn xin nghỉ");
        }
        return assignments;
    }

    private List<Long> assignmentClassIds(List<HomeroomAssignment> assignments) {
        return assignments.stream()
            .map(assignment -> assignment.getCls().getId())
            .distinct()
            .toList();
    }

    private boolean isRequestWithinAssignments(
            LeaveRequest request, List<HomeroomAssignment> assignments) {
        return assignments.stream().anyMatch(assignment ->
            assignment.getCls().getId().equals(request.getCls().getId())
                && assignment.getAcademicYear().getId().equals(
                    request.getAcademicYear().getId())
                && !assignment.getEffectiveFrom().isAfter(request.getDateFrom())
                && (assignment.getEffectiveTo() == null
                    || !assignment.getEffectiveTo().isBefore(request.getDateTo())));
    }

    private void assertTeacherCanReviewRequest(Teacher teacher, LeaveRequest request) {
        List<HomeroomAssignment> assignments = homeroomAssignmentRepository
            .findByTeacherIdAndAcademicYearId(
                teacher.getId(), request.getAcademicYear().getId());
        if (!isRequestWithinAssignments(request, assignments)) {
            throw new ForbiddenException(
                "Giáo viên không được xử lý đơn nằm ngoài thời gian chủ nhiệm của mình");
        }
    }

    private SchoolClass assertTeacherCanAccessClass(
            Teacher teacher, Long classId, LocalDate date) {
        SchoolClass cls = classRepository.findById(classId)
            .orElseThrow(() -> new ResourceNotFoundException("Class", "id", classId));
        if (!homeroomAssignmentRepository.existsActiveForTeacherClassAndDate(
                teacher.getId(), classId, cls.getAcademicYear().getId(), date)) {
            throw new ForbiddenException("Chỉ giáo viên chủ nhiệm được duyệt đơn của lớp này");
        }
        return cls;
    }

    private LeaveRequestDto toDto(LeaveRequest lr) {
        return new LeaveRequestDto(
            lr.getId(), lr.getStudent().getId(), lr.getStudent().getUser().getName(),
            lr.getStudent().getStudentCode(), lr.getParent().getId(), lr.getParent().getUser().getName(),
            lr.getCls().getId(), lr.getCls().getName(),
            lr.getAcademicYear().getId(), lr.getAcademicYear().getName(),
            lr.getDateFrom(), lr.getDateTo(), lr.getShift(), lr.getReason(),
            lr.getStatus(), lr.getResponse(),
            lr.getApprovedBy() != null ? lr.getApprovedBy().getId() : null,
            lr.getApprovedBy() != null ? lr.getApprovedBy().getUser().getName() : null,
            lr.getApprovedAt(),
            lr.getAttachments().stream().map(a -> new AttachmentDto(
                a.getId(), a.getFileUrl(), a.getFileName(), Long.valueOf(a.getFileSize()), a.getMimeType()))
                .collect(Collectors.toList()),
            lr.getCreatedAt());
    }

    private record TeacherLeaveScope(
        Long academicYearId,
        LocalDate startDate,
        LocalDate endDate,
        List<HomeroomAssignment> assignments
    ) {}
}
