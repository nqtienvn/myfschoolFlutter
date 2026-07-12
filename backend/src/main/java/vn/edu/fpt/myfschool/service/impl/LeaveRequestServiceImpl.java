package vn.edu.fpt.myfschool.service.impl;

import vn.edu.fpt.myfschool.entity.Attendance;
import vn.edu.fpt.myfschool.entity.LeaveRequest;
import vn.edu.fpt.myfschool.entity.Parent;
import vn.edu.fpt.myfschool.entity.SchoolClass;
import vn.edu.fpt.myfschool.entity.Student;
import vn.edu.fpt.myfschool.entity.Teacher;
import vn.edu.fpt.myfschool.entity.Enrollment;
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
    private final ClassRepository classRepository;
    private final HomeroomAssignmentRepository homeroomAssignmentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AcademicYearShiftRepository academicYearShiftRepository;
    private final NotificationService notificationService;

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

        // Notify Homeroom Teacher
        List<HomeroomAssignment> assignments = homeroomAssignmentRepository
            .findByClsIdAndAcademicYearId(cls.getId(), enrollment.getAcademicYear().getId())
            .stream()
            .filter(assignment -> !assignment.getEffectiveFrom().isAfter(request.dateFrom())
                && (assignment.getEffectiveTo() == null
                    || !assignment.getEffectiveTo().isBefore(request.dateFrom())))
            .toList();
        if (!assignments.isEmpty()) {
            User teacherUser = assignments.get(0).getTeacher().getUser();
            notificationService.createNotification(
                teacherUser.getId(),
                "Đơn xin nghỉ học mới",
                "Học sinh " + student.getUser().getName() + " có đơn xin nghỉ từ ngày " + request.dateFrom() + " đến " + request.dateTo(),
                "Đơn xin nghỉ", lr.getId(), "LEAVE_REQUEST"
            );
        }

        return toDto(lr);
    }

    @Transactional(readOnly = true)
    @Override
    public List<LeaveRequestDto> getParentLeaveRequests(Long parentUserId, Long studentId) {
        Parent parent = requireParent(parentUserId);
        if (studentId != null) {
            assertParentOwnsStudent(parent, studentId);
        }
        return leaveRequestRepository.findByParentIdOrderByCreatedAtDesc(parent.getId())
            .stream()
            .filter(request -> studentId == null || request.getStudent().getId().equals(studentId))
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
    public List<LeaveRequestDto> getPendingLeaveRequests(Long teacherUserId) {
        Teacher teacher = requireTeacher(teacherUserId);
        List<Long> classIds = getTeacherClassIds(teacher, LocalDate.now());
        if (classIds.isEmpty()) return List.of();

        return leaveRequestRepository.findByClassIdsAndStatusOrderByCreatedAtDesc(classIds, LeaveStatus.PENDING)
            .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Override
    public List<LeaveRequestDto> getClassLeaveRequests(Long classId, LeaveStatus status, Long teacherUserId) {
        Teacher teacher = requireTeacher(teacherUserId);
        assertTeacherCanAccessClass(teacher, classId, LocalDate.now());

        List<LeaveRequest> requests = status != null
            ? leaveRequestRepository.findByClassIdAndStatusOrderByCreatedAtDesc(classId, status)
            : leaveRequestRepository.findByClassIdOrderByCreatedAtDesc(classId);
        return requests.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public LeaveRequestDto approveLeaveRequest(Long leaveRequestId, String response, Long teacherUserId) {
        LeaveRequest lr = leaveRequestRepository.findById(leaveRequestId)
            .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", "id", leaveRequestId));
        if (lr.getStatus() != LeaveStatus.PENDING) {
            throw new BadRequestException("Đơn đã được xử lý");
        }

        Teacher teacher = requireTeacher(teacherUserId);
        assertTeacherCanAccessClass(teacher, lr.getCls().getId(), reviewDate(lr));

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
        assertTeacherCanAccessClass(teacher, lr.getCls().getId(), reviewDate(lr));

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
    public long getPendingCount(Long teacherUserId) {
        Teacher teacher = requireTeacher(teacherUserId);
        List<Long> classIds = getTeacherClassIds(teacher, LocalDate.now());
        return classIds.isEmpty() ? 0 : leaveRequestRepository.countByClassIdsAndStatus(classIds, LeaveStatus.PENDING);
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
            .filter(attendance -> attendance.getStatus()
                == vn.edu.fpt.myfschool.common.enums.AttendanceStatus.ABSENT_WITHOUT_LEAVE)
            .ifPresent(attendance -> {
                attendance.setStatus(vn.edu.fpt.myfschool.common.enums.AttendanceStatus.ABSENT_WITH_LEAVE);
                attendance.setLeaveRequest(lr);
                attendanceRepository.save(attendance);
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

    private LocalDate reviewDate(LeaveRequest request) {
        LocalDate today = LocalDate.now();
        if (!today.isBefore(request.getAcademicYear().getStartDate())
                && !today.isAfter(request.getAcademicYear().getEndDate())) {
            return today;
        }
        return request.getDateFrom();
    }

    private List<Long> getTeacherClassIds(Teacher teacher, LocalDate date) {
        return homeroomAssignmentRepository.findActiveByTeacherAndDate(teacher.getId(), date)
            .stream().map(assignment -> assignment.getCls().getId()).distinct().toList();
    }

    private void assertTeacherCanAccessClass(Teacher teacher, Long classId, LocalDate date) {
        SchoolClass cls = classRepository.findById(classId)
            .orElseThrow(() -> new ResourceNotFoundException("Class", "id", classId));
        if (!homeroomAssignmentRepository.existsActiveForTeacherClassAndDate(
                teacher.getId(), classId, cls.getAcademicYear().getId(), date)) {
            throw new ForbiddenException("Chỉ giáo viên chủ nhiệm được duyệt đơn của lớp này");
        }
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
}
