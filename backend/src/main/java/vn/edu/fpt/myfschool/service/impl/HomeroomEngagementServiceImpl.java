package vn.edu.fpt.myfschool.service.impl;

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
import vn.edu.fpt.myfschool.service.HomeroomEngagementService;
import vn.edu.fpt.myfschool.service.NotificationService;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class HomeroomEngagementServiceImpl implements HomeroomEngagementService {
    private final AcademicYearRepository years;
    private final SemesterRepository semesters;
    private final ClassRepository classes;
    private final StudentRepository students;
    private final TeacherRepository teachers;
    private final ParentRepository parents;
    private final UserRepository users;
    private final EnrollmentRepository enrollments;
    private final HomeroomAssignmentRepository homeroomAssignments;
    private final StudentGuardianRepository guardians;
    private final ParentContactLogRepository contactLogs;
    private final ParentMeetingRepository meetings;
    private final ParentMeetingParticipantRepository participants;
    private final NotificationService notifications;

    @Override
    @Transactional(readOnly = true)
    public List<ParentContactLogDto> getContactLogs(Long studentId, Long academicYearId, Long semesterId,
            Long classId, Long teacherUserId) {
        Scope scope = requireScope(academicYearId, semesterId, classId);
        requireHomeroom(scope, teacherUserId);
        requireHistoricalStudent(scope, studentId);
        return contactLogs.findByStudentIdAndAcademicYearIdAndSemesterIdOrderByContactedAtDesc(
                studentId, academicYearId, semesterId).stream()
                .filter(item -> item.getCls().getId().equals(classId)).map(this::toContact).toList();
    }

    @Override
    public ParentContactLogDto createContactLog(Long studentId, SaveParentContactLogRequest request,
            Long teacherUserId) {
        Scope scope = requireScope(request.academicYearId(), request.semesterId(), request.classId());
        requireHomeroom(scope, teacherUserId);
        Student student = requireActiveStudent(scope, studentId);
        ParentContactLog log = new ParentContactLog();
        applyContact(log, scope, student, request);
        log.setCreatedBy(requireUser(teacherUserId));
        return toContact(contactLogs.save(log));
    }

    @Override
    public ParentContactLogDto updateContactLog(Long id, SaveParentContactLogRequest request, Long teacherUserId) {
        ParentContactLog log = contactLogs.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ParentContactLog", "id", id));
        Scope scope = requireScope(request.academicYearId(), request.semesterId(), request.classId());
        if (!sameScope(log.getAcademicYear().getId(), log.getSemester().getId(), log.getCls().getId(), scope)) {
            throw new ForbiddenException("Không được chuyển nhật ký sang phạm vi khác");
        }
        requireHomeroom(scope, teacherUserId);
        applyContact(log, scope, log.getStudent(), request);
        return toContact(contactLogs.save(log));
    }

    @Override
    public void deleteContactLog(Long id, Long teacherUserId) {
        ParentContactLog log = contactLogs.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ParentContactLog", "id", id));
        Scope scope = requireScope(log.getAcademicYear().getId(), log.getSemester().getId(), log.getCls().getId());
        requireHomeroom(scope, teacherUserId);
        contactLogs.delete(log);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParentMeetingDto> getMeetings(Long academicYearId, Long semesterId, Long classId,
            Long requesterId, UserRole requesterRole) {
        requireYearSemester(academicYearId, semesterId);
        if (requesterRole == UserRole.PARENT) {
            return participants.findForGuardian(requesterId, academicYearId, semesterId).stream()
                    .map(item -> toMeeting(item.getMeeting(), requesterId)).toList();
        }
        if (classId == null) throw new BadRequestException("Phải chọn lớp");
        Scope scope = requireScope(academicYearId, semesterId, classId);
        if (requesterRole == UserRole.TEACHER) requireHomeroom(scope, requesterId);
        else if (requesterRole != UserRole.ADMIN) throw new ForbiddenException("Không có quyền xem lịch họp");
        return meetings.findByAcademicYearIdAndSemesterIdAndClsIdOrderByStartsAtDesc(
                academicYearId, semesterId, classId).stream().map(item -> toMeeting(item, null)).toList();
    }

    @Override
    public ParentMeetingDto createMeeting(SaveParentMeetingRequest request, Long teacherUserId) {
        Scope scope = requireScope(request.academicYearId(), request.semesterId(), request.classId());
        requireHomeroom(scope, teacherUserId);
        ParentMeeting meeting = new ParentMeeting();
        applyMeeting(meeting, scope, request);
        meeting.setCreatedBy(requireUser(teacherUserId));
        meeting = meetings.save(meeting);

        List<Student> invitedStudents = request.studentId() == null
                ? enrollments.findActiveStudentsByClassAndYear(scope.cls().getId(), scope.year().getId())
                : List.of(requireActiveStudent(scope, request.studentId()));
        Map<Long, Parent> invitedGuardians = new LinkedHashMap<>();
        invitedStudents.forEach(student -> guardians.findGuardiansByStudentId(student.getId())
                .forEach(parent -> invitedGuardians.put(parent.getId(), parent)));
        if (invitedGuardians.isEmpty()) throw new ConflictException("Không tìm thấy phụ huynh để gửi lời mời");
        for (Parent guardian : invitedGuardians.values()) {
            ParentMeetingParticipant participant = new ParentMeetingParticipant();
            participant.setMeeting(meeting);
            participant.setGuardian(guardian);
            participant = participants.save(participant);
            meeting.getParticipants().add(participant);
            notifications.createNotification(guardian.getUser().getId(), "Lời mời họp phụ huynh",
                    meeting.getTitle() + " - " + meeting.getStartsAt(), "Lịch họp", meeting.getId(), "PARENT_MEETING");
        }
        return toMeeting(meeting, null);
    }

    @Override
    public ParentMeetingDto updateMeeting(Long id, SaveParentMeetingRequest request, Long teacherUserId) {
        ParentMeeting meeting = requireMeeting(id);
        Scope scope = requireScope(request.academicYearId(), request.semesterId(), request.classId());
        if (!sameScope(meeting.getAcademicYear().getId(), meeting.getSemester().getId(), meeting.getCls().getId(), scope)
                || !Objects.equals(meeting.getStudent() == null ? null : meeting.getStudent().getId(), request.studentId())) {
            throw new ForbiddenException("Không được đổi phạm vi hoặc đối tượng của lịch họp đã gửi");
        }
        requireHomeroom(scope, teacherUserId);
        applyMeeting(meeting, scope, request);
        ParentMeeting saved = meetings.save(meeting);
        saved.getParticipants().forEach(item -> notifications.createNotification(item.getGuardian().getUser().getId(),
                "Lịch họp đã cập nhật", saved.getTitle() + " - " + saved.getStartsAt(), "Lịch họp",
                saved.getId(), "PARENT_MEETING"));
        return toMeeting(saved, null);
    }

    @Override
    public ParentMeetingDto respondMeeting(Long id, MeetingResponseRequest request, Long parentUserId) {
        if (request.response() == MeetingResponse.PENDING) {
            throw new BadRequestException("Phản hồi phải là tham gia hoặc từ chối");
        }
        ParentMeetingParticipant participant = participants.findByMeetingAndGuardianUser(id, parentUserId)
                .orElseThrow(() -> new ForbiddenException("Phụ huynh không thuộc danh sách được mời"));
        if (participant.getMeeting().getStatus() != ParentMeetingStatus.SCHEDULED) {
            throw new ConflictException("Lịch họp không còn nhận phản hồi");
        }
        participant.setResponse(request.response());
        participant.setRespondedAt(LocalDateTime.now());
        participants.save(participant);
        return toMeeting(participant.getMeeting(), parentUserId);
    }

    @Override
    public ParentMeetingDto markAttendance(Long id, MeetingAttendanceRequest request, Long teacherUserId) {
        ParentMeeting meeting = requireMeeting(id);
        Scope scope = requireScope(meeting.getAcademicYear().getId(), meeting.getSemester().getId(), meeting.getCls().getId());
        requireHomeroom(scope, teacherUserId);
        ParentMeetingParticipant participant = participants.findByMeetingIdAndGuardianId(id, request.guardianId())
                .orElseThrow(() -> new ResourceNotFoundException("ParentMeetingParticipant", "guardianId", request.guardianId()));
        participant.setAttendance(request.attendance());
        participants.save(participant);
        return toMeeting(meeting, null);
    }

    private void applyContact(ParentContactLog log, Scope scope, Student student, SaveParentContactLogRequest request) {
        requireDateInSemester(request.contactedAt().toLocalDate(), scope, "Ngày liên hệ");
        log.setAcademicYear(scope.year()); log.setSemester(scope.semester()); log.setCls(scope.cls()); log.setStudent(student);
        log.setContactType(request.contactType()); log.setSubject(request.subject().trim());
        log.setSummary(request.summary().trim()); log.setResult(trim(request.result()));
        log.setContactedAt(request.contactedAt()); log.setNextActionAt(request.nextActionAt());
    }

    private void applyMeeting(ParentMeeting meeting, Scope scope, SaveParentMeetingRequest request) {
        requireDateInSemester(request.startsAt().toLocalDate(), scope, "Ngày họp");
        meeting.setTitle(request.title().trim()); meeting.setAcademicYear(scope.year());
        meeting.setSemester(scope.semester()); meeting.setCls(scope.cls());
        meeting.setStudent(request.studentId() == null ? null : requireActiveStudent(scope, request.studentId()));
        meeting.setStartsAt(request.startsAt()); meeting.setLocation(trim(request.location()));
        meeting.setAgenda(trim(request.agenda()));
        meeting.setStatus(request.status() == null ? ParentMeetingStatus.SCHEDULED : request.status());
    }

    private Scope requireScope(Long academicYearId, Long semesterId, Long classId) {
        AcademicYear year = years.findById(academicYearId)
                .orElseThrow(() -> new ResourceNotFoundException("AcademicYear", "id", academicYearId));
        Semester semester = semesters.findById(semesterId)
                .orElseThrow(() -> new ResourceNotFoundException("Semester", "id", semesterId));
        SchoolClass cls = classes.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class", "id", classId));
        if (!semester.getAcademicYear().getId().equals(academicYearId)
                || !cls.getAcademicYear().getId().equals(academicYearId)) {
            throw new ForbiddenException("Năm học, học kỳ và lớp không cùng phạm vi");
        }
        return new Scope(year, semester, cls);
    }

    private void requireYearSemester(Long academicYearId, Long semesterId) {
        AcademicYear year = years.findById(academicYearId)
                .orElseThrow(() -> new ResourceNotFoundException("AcademicYear", "id", academicYearId));
        Semester semester = semesters.findById(semesterId)
                .orElseThrow(() -> new ResourceNotFoundException("Semester", "id", semesterId));
        if (!semester.getAcademicYear().getId().equals(year.getId())) {
            throw new ForbiddenException("Học kỳ không thuộc năm học đã chọn");
        }
    }

    private Teacher requireHomeroom(Scope scope, Long teacherUserId) {
        Teacher teacher = teachers.findByUserId(teacherUserId)
                .orElseThrow(() -> new ForbiddenException("Không tìm thấy hồ sơ giáo viên"));
        if (homeroomAssignments.findActiveByClassAndYear(scope.cls().getId(), scope.year().getId())
                .filter(item -> item.getTeacher().getId().equals(teacher.getId())).isEmpty()) {
            throw new ForbiddenException("Giáo viên chỉ được quản lý hồ sơ lớp chủ nhiệm");
        }
        return teacher;
    }

    private Student requireActiveStudent(Scope scope, Long studentId) {
        if (!enrollments.existsByStudentIdAndClsIdAndAcademicYearIdAndStatus(studentId, scope.cls().getId(),
                scope.year().getId(), EnrollmentStatus.ACTIVE)) {
            throw new ForbiddenException("Học sinh không thuộc lớp trong năm học đã chọn");
        }
        return requireStudent(studentId);
    }

    private Student requireHistoricalStudent(Scope scope, Long studentId) {
        boolean existed = enrollments.findByStudentId(studentId).stream()
                .anyMatch(item -> item.getAcademicYear().getId().equals(scope.year().getId())
                        && item.getCls().getId().equals(scope.cls().getId()));
        if (!existed) throw new ForbiddenException("Học sinh không có lịch sử trong lớp đã chọn");
        return requireStudent(studentId);
    }

    private void authorizeStudent(Long studentId, Long requesterId, UserRole role) {
        if (role == UserRole.STUDENT) {
            Student student = students.findByUserId(requesterId)
                    .orElseThrow(() -> new ForbiddenException("Không tìm thấy hồ sơ học sinh"));
            if (!student.getId().equals(studentId)) throw new ForbiddenException("Chỉ được xem hồ sơ của chính mình");
            return;
        }
        if (role == UserRole.PARENT) {
            Parent parent = parents.findByUserId(requesterId)
                    .orElseThrow(() -> new ForbiddenException("Không tìm thấy hồ sơ phụ huynh"));
            if (!guardians.existsByStudentIdAndGuardianId(studentId, parent.getId())) {
                throw new ForbiddenException("Học sinh chưa liên kết với phụ huynh");
            }
            return;
        }
        throw new ForbiddenException("Không có quyền xem hồ sơ đã công bố");
    }

    private ParentContactLogDto toContact(ParentContactLog value) {
        return new ParentContactLogDto(value.getId(), value.getStudent().getId(), value.getStudent().getUser().getName(),
                value.getAcademicYear().getId(), value.getSemester().getId(), value.getCls().getId(), value.getCls().getName(),
                value.getContactType(), value.getSubject(), value.getSummary(), value.getResult(), value.getContactedAt(),
                value.getNextActionAt(), value.getCreatedBy().getId(), value.getCreatedBy().getName());
    }

    private ParentMeetingDto toMeeting(ParentMeeting value, Long guardianUserFilter) {
        List<ParentMeetingParticipantDto> rows = value.getParticipants().stream()
                .filter(item -> guardianUserFilter == null || item.getGuardian().getUser().getId().equals(guardianUserFilter))
                .map(item -> new ParentMeetingParticipantDto(item.getGuardian().getId(), item.getGuardian().getUser().getName(),
                        item.getResponse(), item.getAttendance(), item.getRespondedAt())).toList();
        return new ParentMeetingDto(value.getId(), value.getTitle(), value.getAcademicYear().getId(),
                value.getSemester().getId(), value.getCls().getId(), value.getCls().getName(),
                value.getStudent() == null ? null : value.getStudent().getId(),
                value.getStudent() == null ? null : value.getStudent().getUser().getName(), value.getStartsAt(),
                value.getLocation(), value.getAgenda(), value.getStatus(), rows);
    }

    private boolean sameScope(Long yearId, Long semesterId, Long classId, Scope scope) {
        return yearId.equals(scope.year().getId()) && semesterId.equals(scope.semester().getId())
                && classId.equals(scope.cls().getId());
    }

    private void requireDateInSemester(java.time.LocalDate date, Scope scope, String label) {
        if (date.isBefore(scope.semester().getStartDate()) || date.isAfter(scope.semester().getEndDate())) {
            throw new BadRequestException(label + " không thuộc học kỳ đã chọn");
        }
    }

    private Student requireStudent(Long id) {
        return students.findById(id).orElseThrow(() -> new ResourceNotFoundException("Student", "id", id));
    }
    private User requireUser(Long id) {
        return users.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }
    private ParentMeeting requireMeeting(Long id) {
        return meetings.findById(id).orElseThrow(() -> new ResourceNotFoundException("ParentMeeting", "id", id));
    }
    private String trim(String value) { return value == null || value.trim().isEmpty() ? null : value.trim(); }
    private record Scope(AcademicYear year, Semester semester, SchoolClass cls) {}
}
