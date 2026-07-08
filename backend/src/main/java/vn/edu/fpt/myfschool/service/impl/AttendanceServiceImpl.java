package vn.edu.fpt.myfschool.service.impl;

import vn.edu.fpt.myfschool.controller.entity.Attendance;
import vn.edu.fpt.myfschool.controller.entity.SchoolClass;
import vn.edu.fpt.myfschool.controller.entity.Semester;
import vn.edu.fpt.myfschool.controller.entity.Student;
import vn.edu.fpt.myfschool.controller.entity.Teacher;
import vn.edu.fpt.myfschool.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.enums.AttendanceStatus;
import vn.edu.fpt.myfschool.common.enums.Shift;
import vn.edu.fpt.myfschool.common.exception.BadRequestException;
import vn.edu.fpt.myfschool.common.exception.ForbiddenException;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.repository.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service("attendanceService")
@RequiredArgsConstructor
@Transactional
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final StudentRepository studentRepository;
    private final ClassRepository classRepository;
    private final TeacherRepository teacherRepository;
    private final ClassSubjectRepository classSubjectRepository;
    private final SemesterRepository semesterRepository;
    private final SemesterResultService semesterResultService;

    @Transactional(readOnly = true)
    @Override
    public DailyAttendanceDto getDailyAttendance(Long classId, LocalDate date, Shift shift, Long teacherUserId) {
        Teacher teacher = requireTeacher(teacherUserId);
        assertTeacherCanAccessClass(teacher, classId);

        SchoolClass cls = classRepository.findById(classId)
            .orElseThrow(() -> new ResourceNotFoundException("Class", "id", classId));

        List<Student> students = studentRepository.findByCurrentClassId(classId);
        List<Attendance> existing = attendanceRepository.findByClsIdAndDateAndShift(classId, date, shift);

        List<AttendanceEntryDto> entries = students.stream().map(s -> {
            Attendance att = existing.stream().filter(a -> a.getStudent().getId().equals(s.getId())).findFirst().orElse(null);
            return new AttendanceEntryDto(s.getId(), s.getUser().getName(), s.getStudentCode(),
                att != null ? att.getStatus() : null, att != null ? att.getId() : null);
        }).collect(Collectors.toList());

        return new DailyAttendanceDto(classId, cls.getName(), date, shift, entries);
    }

    @Override
    public List<AttendanceDto> submitAttendance(SubmitAttendanceRequest request, Long teacherUserId) {
        Teacher teacher = requireTeacher(teacherUserId);
        SchoolClass cls = classRepository.findById(request.classId())
            .orElseThrow(() -> new ResourceNotFoundException("Class", "id", request.classId()));
        assertTeacherCanAccessClass(teacher, request.classId());

        Map<Long, Student> studentsById = new HashMap<>();
        for (AttendanceEntry entry : request.entries()) {
            Student student = studentRepository.findById(entry.studentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", entry.studentId()));
            assertStudentBelongsToClass(student, request.classId());
            studentsById.put(entry.studentId(), student);
        }

        List<AttendanceDto> results = new ArrayList<>();
        for (AttendanceEntry entry : request.entries()) {
            Student student = studentsById.get(entry.studentId());
            Attendance att = attendanceRepository.findByStudentIdAndDateAndShift(
                entry.studentId(), request.date(), request.shift())
                .orElseGet(() -> {
                    Attendance a = new Attendance();
                    a.setStudent(student);
                    a.setDate(request.date());
                    a.setShift(request.shift());
                    return a;
                });

            att.setCls(cls);
            att.setTeacher(teacher);
            att.setStatus(entry.status());
            att = attendanceRepository.save(att);
            results.add(toDto(att));
        }
        return results;
    }

    @Override
    public AttendanceDto updateAttendance(Long attendanceId, AttendanceStatus newStatus, Long teacherUserId) {
        Teacher teacher = requireTeacher(teacherUserId);
        Attendance att = attendanceRepository.findById(attendanceId)
            .orElseThrow(() -> new ResourceNotFoundException("Attendance", "id", attendanceId));
        assertTeacherCanAccessClass(teacher, att.getCls().getId());
        att.setStatus(newStatus);
        att.setTeacher(teacher);
        return toDto(attendanceRepository.save(att));
    }

    @Transactional(readOnly = true)
    @Override
    public AttendanceLogDto getStudentAttendanceLog(Long studentId, Long semesterId) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("Student", "id", studentId));
        Semester semester = semesterRepository.findById(semesterId)
            .orElseThrow(() -> new ResourceNotFoundException("Semester", "id", semesterId));

        List<Attendance> records = attendanceRepository.findByStudentIdOrderByDateDesc(studentId)
            .stream().filter(a -> !a.getDate().isBefore(semester.getStartDate())
                && !a.getDate().isAfter(semester.getEndDate()))
            .collect(Collectors.toList());

        List<AttendanceDto> dtos = records.stream().map(this::toDto).collect(Collectors.toList());

        int total = records.size();
        int present = (int) records.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT).count();
        int late = (int) records.stream().filter(a -> a.getStatus() == AttendanceStatus.LATE).count();
        int absWith = (int) records.stream().filter(a -> a.getStatus() == AttendanceStatus.ABSENT_WITH_LEAVE).count();
        int absWithout = (int) records.stream().filter(a -> a.getStatus() == AttendanceStatus.ABSENT_WITHOUT_LEAVE).count();
        double rate = total > 0 ? (double)(present + late) / total * 100 : 0;

        AttendanceStatsDto stats = new AttendanceStatsDto(studentId, student.getUser().getName(),
            semesterId, semester.getName(), total, present, late, absWith, absWithout,
            BigDecimal.valueOf(rate).setScale(1, RoundingMode.HALF_UP).doubleValue());

        return new AttendanceLogDto(dtos, stats);
    }

    @Override
    public void autoUpdateForApprovedLeave(Long leaveRequestId) {
        // Called by LeaveRequestService when approved
        // Implementation depends on LeaveRequest entity
    }

    private Teacher requireTeacher(Long teacherUserId) {
        return teacherRepository.findByUserId(teacherUserId)
            .orElseThrow(() -> new ResourceNotFoundException("Teacher", "userId", teacherUserId));
    }

    private void assertTeacherCanAccessClass(Teacher teacher, Long classId) {
        if (!classSubjectRepository.existsByTeacherIdAndClassId(teacher.getId(), classId)) {
            throw new ForbiddenException("Giáo viên không có quyền thao tác với lớp này");
        }
    }

    private void assertStudentBelongsToClass(Student student, Long classId) {
        if (student.getCurrentClass() == null || !student.getCurrentClass().getId().equals(classId)) {
            throw new BadRequestException("Học sinh không thuộc lớp được điểm danh");
        }
    }

    private AttendanceDto toDto(Attendance a) {
        return new AttendanceDto(a.getId(), a.getStudent().getId(), a.getStudent().getUser().getName(),
            a.getStudent().getStudentCode(), a.getCls().getId(), a.getCls().getName(),
            a.getDate(), a.getShift(), a.getStatus(),
            a.getLeaveRequest() != null ? a.getLeaveRequest().getId() : null,
            a.getTeacher() != null ? a.getTeacher().getUser().getName() : null, a.getCreatedAt());
    }
}
