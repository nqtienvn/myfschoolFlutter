package vn.edu.fpt.myfschool.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.enums.AttendanceStatus;
import vn.edu.fpt.myfschool.common.enums.Shift;
import vn.edu.fpt.myfschool.common.exception.BadRequestException;
import vn.edu.fpt.myfschool.common.exception.ConflictException;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.entity.AttendanceDetail;
import vn.edu.fpt.myfschool.entity.AttendanceSession;
import vn.edu.fpt.myfschool.entity.SchoolClass;
import vn.edu.fpt.myfschool.entity.Student;
import vn.edu.fpt.myfschool.entity.Teacher;
import vn.edu.fpt.myfschool.repository.AttendanceDetailRepository;
import vn.edu.fpt.myfschool.repository.AttendanceSessionRepository;
import vn.edu.fpt.myfschool.repository.ClassRepository;
import vn.edu.fpt.myfschool.repository.EnrollmentRepository;
import vn.edu.fpt.myfschool.repository.TeacherRepository;
import vn.edu.fpt.myfschool.service.AttendanceSessionService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service("attendanceSessionService")
@RequiredArgsConstructor
@Transactional
public class AttendanceSessionServiceImpl implements AttendanceSessionService {

    private final AttendanceSessionRepository sessionRepository;
    private final AttendanceDetailRepository detailRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ClassRepository classRepository;
    private final TeacherRepository teacherRepository;

    @Override
    public AttendanceSessionDto createSession(CreateAttendanceSessionRequest request, Long userId) {
        Optional<AttendanceSession> existing = request.scheduleId() != null
            ? sessionRepository.findByClsIdAndDateAndShiftAndScheduleId(
                request.classId(), request.date(), request.shift(), request.scheduleId())
            : sessionRepository.findTopByClsIdAndDateAndShiftOrderByCreatedAtDesc(
                request.classId(), request.date(), request.shift());
        if (existing.isPresent() && !existing.get().getIsClosed()) {
            throw new ConflictException("Buoi diem danh da ton tai");
        }

        SchoolClass cls = classRepository.findById(request.classId())
            .orElseThrow(() -> new ResourceNotFoundException("Class", "id", request.classId()));
        Teacher teacher = teacherRepository.findById(request.teacherId())
            .orElseThrow(() -> new ResourceNotFoundException("Teacher", "id", request.teacherId()));

        AttendanceSession session = new AttendanceSession();
        session.setCls(cls);
        session.setTeacher(teacher);
        session.setDate(request.date());
        session.setShift(request.shift());
        session = sessionRepository.save(session);

        List<Student> students = enrollmentRepository.findActiveStudentsByClassAndYear(
            request.classId(), cls.getAcademicYear().getId());

        List<AttendanceDetail> details = new ArrayList<>();
        for (Student s : students) {
            AttendanceDetail d = new AttendanceDetail();
            d.setSession(session);
            d.setStudent(s);
            d.setStatus(AttendanceStatus.PRESENT);
            details.add(d);
        }
        detailRepository.saveAll(details);

        session.setTotal(students.size());
        session.setPresent(students.size());
        session.setLate(0);
        session.setAbsent(0);
        sessionRepository.save(session);

        return toDto(session);
    }

    @Override
    public List<AttendanceDetailDto> updateDetails(UpdateAttendanceDetailRequest request, Long userId) {
        AttendanceSession session = sessionRepository.findById(request.sessionId())
            .orElseThrow(() -> new ResourceNotFoundException("AttendanceSession", "id", request.sessionId()));
        if (session.getIsClosed()) {
            throw new BadRequestException("Buoi diem danh da ket thuc");
        }

        List<AttendanceDetailDto> results = new ArrayList<>();
        for (UpdateAttendanceDetailEntry entry : request.entries()) {
            AttendanceDetail detail = detailRepository.findBySessionIdAndStudentId(
                session.getId(), entry.studentId())
                .orElseThrow(() -> new ResourceNotFoundException("AttendanceDetail", "studentId", entry.studentId()));
            detail.setStatus(AttendanceStatus.valueOf(entry.status()));
            detail.setNote(entry.note());
            detailRepository.save(detail);
            results.add(toDetailDto(detail));
        }

        recalculateCounts(session);
        return results;
    }

    @Override
    public AttendanceSessionDto closeSession(Long sessionId, Long userId) {
        AttendanceSession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("AttendanceSession", "id", sessionId));
        session.setIsClosed(true);
        sessionRepository.save(session);
        return toDto(session);
    }

    @Override
    public List<AttendanceSessionDto> findByClassDateShift(Long classId, LocalDate date, Shift shift) {
        return sessionRepository.findByClsIdAndDateAndShift(classId, date, shift)
            .stream().map(this::toDto).toList();
    }

    private void recalculateCounts(AttendanceSession session) {
        List<AttendanceDetail> details = detailRepository.findBySessionId(session.getId());
        int present = 0, late = 0, absent = 0;
        for (AttendanceDetail d : details) {
            switch (d.getStatus()) {
                case PRESENT -> present++;
                case LATE -> late++;
                default -> absent++;
            }
        }
        session.setPresent(present);
        session.setLate(late);
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
            s.getTotal(), s.getPresent(), s.getLate(), s.getAbsent(),
            s.getIsClosed(), detailDtos);
    }

    private AttendanceDetailDto toDetailDto(AttendanceDetail d) {
        return new AttendanceDetailDto(
            d.getId(), d.getSession().getId(),
            d.getStudent().getId(), d.getStudent().getUser().getName(), d.getStudent().getStudentCode(),
            d.getStatus().name(), d.getNote());
    }
}