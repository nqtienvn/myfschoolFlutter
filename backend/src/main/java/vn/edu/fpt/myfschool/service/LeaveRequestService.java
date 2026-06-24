package vn.edu.fpt.myfschool.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.enums.LeaveStatus;
import vn.edu.fpt.myfschool.common.exception.BadRequestException;
import vn.edu.fpt.myfschool.common.exception.ForbiddenException;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.entity.*;
import vn.edu.fpt.myfschool.repository.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class LeaveRequestService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;
    private final TeacherRepository teacherRepository;
    private final ClassRepository classRepository;
    private final ClassSubjectRepository classSubjectRepository;
    private final AttendanceRepository attendanceRepository;
    private final AttachmentRepository attachmentRepository;

    public LeaveRequestDto createLeaveRequest(CreateLeaveRequestRequest request, Long parentUserId) {
        Parent parent = parentRepository.findByUserId(parentUserId)
            .orElseThrow(() -> new ResourceNotFoundException("Parent", "userId", parentUserId));
        Student student = studentRepository.findById(request.studentId())
            .orElseThrow(() -> new ResourceNotFoundException("Student", "id", request.studentId()));

        if (request.dateFrom().isAfter(request.dateTo())) {
            throw new BadRequestException("Ngày bắt đầu phải trước ngày kết thúc");
        }

        long overlapping = leaveRequestRepository.countOverlappingPending(
            request.studentId(), request.dateFrom(), request.dateTo());
        if (overlapping > 0) {
            throw new BadRequestException("Học sinh đã có đơn chờ duyệt cho khoảng ngày này");
        }

        SchoolClass cls = student.getCurrentClass();
        if (cls == null) throw new BadRequestException("Học sinh chưa thuộc lớp nào");

        LeaveRequest lr = new LeaveRequest();
        lr.setStudent(student);
        lr.setParent(parent);
        lr.setCls(cls);
        lr.setDateFrom(request.dateFrom());
        lr.setDateTo(request.dateTo());
        lr.setShift(request.shift());
        lr.setReason(request.reason());
        lr.setStatus(LeaveStatus.PENDING);
        lr = leaveRequestRepository.save(lr);
        return toDto(lr);
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestDto> getParentLeaveRequests(Long parentUserId) {
        Parent parent = parentRepository.findByUserId(parentUserId)
            .orElseThrow(() -> new ResourceNotFoundException("Parent", "userId", parentUserId));
        return leaveRequestRepository.findByParentIdOrderByCreatedAtDesc(parent.getId())
            .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestDto> getStudentLeaveRequests(Long studentUserId) {
        Student student = studentRepository.findByUserId(studentUserId)
            .orElseThrow(() -> new ResourceNotFoundException("Student", "userId", studentUserId));
        return leaveRequestRepository.findByStudentIdOrderByCreatedAtDesc(student.getId())
            .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestDto> getPendingLeaveRequests(Long teacherUserId) {
        Teacher teacher = teacherRepository.findByUserId(teacherUserId)
            .orElseThrow(() -> new ResourceNotFoundException("Teacher", "userId", teacherUserId));

        List<Long> classIds = classSubjectRepository.findByTeacherIdAndAcademicYear(
            teacher.getId(), "2026-2027").stream()
            .map(cs -> cs.getCls().getId()).distinct().collect(Collectors.toList());

        return leaveRequestRepository.findPendingByClass(classIds.isEmpty() ? 0L : classIds.get(0))
            .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestDto> getClassLeaveRequests(Long classId, LeaveStatus status) {
        return leaveRequestRepository.findByClassIdOrderByCreatedAtDesc(classId)
            .stream().filter(lr -> status == null || lr.getStatus() == status)
            .map(this::toDto).collect(Collectors.toList());
    }

    public LeaveRequestDto approveLeaveRequest(Long leaveRequestId, String response, Long teacherUserId) {
        LeaveRequest lr = leaveRequestRepository.findById(leaveRequestId)
            .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", "id", leaveRequestId));
        if (lr.getStatus() != LeaveStatus.PENDING) {
            throw new BadRequestException("Đơn đã được xử lý");
        }

        Teacher teacher = teacherRepository.findByUserId(teacherUserId)
            .orElseThrow(() -> new ResourceNotFoundException("Teacher", "userId", teacherUserId));

        lr.setStatus(LeaveStatus.APPROVED);
        lr.setApprovedBy(teacher);
        lr.setResponse(response);
        lr.setApprovedAt(LocalDateTime.now());
        lr = leaveRequestRepository.save(lr);

        // Auto-update attendance
        autoUpdateAttendance(lr);

        return toDto(lr);
    }

    public LeaveRequestDto rejectLeaveRequest(Long leaveRequestId, String response, Long teacherUserId) {
        LeaveRequest lr = leaveRequestRepository.findById(leaveRequestId)
            .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", "id", leaveRequestId));
        if (lr.getStatus() != LeaveStatus.PENDING) {
            throw new BadRequestException("Đơn đã được xử lý");
        }

        Teacher teacher = teacherRepository.findByUserId(teacherUserId)
            .orElseThrow(() -> new ResourceNotFoundException("Teacher", "userId", teacherUserId));

        lr.setStatus(LeaveStatus.REJECTED);
        lr.setApprovedBy(teacher);
        lr.setResponse(response);
        lr = leaveRequestRepository.save(lr);
        return toDto(lr);
    }

    public void cancelLeaveRequest(Long leaveRequestId, Long parentUserId) {
        LeaveRequest lr = leaveRequestRepository.findById(leaveRequestId)
            .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", "id", leaveRequestId));
        if (lr.getStatus() != LeaveStatus.PENDING) {
            throw new BadRequestException("Chỉ có thể hủy đơn đang chờ duyệt");
        }
        leaveRequestRepository.delete(lr);
    }

    @Transactional(readOnly = true)
    public long getPendingCount(Long teacherUserId) {
        Teacher teacher = teacherRepository.findByUserId(teacherUserId)
            .orElseThrow(() -> new ResourceNotFoundException("Teacher", "userId", teacherUserId));
        List<Long> classIds = classSubjectRepository.findByTeacherIdAndAcademicYear(
            teacher.getId(), "2026-2027").stream()
            .map(cs -> cs.getCls().getId()).distinct().collect(Collectors.toList());
        return classIds.isEmpty() ? 0 : leaveRequestRepository.countPendingByClassIds(classIds);
    }

    private void autoUpdateAttendance(LeaveRequest lr) {
        java.time.LocalDate current = lr.getDateFrom();
        while (!current.isAfter(lr.getDateTo())) {
            if (lr.getShift() == vn.edu.fpt.myfschool.common.enums.LeaveShift.FULL_DAY ||
                lr.getShift() == vn.edu.fpt.myfschool.common.enums.LeaveShift.MORNING) {
                upsertAttendance(lr, current, vn.edu.fpt.myfschool.common.enums.Shift.MORNING);
            }
            if (lr.getShift() == vn.edu.fpt.myfschool.common.enums.LeaveShift.FULL_DAY ||
                lr.getShift() == vn.edu.fpt.myfschool.common.enums.LeaveShift.AFTERNOON) {
                upsertAttendance(lr, current, vn.edu.fpt.myfschool.common.enums.Shift.AFTERNOON);
            }
            current = current.plusDays(1);
        }
    }

    private void upsertAttendance(LeaveRequest lr, java.time.LocalDate date, vn.edu.fpt.myfschool.common.enums.Shift shift) {
        Attendance att = attendanceRepository.findByStudentIdAndDateAndShift(lr.getStudent().getId(), date, shift)
            .orElseGet(() -> {
                Attendance a = new Attendance();
                a.setStudent(lr.getStudent());
                a.setCls(lr.getCls());
                a.setTeacher(lr.getApprovedBy());
                a.setDate(date);
                a.setShift(shift);
                return a;
            });
        att.setStatus(vn.edu.fpt.myfschool.common.enums.AttendanceStatus.ABSENT_WITH_LEAVE);
        att.setLeaveRequest(lr);
        attendanceRepository.save(att);
    }

    private LeaveRequestDto toDto(LeaveRequest lr) {
        return new LeaveRequestDto(
            lr.getId(), lr.getStudent().getId(), lr.getStudent().getUser().getName(),
            lr.getStudent().getStudentCode(), lr.getParent().getId(), lr.getParent().getUser().getName(),
            lr.getCls().getId(), lr.getCls().getName(),
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
