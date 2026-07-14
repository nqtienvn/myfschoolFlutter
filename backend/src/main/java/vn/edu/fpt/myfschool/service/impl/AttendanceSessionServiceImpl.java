package vn.edu.fpt.myfschool.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.enums.AttendanceStatus;
import vn.edu.fpt.myfschool.common.enums.Shift;
import vn.edu.fpt.myfschool.common.enums.UserRole;
import vn.edu.fpt.myfschool.common.exception.BadRequestException;
import vn.edu.fpt.myfschool.common.exception.ConflictException;
import vn.edu.fpt.myfschool.common.exception.ForbiddenException;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.entity.AttendanceDetail;
import vn.edu.fpt.myfschool.entity.AttendanceSession;
import vn.edu.fpt.myfschool.entity.SchoolClass;
import vn.edu.fpt.myfschool.entity.Student;
import vn.edu.fpt.myfschool.entity.Teacher;
import vn.edu.fpt.myfschool.entity.Schedule;
import vn.edu.fpt.myfschool.repository.AttendanceDetailRepository;
import vn.edu.fpt.myfschool.repository.AttendanceRepository;
import vn.edu.fpt.myfschool.repository.AttendanceSessionRepository;
import vn.edu.fpt.myfschool.repository.ClassRepository;
import vn.edu.fpt.myfschool.repository.EnrollmentRepository;
import vn.edu.fpt.myfschool.repository.TeacherRepository;
import vn.edu.fpt.myfschool.repository.ScheduleRepository;
import vn.edu.fpt.myfschool.repository.TimetableRepository;
import vn.edu.fpt.myfschool.service.AttendanceService;
import vn.edu.fpt.myfschool.service.AttendanceSessionService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.HashSet;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service("attendanceSessionService")
@RequiredArgsConstructor
@Transactional
public class AttendanceSessionServiceImpl implements AttendanceSessionService {

    private final AttendanceSessionRepository sessionRepository;
    private final AttendanceDetailRepository detailRepository;
    private final AttendanceRepository attendanceRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ClassRepository classRepository;
    private final TeacherRepository teacherRepository;
    private final ScheduleRepository scheduleRepository;
    private final TimetableRepository timetableRepository;
    private final AttendanceService attendanceService;

    @Override
    public AttendanceSessionDto createSession(CreateAttendanceSessionRequest request, Long userId) {
        Optional<AttendanceSession> existing = sessionRepository
            .findTopByClsIdAndDateAndShiftOrderByCreatedAtDesc(
                request.classId(), request.date(), request.shift());
        if (existing.isPresent()) {
            throw new ConflictException("Buoi diem danh da ton tai");
        }

        SchoolClass cls = classRepository.findById(request.classId())
            .orElseThrow(() -> new ResourceNotFoundException("Class", "id", request.classId()));
        Teacher teacher = requireAuthenticatedTeacher(userId);
        if (!teacher.getId().equals(request.teacherId())) {
            throw new BadRequestException("Giáo viên trong yêu cầu không khớp tài khoản đăng nhập");
        }
        Schedule schedule = resolveSchedule(request, cls);

        List<Student> students = enrollmentRepository.findActiveStudentsByClassAndYear(
            request.classId(), cls.getAcademicYear().getId());
        Map<Long, AttendanceStatus> existingStatuses = attendanceRepository
            .findByClsIdAndDateAndShift(request.classId(), request.date(), request.shift())
            .stream()
            .collect(Collectors.toMap(
                attendance -> attendance.getStudent().getId(),
                attendance -> attendance.getStatus()));
        SubmitAttendanceRequest canonicalRequest = new SubmitAttendanceRequest(
            request.classId(), request.date(), request.shift(), students.stream()
                .map(student -> new AttendanceEntry(
                    student.getId(),
                    existingStatuses.getOrDefault(
                        student.getId(), AttendanceStatus.PRESENT)))
                .toList());
        Map<Long, AttendanceDto> canonical = attendanceService
            .synchronizeSessionAttendance(canonicalRequest, userId).stream()
            .collect(Collectors.toMap(AttendanceDto::studentId, Function.identity()));

        AttendanceSession session = new AttendanceSession();
        session.setCls(cls);
        session.setTeacher(teacher);
        session.setDate(request.date());
        session.setShift(request.shift());
        session.setSchedule(schedule);
        try {
            session = sessionRepository.saveAndFlush(session);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Buổi điểm danh đã tồn tại");
        }

        List<AttendanceDetail> details = new ArrayList<>();
        for (Student s : students) {
            AttendanceDetail d = new AttendanceDetail();
            d.setSession(session);
            d.setStudent(s);
            AttendanceDto attendance = canonical.get(s.getId());
            d.setStatus(attendance != null ? attendance.status() : AttendanceStatus.PRESENT);
            details.add(d);
        }
        detailRepository.saveAll(details);

        session.setTotal(students.size());
        session.setPresent((int) details.stream()
            .filter(detail -> detail.getStatus() == AttendanceStatus.PRESENT).count());
        session.setAbsent(session.getTotal() - session.getPresent());
        sessionRepository.save(session);

        return toDto(session);
    }

    @Override
    public List<AttendanceDetailDto> updateDetails(UpdateAttendanceDetailRequest request, Long userId) {
        AttendanceSession session = sessionRepository.findById(request.sessionId())
            .orElseThrow(() -> new ResourceNotFoundException("AttendanceSession", "id", request.sessionId()));
        requireSessionOwner(session, userId);
        if (session.getIsClosed()) {
            throw new BadRequestException("Buoi diem danh da ket thuc");
        }

        HashSet<Long> submittedIds = new HashSet<>();
        for (UpdateAttendanceDetailEntry entry : request.entries()) {
            if (entry.studentId() == null || !submittedIds.add(entry.studentId())) {
                throw new BadRequestException("Danh sách cập nhật có học sinh trùng hoặc thiếu mã");
            }
            AttendanceDetail detail = detailRepository.findBySessionIdAndStudentId(
                session.getId(), entry.studentId())
                .orElseThrow(() -> new ResourceNotFoundException("AttendanceDetail", "studentId", entry.studentId()));
            try {
                detail.setStatus(AttendanceStatus.valueOf(entry.status()));
            } catch (IllegalArgumentException | NullPointerException exception) {
                throw new BadRequestException("Trạng thái điểm danh không hợp lệ");
            }
            detail.setNote(entry.note());
        }

        List<AttendanceDetail> allDetails = detailRepository.findBySessionId(session.getId());
        SubmitAttendanceRequest canonicalRequest = new SubmitAttendanceRequest(
            session.getCls().getId(), session.getDate(), session.getShift(), allDetails.stream()
                .map(detail -> new AttendanceEntry(detail.getStudent().getId(), detail.getStatus()))
                .toList());
        Map<Long, AttendanceDto> canonical = attendanceService
            .synchronizeSessionAttendance(canonicalRequest, userId).stream()
            .collect(Collectors.toMap(AttendanceDto::studentId, Function.identity()));
        allDetails.forEach(detail -> {
            AttendanceDto attendance = canonical.get(detail.getStudent().getId());
            if (attendance != null) detail.setStatus(attendance.status());
        });
        detailRepository.saveAll(allDetails);

        recalculateCounts(session);
        return allDetails.stream()
            .filter(detail -> submittedIds.contains(detail.getStudent().getId()))
            .map(this::toDetailDto)
            .toList();
    }

    @Override
    public AttendanceSessionDto closeSession(Long sessionId, Long userId) {
        AttendanceSession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("AttendanceSession", "id", sessionId));
        requireSessionOwner(session, userId);
        session.setIsClosed(true);
        sessionRepository.save(session);
        return toDto(session);
    }

    @Override
    public List<AttendanceSessionDto> findByClassDateShift(
            Long classId, LocalDate date, Shift shift, Long userId, UserRole userRole) {
        if (userRole == UserRole.TEACHER) {
            requireAuthenticatedTeacher(userId);
            attendanceService.getDailyAttendance(classId, date, shift, userId);
        } else if (userRole != UserRole.ADMIN) {
            throw new ForbiddenException("Không có quyền xem buổi điểm danh");
        }
        return sessionRepository.findByClsIdAndDateAndShift(classId, date, shift)
            .stream().map(this::toDto).toList();
    }

    private Teacher requireAuthenticatedTeacher(Long userId) {
        return teacherRepository.findByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Teacher", "userId", userId));
    }

    private void requireSessionOwner(AttendanceSession session, Long userId) {
        Teacher teacher = requireAuthenticatedTeacher(userId);
        if (!session.getTeacher().getId().equals(teacher.getId())) {
            throw new ForbiddenException(
                "Chỉ giáo viên tạo buổi điểm danh mới được cập nhật");
        }
        attendanceService.getDailyAttendance(
            session.getCls().getId(), session.getDate(), session.getShift(), userId);
    }

    private Schedule resolveSchedule(CreateAttendanceSessionRequest request, SchoolClass cls) {
        if (request.scheduleId() == null) return null;
        Schedule schedule = scheduleRepository.findById(request.scheduleId())
            .orElseThrow(() -> new ResourceNotFoundException("Schedule", "id", request.scheduleId()));
        int dayOfWeek = request.date().getDayOfWeek().getValue() % 7 + 1;
        Long effectiveTimetableId = timetableRepository.findEffective(
                cls.getId(), schedule.getTimetable().getSemester().getId(), request.date())
            .stream().findFirst().map(item -> item.getId()).orElse(null);
        if (!schedule.getTimetable().getCls().getId().equals(cls.getId())
                || schedule.getShift() != request.shift()
                || schedule.getDayOfWeek() != dayOfWeek
                || !schedule.getTimetable().getId().equals(effectiveTimetableId)
                || request.date().isBefore(schedule.getTimetable().getSemester().getStartDate())
                || request.date().isAfter(schedule.getTimetable().getSemester().getEndDate())) {
            throw new BadRequestException("Tiết học không thuộc lớp, ngày và buổi đã chọn");
        }
        return schedule;
    }

    private void recalculateCounts(AttendanceSession session) {
        List<AttendanceDetail> details = detailRepository.findBySessionId(session.getId());
        int present = 0, absent = 0;
        for (AttendanceDetail d : details) {
            switch (d.getStatus()) {
                case PRESENT -> present++;
                default -> absent++;
            }
        }
        session.setPresent(present);
        session.setAbsent(absent);
        sessionRepository.save(session);
    }

    private AttendanceSessionDto toDto(AttendanceSession s) {
        List<AttendanceDetail> details = detailRepository.findBySessionId(s.getId());
        List<AttendanceDetailDto> detailDtos = details.stream().map(this::toDetailDto).toList();
        return new AttendanceSessionDto(
            s.getId(),
            s.getCls().getId(), s.getCls().getName(),
            s.getTeacher().getId(), s.getTeacher().getUser().getName(),
            s.getDate(), s.getShift(),
            s.getSchedule() != null ? s.getSchedule().getId() : null,
            s.getTotal(), s.getPresent(), s.getAbsent(),
            s.getIsClosed(), detailDtos);
    }

    private AttendanceDetailDto toDetailDto(AttendanceDetail d) {
        return new AttendanceDetailDto(
            d.getId(), d.getSession().getId(),
            d.getStudent().getId(), d.getStudent().getUser().getName(), d.getStudent().getStudentCode(),
            d.getStatus().name(), d.getNote());
    }
}
