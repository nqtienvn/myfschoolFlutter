package vn.edu.fpt.myfschool.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import vn.edu.fpt.myfschool.entity.Attendance;
import vn.edu.fpt.myfschool.entity.AttendanceDetail;
import vn.edu.fpt.myfschool.entity.AttendanceSession;
import vn.edu.fpt.myfschool.entity.SchoolClass;
import vn.edu.fpt.myfschool.entity.Semester;
import vn.edu.fpt.myfschool.entity.Student;
import vn.edu.fpt.myfschool.entity.Teacher;
import vn.edu.fpt.myfschool.entity.Parent;
import vn.edu.fpt.myfschool.entity.HomeroomAssignment;
import vn.edu.fpt.myfschool.entity.LeaveRequest;
import vn.edu.fpt.myfschool.entity.Schedule;
import vn.edu.fpt.myfschool.entity.Timetable;
import vn.edu.fpt.myfschool.entity.AttendanceCorrectionRequest;
import vn.edu.fpt.myfschool.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.enums.AttendanceStatus;
import vn.edu.fpt.myfschool.common.enums.AcademicYearStatus;
import vn.edu.fpt.myfschool.common.enums.LeaveShift;
import vn.edu.fpt.myfschool.common.enums.Shift;
import vn.edu.fpt.myfschool.common.enums.AttendanceCorrectionStatus;
import vn.edu.fpt.myfschool.common.exception.BadRequestException;
import vn.edu.fpt.myfschool.common.exception.ForbiddenException;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.repository.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service("attendanceService")
@RequiredArgsConstructor
@Transactional
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final AttendanceSessionRepository attendanceSessionRepository;
    private final AttendanceDetailRepository attendanceDetailRepository;
    private final StudentRepository studentRepository;
    private final ClassRepository classRepository;
    private final TeacherRepository teacherRepository;
    private final SemesterRepository semesterRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final HomeroomAssignmentRepository homeroomAssignmentRepository;
    private final AcademicYearShiftRepository academicYearShiftRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final ParentRepository parentRepository;
    private final StudentGuardianRepository studentGuardianRepository;
    private final ScheduleRepository scheduleRepository;
    private final TimetableRepository timetableRepository;
    private final AttendanceCorrectionRequestRepository correctionRequestRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.attendance.enforce-current-day:true}")
    private boolean enforceCurrentDay;

    @Transactional(readOnly = true)
    @Override
    public HomeroomAttendanceContextDto getHomeroomContext(LocalDate date, Long teacherUserId) {
        Teacher teacher = requireTeacher(teacherUserId);
        List<HomeroomAssignment> assignments =
            homeroomAssignmentRepository.findActiveByTeacherAndDate(teacher.getId(), date);
        if (assignments.isEmpty()) {
            throw new ResourceNotFoundException("HomeroomAssignment", "teacherId", teacher.getId());
        }
        if (assignments.size() > 1) {
            throw new BadRequestException("Giáo viên có nhiều hơn một lớp chủ nhiệm trong cùng ngày");
        }
        HomeroomAssignment assignment = assignments.get(0);
        SchoolClass cls = assignment.getCls();
        List<Shift> shifts = configuredShifts(cls).stream()
            .filter(shift -> !scheduledSlots(cls, date, shift).isEmpty())
            .toList();
        return new HomeroomAttendanceContextDto(
            cls.getId(), cls.getName(), cls.getAcademicYear().getId(),
            cls.getAcademicYear().getName(), shifts);
    }

    @Transactional(readOnly = true)
    @Override
    public DailyAttendanceDto getDailyAttendance(Long classId, LocalDate date, Shift shift, Long teacherUserId) {
        Teacher teacher = requireTeacher(teacherUserId);
        SchoolClass cls = classRepository.findById(classId)
            .orElseThrow(() -> new ResourceNotFoundException("Class", "id", classId));
        validateAttendanceScope(teacher, cls, date, shift);
        List<Schedule> slots = scheduledSlots(cls, date, shift);

        List<Student> students = enrollmentRepository.findActiveStudentsByClassAndYear(classId, cls.getAcademicYear().getId());
        List<Attendance> existing = attendanceRepository.findByClsIdAndDateAndShift(classId, date, shift);
        Map<Long, LeaveRequest> approvedLeaves = approvedLeavesByStudent(classId, date, shift);

        List<AttendanceEntryDto> entries = students.stream().map(s -> {
            Attendance att = existing.stream().filter(a -> a.getStudent().getId().equals(s.getId())).findFirst().orElse(null);
            return new AttendanceEntryDto(s.getId(), s.getUser().getName(), s.getStudentCode(),
                att != null ? att.getStatus() : null, att != null ? att.getId() : null,
                approvedLeaves.containsKey(s.getId()));
        }).collect(Collectors.toList());

        boolean submitted = !students.isEmpty() && existing.size() == students.size();
        boolean correctionPending = correctionRequestRepository.existsByClsIdAndDateAndShiftAndStatus(
            classId, date, shift, AttendanceCorrectionStatus.PENDING);
        return new DailyAttendanceDto(classId, cls.getName(), date, shift,
            !slots.isEmpty(), submitted, date.equals(LocalDate.now()), correctionPending,
            slots.size(), entries);
    }

    @Override
    public List<AttendanceDto> submitAttendance(SubmitAttendanceRequest request, Long teacherUserId) {
        Teacher teacher = requireTeacher(teacherUserId);
        SchoolClass cls = classRepository.findById(request.classId())
            .orElseThrow(() -> new ResourceNotFoundException("Class", "id", request.classId()));
        validateAttendanceScope(teacher, cls, request.date(), request.shift());
        requireTeacherCanEditToday(request.date());
        List<Schedule> slots = requireScheduledSlots(cls, request.date(), request.shift());
        if (!attendanceRepository.findByClsIdAndDateAndShift(
                cls.getId(), request.date(), request.shift()).isEmpty()) {
            throw new BadRequestException(
                "Buổi học đã điểm danh; mọi thay đổi phải gửi yêu cầu để Admin duyệt");
        }

        return applyAttendanceEntries(request, teacher, cls, slots);
    }

    @Override
    public List<AttendanceDto> synchronizeSessionAttendance(
            SubmitAttendanceRequest request, Long teacherUserId) {
        Teacher teacher = requireTeacher(teacherUserId);
        SchoolClass cls = classRepository.findById(request.classId())
            .orElseThrow(() -> new ResourceNotFoundException("Class", "id", request.classId()));
        validateAttendanceScope(teacher, cls, request.date(), request.shift());
        requireTeacherCanEditToday(request.date());
        List<Schedule> slots = requireScheduledSlots(cls, request.date(), request.shift());
        return applyAttendanceEntries(request, teacher, cls, slots);
    }

    private List<AttendanceDto> applyAttendanceEntries(
            SubmitAttendanceRequest request, Teacher teacher, SchoolClass cls, List<Schedule> slots) {

        List<Student> roster = enrollmentRepository.findActiveStudentsByClassAndYear(
            cls.getId(), cls.getAcademicYear().getId());
        Map<Long, Student> studentsById = roster.stream()
            .collect(Collectors.toMap(Student::getId, student -> student));
        Map<Long, AttendanceStatus> submitted = new HashMap<>();
        for (AttendanceEntry entry : request.entries()) {
            if (!studentsById.containsKey(entry.studentId())) {
                throw new BadRequestException("Học sinh không thuộc lớp được điểm danh");
            }
            if (submitted.put(entry.studentId(), entry.status()) != null) {
                throw new BadRequestException("Danh sách điểm danh có học sinh bị trùng");
            }
        }

        Map<Long, LeaveRequest> approvedLeaves = approvedLeavesByStudent(
            cls.getId(), request.date(), request.shift());

        List<AttendanceDto> results = new ArrayList<>();
        for (Student student : roster) {
            AttendanceStatus requestedStatus = submitted.getOrDefault(
                student.getId(), AttendanceStatus.PRESENT);
            LeaveRequest approvedLeave = approvedLeaves.get(student.getId());
            AttendanceStatus effectiveStatus = requestedStatus;
            if (requestedStatus == AttendanceStatus.ABSENT_WITHOUT_LEAVE && approvedLeave != null) {
                effectiveStatus = AttendanceStatus.ABSENT_WITH_LEAVE;
            }
            if (requestedStatus == AttendanceStatus.ABSENT_WITH_LEAVE && approvedLeave == null) {
                effectiveStatus = AttendanceStatus.ABSENT_WITHOUT_LEAVE;
            }
            Attendance att = attendanceRepository.findByStudentIdAndDateAndShift(
                student.getId(), request.date(), request.shift())
                .orElseGet(() -> {
                    Attendance a = new Attendance();
                    a.setStudent(student);
                    a.setDate(request.date());
                    a.setShift(request.shift());
                    return a;
                });

            att.setCls(cls);
            att.setTeacher(teacher);
            att.setSchedule(slots.get(0));
            att.setStatus(effectiveStatus);
            att.setLeaveRequest(effectiveStatus == AttendanceStatus.ABSENT_WITH_LEAVE
                ? approvedLeave : null);
            att = attendanceRepository.save(att);
            results.add(toDto(att));
        }
        synchronizeExistingSessions(cls, request.date(), request.shift());
        return results;
    }

    @Transactional(readOnly = true)
    @Override
    public AttendanceLogDto getStudentAttendanceLog(Long studentId, Long semesterId, Long requestUserId) {
        Student student = resolveAccessibleStudent(studentId, requestUserId);
        Semester semester = semesterId != null
            ? semesterRepository.findById(semesterId)
                .orElseThrow(() -> new ResourceNotFoundException("Semester", "id", semesterId))
            : semesterRepository.findFirstByIsCurrentTrueAndAcademicYearStatus(AcademicYearStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Semester", "current", true));
        boolean enrolledInYear = enrollmentRepository.findByStudentId(student.getId()).stream()
            .anyMatch(e -> e.getAcademicYear().getId().equals(semester.getAcademicYear().getId()));
        if (!enrolledInYear) {
            throw new BadRequestException("Học sinh không thuộc năm học của học kỳ đã chọn");
        }

        List<Attendance> records = attendanceRepository.findByStudentIdOrderByDateDesc(student.getId())
            .stream().filter(a -> !a.getDate().isBefore(semester.getStartDate())
                && !a.getDate().isAfter(semester.getEndDate()))
            .collect(Collectors.toList());

        List<AttendanceDto> dtos = records.stream().map(this::toDto).collect(Collectors.toList());

        int total = records.size();
        int present = (int) records.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT).count();
        int absWith = (int) records.stream().filter(a -> a.getStatus() == AttendanceStatus.ABSENT_WITH_LEAVE).count();
        int absWithout = (int) records.stream().filter(a -> a.getStatus() == AttendanceStatus.ABSENT_WITHOUT_LEAVE).count();
        double rate = total > 0 ? (double) present / total * 100 : 0;

        AttendanceStatsDto stats = new AttendanceStatsDto(student.getId(), student.getUser().getName(),
            semester.getId(), semester.getName(), total, present, absWith, absWithout,
            BigDecimal.valueOf(rate).setScale(1, RoundingMode.HALF_UP).doubleValue());

        return new AttendanceLogDto(dtos, stats);
    }

    private Teacher requireTeacher(Long teacherUserId) {
        return teacherRepository.findByUserId(teacherUserId)
            .orElseThrow(() -> new ResourceNotFoundException("Teacher", "userId", teacherUserId));
    }

    @Transactional(readOnly = true)
    @Override
    public List<ClassAttendanceSummaryDto> getClassAttendanceSummary(
            Long classId, Long semesterId, Long academicYearId) {
        SchoolClass cls = classRepository.findById(classId)
            .orElseThrow(() -> new ResourceNotFoundException("Class", "id", classId));
        Semester sem = semesterRepository.findById(semesterId)
            .orElseThrow(() -> new ResourceNotFoundException("Semester", "id", semesterId));
        if (!cls.getAcademicYear().getId().equals(academicYearId)
                || !sem.getAcademicYear().getId().equals(academicYearId)) {
            throw new BadRequestException("Lớp và học kỳ không thuộc cùng một năm học");
        }

        List<Student> students = enrollmentRepository.findActiveStudentsByClassAndYear(classId, cls.getAcademicYear().getId());
        List<ClassAttendanceSummaryDto> summaryList = new ArrayList<>();

        for (Student student : students) {
            List<Attendance> records = attendanceRepository.findByStudentIdOrderByDateDesc(student.getId())
                .stream().filter(a -> !a.getDate().isBefore(sem.getStartDate())
                    && !a.getDate().isAfter(sem.getEndDate()))
                .collect(Collectors.toList());

            int total = records.size();
            int present = (int) records.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT).count();
            int absWith = (int) records.stream().filter(a -> a.getStatus() == AttendanceStatus.ABSENT_WITH_LEAVE).count();
            int absWithout = (int) records.stream().filter(a -> a.getStatus() == AttendanceStatus.ABSENT_WITHOUT_LEAVE).count();

            double rate = total > 0 ? (double) present / total * 100 : 100.0;
            double roundedRate = BigDecimal.valueOf(rate).setScale(1, RoundingMode.HALF_UP).doubleValue();

            String suggestedConduct = "TỐT";
            if (absWithout > 5) {
                suggestedConduct = "YẾU";
            } else if (absWithout > 3 || absWith > 10) {
                suggestedConduct = "TRUNG BÌNH";
            } else if (absWithout > 0 || absWith > 5) {
                suggestedConduct = "KHÁ";
            }

            summaryList.add(new ClassAttendanceSummaryDto(
                student.getId(),
                student.getStudentCode(),
                student.getUser().getName(),
                present,
                absWith + absWithout,
                absWith,
                absWithout,
                roundedRate,
                suggestedConduct
            ));
        }

        return summaryList;
    }

    @Transactional(readOnly = true)
    @Override
    public List<AdminAttendanceDayDto> getAdminDailyAttendance(Long academicYearId, LocalDate date) {
        List<AdminAttendanceDayDto> result = new ArrayList<>();
        for (SchoolClass cls : classRepository.findByAcademicYearId(academicYearId)) {
            for (Shift shift : Shift.values()) {
                List<Schedule> slots = scheduledSlots(cls, date, shift);
                if (slots.isEmpty()) continue;
                result.add(toAdminDayDto(cls, date, shift, slots.size()));
            }
        }
        return result;
    }

    @Override
    public AdminAttendanceDayDto adjustAdminDailyAttendance(AdminAttendanceAdjustmentRequest request) {
        SchoolClass cls = classRepository.findById(request.classId())
            .orElseThrow(() -> new ResourceNotFoundException("Class", "id", request.classId()));
        if (!cls.getAcademicYear().getId().equals(request.academicYearId())) {
            throw new BadRequestException("Lớp không thuộc năm học đã chọn");
        }
        List<Schedule> slots = requireScheduledSlots(cls, request.date(), request.shift());
        List<Student> students = enrollmentRepository.findActiveStudentsByClassAndYear(
            cls.getId(), request.academicYearId());
        int requestedTotal = request.presentCount() + request.absentWithLeaveCount()
            + request.absentWithoutLeaveCount();
        if (requestedTotal != students.size()) {
            throw new BadRequestException("Tổng số có mặt và vắng phải bằng sĩ số lớp");
        }

        Map<Long, LeaveRequest> approvedLeaves = approvedLeavesByStudent(
            cls.getId(), request.date(), request.shift());
        long eligibleApprovedLeaves = students.stream()
            .filter(student -> approvedLeaves.containsKey(student.getId()))
            .count();
        if (request.absentWithLeaveCount() > eligibleApprovedLeaves) {
            throw new BadRequestException(
                "Số học sinh vắng có phép vượt quá số đơn nghỉ đã được duyệt cho ngày và buổi này");
        }
        List<Student> ordered = new ArrayList<>(students);
        ordered.sort((left, right) -> Boolean.compare(
            !approvedLeaves.containsKey(left.getId()), !approvedLeaves.containsKey(right.getId())));

        for (int index = 0; index < ordered.size(); index++) {
            Student student = ordered.get(index);
            AttendanceStatus status = index < request.absentWithLeaveCount()
                ? AttendanceStatus.ABSENT_WITH_LEAVE
                : index < request.absentWithLeaveCount() + request.absentWithoutLeaveCount()
                    ? AttendanceStatus.ABSENT_WITHOUT_LEAVE
                    : AttendanceStatus.PRESENT;
            LeaveRequest approvedLeave = approvedLeaves.get(student.getId());
            Attendance attendance = attendanceRepository.findByStudentIdAndDateAndShift(
                student.getId(), request.date(), request.shift()).orElseGet(() -> {
                    Attendance created = new Attendance();
                    created.setStudent(student);
                    created.setDate(request.date());
                    created.setShift(request.shift());
                    return created;
                });
            attendance.setCls(cls);
            attendance.setTeacher(slots.get(0).getAssignment().getTeacher());
            attendance.setSchedule(slots.get(0));
            attendance.setStatus(status);
            if (status == AttendanceStatus.ABSENT_WITH_LEAVE && approvedLeave == null) {
                throw new BadRequestException(
                    "Không thể ghi nhận vắng có phép khi chưa có đơn nghỉ được duyệt");
            }
            attendance.setLeaveRequest(
                status == AttendanceStatus.ABSENT_WITH_LEAVE ? approvedLeave : null);
            attendanceRepository.save(attendance);
        }
        synchronizeExistingSessions(cls, request.date(), request.shift());
        return toAdminDayDto(cls, request.date(), request.shift(), slots.size());
    }

    private void synchronizeExistingSessions(
            SchoolClass cls, LocalDate date, Shift shift) {
        List<AttendanceSession> sessions = attendanceSessionRepository
            .findByClsIdAndDateAndShift(cls.getId(), date, shift);
        if (sessions.isEmpty()) return;

        List<Attendance> canonicalRecords = attendanceRepository
            .findByClsIdAndDateAndShift(cls.getId(), date, shift);
        for (AttendanceSession session : sessions) {
            List<AttendanceDetail> details = attendanceDetailRepository
                .findBySessionId(session.getId());
            Map<Long, AttendanceDetail> detailsByStudent = details.stream()
                .collect(Collectors.toMap(
                    detail -> detail.getStudent().getId(),
                    detail -> detail));

            for (Attendance attendance : canonicalRecords) {
                AttendanceDetail detail = detailsByStudent.get(
                    attendance.getStudent().getId());
                if (detail == null) {
                    detail = new AttendanceDetail();
                    detail.setSession(session);
                    detail.setStudent(attendance.getStudent());
                    details.add(detail);
                    detailsByStudent.put(attendance.getStudent().getId(), detail);
                }
                detail.setStatus(attendance.getStatus());
            }
            attendanceDetailRepository.saveAll(details);

            int present = (int) details.stream()
                .filter(detail -> detail.getStatus() == AttendanceStatus.PRESENT)
                .count();
            session.setTotal(details.size());
            session.setPresent(present);
            session.setAbsent(details.size() - present);
            attendanceSessionRepository.save(session);
        }
    }

    @Override
    public AttendanceCorrectionRequestDto requestAttendanceCorrection(
            CreateAttendanceCorrectionRequest request, Long teacherUserId) {
        Teacher teacher = requireTeacher(teacherUserId);
        SchoolClass cls = classRepository.findById(request.classId())
            .orElseThrow(() -> new ResourceNotFoundException("Class", "id", request.classId()));
        validateAttendanceScope(teacher, cls, request.date(), request.shift());
        requireTeacherCanEditToday(request.date());
        requireScheduledSlots(cls, request.date(), request.shift());
        List<Attendance> existing = attendanceRepository.findByClsIdAndDateAndShift(
            cls.getId(), request.date(), request.shift());
        if (existing.isEmpty()) {
            throw new BadRequestException("Buổi học chưa được điểm danh lần đầu");
        }
        if (correctionRequestRepository.existsByClsIdAndDateAndShiftAndStatus(
                cls.getId(), request.date(), request.shift(), AttendanceCorrectionStatus.PENDING)) {
            throw new BadRequestException("Buổi học đang có yêu cầu sửa chờ Admin duyệt");
        }
        List<Student> roster = enrollmentRepository.findActiveStudentsByClassAndYear(
            cls.getId(), cls.getAcademicYear().getId());
        if (request.entries().size() != roster.size()) {
            throw new BadRequestException("Yêu cầu sửa phải chứa trạng thái của toàn bộ học sinh trong lớp");
        }
        var rosterIds = roster.stream().map(Student::getId).collect(java.util.stream.Collectors.toSet());
        var submittedIds = new java.util.HashSet<Long>();
        for (AttendanceEntry entry : request.entries()) {
            if (!rosterIds.contains(entry.studentId()) || !submittedIds.add(entry.studentId())) {
                throw new BadRequestException("Danh sách học sinh trong yêu cầu sửa không hợp lệ");
            }
        }

        AttendanceCorrectionRequest correction = new AttendanceCorrectionRequest();
        correction.setCls(cls);
        correction.setTeacher(teacher);
        correction.setDate(request.date());
        correction.setShift(request.shift());
        correction.setOriginalEntries(writeEntries(existing.stream()
            .map(attendance -> new AttendanceEntry(
                attendance.getStudent().getId(), attendance.getStatus()))
            .toList()));
        correction.setProposedEntries(writeEntries(request.entries()));
        correction.setReason(request.reason().trim());
        correction.setStatus(AttendanceCorrectionStatus.PENDING);
        return toCorrectionDto(correctionRequestRepository.save(correction));
    }

    @Transactional(readOnly = true)
    @Override
    public List<AttendanceCorrectionRequestDto> getPendingCorrections(
            Long academicYearId, LocalDate date) {
        return correctionRequestRepository
            .findByClsAcademicYearIdAndDateAndStatusOrderByCreatedAtAsc(
                academicYearId, date, AttendanceCorrectionStatus.PENDING)
            .stream().map(this::toCorrectionDto).toList();
    }

    @Transactional(readOnly = true)
    @Override
    public List<AttendanceCorrectionRequestDto> getTeacherCorrectionHistory(
            Long academicYearId, Long teacherUserId) {
        Teacher teacher = requireTeacher(teacherUserId);
        return correctionRequestRepository
            .findByTeacherIdAndClsAcademicYearIdOrderByCreatedAtDesc(
                teacher.getId(), academicYearId)
            .stream().limit(50).map(this::toCorrectionDto).toList();
    }

    @Transactional(readOnly = true)
    @Override
    public List<AttendanceCorrectionRequestDto> getAdminCorrectionHistory(
            Long academicYearId, LocalDate date) {
        return correctionRequestRepository
            .findByClsAcademicYearIdAndDateOrderByCreatedAtDesc(academicYearId, date)
            .stream().map(this::toCorrectionDto).toList();
    }

    @Override
    public AttendanceCorrectionRequestDto reviewAttendanceCorrection(
            Long requestId, boolean approve, Long reviewerUserId) {
        AttendanceCorrectionRequest correction = correctionRequestRepository.findById(requestId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "AttendanceCorrectionRequest", "id", requestId));
        if (correction.getStatus() != AttendanceCorrectionStatus.PENDING) {
            throw new BadRequestException("Yêu cầu sửa điểm danh đã được xử lý");
        }
        if (approve) {
            List<AttendanceEntry> entries = readEntries(correction.getProposedEntries());
            SubmitAttendanceRequest request = new SubmitAttendanceRequest(
                correction.getCls().getId(), correction.getDate(), correction.getShift(), entries);
            List<Schedule> slots = requireScheduledSlots(
                correction.getCls(), correction.getDate(), correction.getShift());
            applyAttendanceEntries(request, correction.getTeacher(), correction.getCls(), slots);
            correction.setStatus(AttendanceCorrectionStatus.APPROVED);
        } else {
            correction.setStatus(AttendanceCorrectionStatus.REJECTED);
        }
        correction.setReviewedAt(LocalDateTime.now());
        correction.setReviewedBy(userRepository.findById(reviewerUserId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", reviewerUserId)));
        return toCorrectionDto(correctionRequestRepository.save(correction));
    }

    private Student resolveAccessibleStudent(Long requestedStudentId, Long requestUserId) {
        Optional<Student> self = studentRepository.findByUserId(requestUserId);
        if (self.isPresent()) {
            if (requestedStudentId != null && !self.get().getId().equals(requestedStudentId)) {
                throw new ForbiddenException("Học sinh chỉ được xem chuyên cần của chính mình");
            }
            return self.get();
        }

        Parent parent = parentRepository.findByUserId(requestUserId)
            .orElseThrow(() -> new ForbiddenException("Tài khoản không có quyền xem chuyên cần"));
        if (requestedStudentId == null
                || !studentGuardianRepository.existsByStudentIdAndGuardianId(
                    requestedStudentId, parent.getId())) {
            throw new ForbiddenException("Phụ huynh không quản lý học sinh này");
        }
        return studentRepository.findById(requestedStudentId)
            .orElseThrow(() -> new ResourceNotFoundException("Student", "id", requestedStudentId));
    }

    private void validateAttendanceScope(
            Teacher teacher, SchoolClass cls, LocalDate date, Shift shift) {
        if (date.isBefore(cls.getAcademicYear().getStartDate())
                || date.isAfter(cls.getAcademicYear().getEndDate())) {
            throw new BadRequestException("Ngày điểm danh không thuộc năm học của lớp");
        }
        if (!homeroomAssignmentRepository.existsActiveForTeacherClassAndDate(
                teacher.getId(), cls.getId(), cls.getAcademicYear().getId(), date)) {
            throw new ForbiddenException("Chỉ giáo viên chủ nhiệm được điểm danh lớp này");
        }
        if (!academicYearShiftRepository.existsByAcademicYearIdAndShiftCode(
                cls.getAcademicYear().getId(), shift.name())) {
            throw new BadRequestException("Buổi học chưa được cấu hình cho năm học");
        }
    }

    private void requireTeacherCanEditToday(LocalDate date) {
        if (enforceCurrentDay && !date.equals(LocalDate.now())) {
            throw new BadRequestException("Giáo viên chỉ được điểm danh hoặc sửa điểm danh trong ngày hiện tại");
        }
    }

    private List<Schedule> requireScheduledSlots(SchoolClass cls, LocalDate date, Shift shift) {
        List<Schedule> slots = scheduledSlots(cls, date, shift);
        if (slots.isEmpty()) {
            throw new BadRequestException("Lớp không có lịch học trong buổi đã chọn");
        }
        return slots;
    }

    private List<Schedule> scheduledSlots(SchoolClass cls, LocalDate date, Shift shift) {
        Semester semester = semesterRepository.findByAcademicYearId(cls.getAcademicYear().getId()).stream()
            .filter(item -> !date.isBefore(item.getStartDate()) && !date.isAfter(item.getEndDate()))
            .findFirst().orElse(null);
        if (semester == null) return List.of();
        Timetable timetable = timetableRepository.findEffective(cls.getId(), semester.getId(), date)
            .stream().findFirst().orElse(null);
        if (timetable == null) return List.of();
        int dayOfWeek = date.getDayOfWeek().getValue() % 7 + 1;
        return scheduleRepository.findByTimetableIdOrderByDayOfWeekAscPeriodAsc(timetable.getId())
            .stream()
            .filter(slot -> slot.getDayOfWeek() == dayOfWeek && slot.getShift() == shift)
            .toList();
    }

    private AdminAttendanceDayDto toAdminDayDto(
            SchoolClass cls, LocalDate date, Shift shift, int scheduledPeriods) {
        List<Attendance> records = attendanceRepository.findByClsIdAndDateAndShift(cls.getId(), date, shift);
        int totalStudents = enrollmentRepository.findActiveStudentsByClassAndYear(
            cls.getId(), cls.getAcademicYear().getId()).size();
        int present = (int) records.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT).count();
        int absentWithLeave = (int) records.stream()
            .filter(a -> a.getStatus() == AttendanceStatus.ABSENT_WITH_LEAVE).count();
        int absentWithoutLeave = (int) records.stream()
            .filter(a -> a.getStatus() == AttendanceStatus.ABSENT_WITHOUT_LEAVE).count();
        return new AdminAttendanceDayDto(cls.getId(), cls.getName(), date, shift,
            scheduledPeriods, totalStudents, totalStudents > 0 && records.size() == totalStudents,
            present, absentWithLeave, absentWithoutLeave);
    }

    private String writeEntries(List<AttendanceEntry> entries) {
        try {
            return objectMapper.writeValueAsString(entries);
        } catch (JsonProcessingException exception) {
            throw new BadRequestException("Không thể tạo yêu cầu sửa điểm danh");
        }
    }

    private List<AttendanceEntry> readEntries(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<AttendanceEntry>>() {});
        } catch (JsonProcessingException exception) {
            throw new BadRequestException("Dữ liệu yêu cầu sửa điểm danh không hợp lệ");
        }
    }

    private AttendanceCorrectionRequestDto toCorrectionDto(AttendanceCorrectionRequest correction) {
        List<AttendanceEntry> originalEntries = readEntriesOrEmpty(correction.getOriginalEntries());
        List<AttendanceEntry> entries = readEntries(correction.getProposedEntries());
        Map<Long, AttendanceStatus> originalByStudent = originalEntries.stream()
            .collect(Collectors.toMap(AttendanceEntry::studentId, AttendanceEntry::status));
        Map<Long, Student> rosterByStudent = enrollmentRepository
            .findActiveStudentsByClassAndYear(
                correction.getCls().getId(), correction.getCls().getAcademicYear().getId())
            .stream().collect(Collectors.toMap(Student::getId, student -> student));
        List<AttendanceCorrectionEntryDto> changes = entries.stream()
            .filter(entry -> originalByStudent.get(entry.studentId()) != entry.status())
            .map(entry -> {
                Student student = rosterByStudent.get(entry.studentId());
                return new AttendanceCorrectionEntryDto(
                    entry.studentId(),
                    student == null ? "Học sinh" : student.getUser().getName(),
                    student == null ? "" : student.getStudentCode(),
                    originalByStudent.get(entry.studentId()), entry.status());
            }).toList();
        int originalPresent = countStatus(originalEntries, AttendanceStatus.PRESENT);
        int originalAbsentWithLeave = countStatus(
            originalEntries, AttendanceStatus.ABSENT_WITH_LEAVE);
        int originalAbsentWithoutLeave = countStatus(
            originalEntries, AttendanceStatus.ABSENT_WITHOUT_LEAVE);
        int present = (int) entries.stream()
            .filter(entry -> entry.status() == AttendanceStatus.PRESENT).count();
        int absentWithLeave = (int) entries.stream()
            .filter(entry -> entry.status() == AttendanceStatus.ABSENT_WITH_LEAVE).count();
        int absentWithoutLeave = (int) entries.stream()
            .filter(entry -> entry.status() == AttendanceStatus.ABSENT_WITHOUT_LEAVE).count();
        return new AttendanceCorrectionRequestDto(
            correction.getId(), correction.getCls().getId(), correction.getCls().getName(),
            correction.getTeacher().getUser().getName(), correction.getDate(), correction.getShift(),
            correction.getStatus(), originalPresent, originalAbsentWithLeave,
            originalAbsentWithoutLeave, present, absentWithLeave, absentWithoutLeave,
            correction.getReason(), changes, correction.getCreatedAt(),
            correction.getReviewedBy() == null ? null : correction.getReviewedBy().getName(),
            correction.getReviewedAt());
    }

    private int countStatus(List<AttendanceEntry> entries, AttendanceStatus status) {
        return (int) entries.stream().filter(entry -> entry.status() == status).count();
    }

    private List<AttendanceEntry> readEntriesOrEmpty(String json) {
        return json == null || json.isBlank() ? List.of() : readEntries(json);
    }

    private List<Shift> configuredShifts(SchoolClass cls) {
        List<Shift> shifts = academicYearShiftRepository
            .findByAcademicYearId(cls.getAcademicYear().getId()).stream()
            .map(item -> {
                try {
                    return Shift.valueOf(item.getShift().getCode().toUpperCase());
                } catch (IllegalArgumentException ignored) {
                    return null;
                }
            })
            .filter(java.util.Objects::nonNull)
            .distinct()
            .toList();
        if (shifts.isEmpty()) {
            throw new BadRequestException("Năm học chưa cấu hình buổi học để điểm danh");
        }
        return shifts;
    }

    private Map<Long, LeaveRequest> approvedLeavesByStudent(
            Long classId, LocalDate date, Shift shift) {
        return leaveRequestRepository.findApprovedByClassAndDate(classId, date).stream()
            .filter(request -> coversShift(request.getShift(), shift))
            .collect(Collectors.toMap(
                request -> request.getStudent().getId(),
                request -> request,
                (first, ignored) -> first));
    }

    private boolean coversShift(LeaveShift leaveShift, Shift attendanceShift) {
        return leaveShift == LeaveShift.FULL_DAY
            || (leaveShift == LeaveShift.MORNING && attendanceShift == Shift.MORNING)
            || (leaveShift == LeaveShift.AFTERNOON && attendanceShift == Shift.AFTERNOON);
    }

    private AttendanceDto toDto(Attendance a) {
        return new AttendanceDto(a.getId(), a.getStudent().getId(), a.getStudent().getUser().getName(),
            a.getStudent().getStudentCode(), a.getCls().getId(), a.getCls().getName(),
            a.getDate(), a.getShift(), a.getStatus(),
            a.getLeaveRequest() != null ? a.getLeaveRequest().getId() : null,
            a.getTeacher() != null ? a.getTeacher().getUser().getName() : null, a.getCreatedAt());
    }
}
