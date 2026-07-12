package vn.edu.fpt.myfschool.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.AnnouncementDto;
import vn.edu.fpt.myfschool.common.enums.AssignmentStatus;
import vn.edu.fpt.myfschool.common.enums.TargetRole;
import vn.edu.fpt.myfschool.common.enums.UserRole;
import vn.edu.fpt.myfschool.common.exception.ForbiddenException;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.entity.*;
import vn.edu.fpt.myfschool.repository.*;
import vn.edu.fpt.myfschool.service.AnnouncementService;
import vn.edu.fpt.myfschool.service.NotificationService;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service("announcementService")
@RequiredArgsConstructor
@Transactional
public class AnnouncementServiceImpl implements AnnouncementService {
    private final AnnouncementRepository announcementRepository;
    private final AnnouncementClassRepository announcementClassRepository;
    private final AnnouncementReadRepository announcementReadRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;
    private final StudentGuardianRepository studentGuardianRepository;
    private final TeachingAssignmentRepository teachingAssignmentRepository;
    private final HomeroomAssignmentRepository homeroomAssignmentRepository;
    private final ClassRepository schoolClassRepository;
    private final AcademicYearRepository academicYearRepository;
    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final AcademicYearSubjectRepository academicYearSubjectRepository;
    private final NotificationService notificationService;

    @Override
    public AnnouncementDto createAnnouncement(String title, String body, TargetRole targetRole,
            boolean requiresReply, Long academicYearId, List<Long> classIds, Long teacherUserId) {
        Teacher teacher = teacherByUser(teacherUserId);
        academicYearId = resolveYearId(academicYearId, classIds);
        validateTeacherClasses(teacher, academicYearId, classIds);
        Announcement ann = base(title, body, targetRole, academicYearId, teacherUserId);
        ann.setTeacher(teacher);
        ann.setRequiresReply(requiresReply);
        ann.setRecipientScope("CLASSES");
        ann.setApprovalStatus("PENDING");
        ann.setSenderType(isHomeroomTeacher(teacher.getId(), academicYearId, classIds)
                ? "HOMEROOM_TEACHER" : "SUBJECT_TEACHER");
        ann = announcementRepository.save(ann);
        replaceClasses(ann, classIds);
        notifyAdmins(ann);
        return toDto(ann, false);
    }

    @Override
    public AnnouncementDto updateAnnouncement(Long id, String title, String body, TargetRole targetRole,
            Long academicYearId, List<Long> classIds, Long userId) {
        Announcement ann = ownedByTeacher(id, userId);
        academicYearId = resolveYearId(academicYearId, classIds);
        if ("APPROVED".equals(ann.getApprovalStatus())) throw new ForbiddenException("Thông báo đã duyệt không thể sửa");
        validateTeacherClasses(ann.getTeacher(), academicYearId, classIds);
        ann.setTitle(title); ann.setBody(body); ann.setTargetRole(targetRole);
        ann.setAcademicYear(year(academicYearId)); ann.setApprovalStatus("PENDING"); ann.setRejectionReason(null);
        ann.setSenderType(isHomeroomTeacher(ann.getTeacher().getId(), academicYearId, classIds)
                ? "HOMEROOM_TEACHER" : "SUBJECT_TEACHER");
        replaceClasses(ann, classIds);
        notifyAdmins(ann);
        return toDto(ann, false);
    }

    @Override
    public void deleteAnnouncement(Long id, Long userId, UserRole role) {
        Announcement ann = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", "id", id));
        if (role != UserRole.ADMIN && !ann.getSender().getId().equals(userId))
            throw new ForbiddenException("Bạn không có quyền xóa thông báo này");
        announcementRepository.delete(ann);
    }

    @Override @Transactional(readOnly = true)
    public List<AnnouncementDto> getAdminAnnouncements(Long academicYearId, String status) {
        List<Announcement> items = status == null || status.isBlank()
                ? announcementRepository.findByAcademicYearIdOrderByCreatedAtDesc(academicYearId)
                : announcementRepository.findByAcademicYearIdAndApprovalStatusOrderByCreatedAtDesc(academicYearId, status);
        return items.stream().map(a -> toDto(a, false)).toList();
    }

    @Override
    public AnnouncementDto review(Long id, boolean approve, String reason, Long adminUserId) {
        Announcement ann = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", "id", id));
        if (!"PENDING".equals(ann.getApprovalStatus())) throw new ForbiddenException("Thông báo không còn chờ duyệt");
        if (!approve && (reason == null || reason.isBlank())) throw new IllegalArgumentException("Phải nhập lý do từ chối");
        ann.setApprovalStatus(approve ? "APPROVED" : "REJECTED");
        ann.setRejectionReason(approve ? null : reason.trim());
        if (approve) publish(ann);
        else notificationService.createNotification(ann.getSender().getId(), "Thông báo bị từ chối",
                reason, "Quản trị viên", ann.getId(), "ANNOUNCEMENT");
        return toDto(ann, false);
    }

    @Override
    public AnnouncementDto createAdminAnnouncement(String title, String body, Long academicYearId,
            String recipientScope, TargetRole targetRole, List<Long> classIds,
            String teacherAudience, Long subjectId, Long adminUserId) {
        year(academicYearId);
        String scope = recipientScope == null ? "SCHOOL" : recipientScope.toUpperCase(Locale.ROOT);
        if (!Set.of("SCHOOL", "CLASSES", "TEACHERS").contains(scope))
            throw new IllegalArgumentException("Phạm vi người nhận không hợp lệ");
        TargetRole role = targetRole == null ? TargetRole.ALL : targetRole;
        Announcement ann = base(title, body, role, academicYearId, adminUserId);
        ann.setApprovalStatus("APPROVED"); ann.setSenderType("ADMIN"); ann.setRequiresReply(false);
        ann.setRecipientScope(scope);
        Set<Long> recipientIds = new LinkedHashSet<>();

        if ("SCHOOL".equals(scope)) {
            teacherRepository.findAll().stream().map(Teacher::getUser).filter(Objects::nonNull)
                    .forEach(user -> recipientIds.add(user.getId()));
            List<Long> yearClassIds = schoolClassRepository.findByAcademicYearId(academicYearId).stream()
                    .map(SchoolClass::getId).toList();
            collectClassRecipients(yearClassIds, TargetRole.ALL, recipientIds);
            recipientIds.remove(adminUserId);
        } else if ("CLASSES".equals(scope)) {
            List<Long> selectedClasses = classIds == null ? List.of() : classIds;
            validateAdminClasses(academicYearId, selectedClasses);
            ann = announcementRepository.save(ann);
            replaceClasses(ann, selectedClasses);
            collectClassRecipients(selectedClasses, role, recipientIds);
        } else {
            String audience = teacherAudience == null ? "ALL" : teacherAudience.toUpperCase(Locale.ROOT);
            if (!Set.of("ALL", "SUBJECT", "HOMEROOM").contains(audience))
                throw new IllegalArgumentException("Nhóm giáo viên không hợp lệ");
            ann.setTeacherAudience(audience);
            if ("SUBJECT".equals(audience)) {
                if (subjectId == null || !academicYearSubjectRepository.existsByAcademicYearIdAndSubjectId(academicYearId, subjectId))
                    throw new ForbiddenException("Môn học không thuộc năm học đã chọn");
                Subject subject = subjectRepository.findById(subjectId)
                        .orElseThrow(() -> new ResourceNotFoundException("Subject", "id", subjectId));
                ann.setRecipientSubject(subject);
                teachingAssignmentRepository.findByAcademicYearId(academicYearId).stream()
                        .filter(assignment -> assignment.getSubject().getId().equals(subjectId))
                        .map(TeachingAssignment::getTeacher).map(Teacher::getUser).filter(Objects::nonNull)
                        .forEach(user -> recipientIds.add(user.getId()));
            } else if ("HOMEROOM".equals(audience)) {
                homeroomAssignmentRepository.findAll().stream()
                        .filter(h -> h.getAcademicYear().getId().equals(academicYearId))
                        .forEach(h -> recipientIds.add(h.getTeacher().getUser().getId()));
            } else {
                teacherRepository.findAll().forEach(t -> recipientIds.add(t.getUser().getId()));
            }
        }
        if (ann.getId() == null) ann = announcementRepository.save(ann);
        Long announcementId = ann.getId();
        recipientIds.forEach(id -> notificationService.createNotification(id, title, body,
                "Nhà trường", announcementId, "ANNOUNCEMENT"));
        return toDto(ann, false);
    }

    @Override @Transactional(readOnly = true)
    public List<Map<String, Object>> getEligibleClasses(Long academicYearId, Long teacherUserId) {
        Teacher teacher = teacherByUser(teacherUserId);
        Set<Long> ids = new LinkedHashSet<>(teachingAssignmentRepository.findActiveClassIdsByTeacherAndYear(teacher.getId(), academicYearId));
        homeroomAssignmentRepository.findActiveByTeacherAndYear(teacher.getId(), academicYearId)
                .forEach(h -> ids.add(h.getCls().getId()));
        return schoolClassRepository.findAllById(ids).stream()
                .map(c -> Map.<String, Object>of("id", c.getId(), "name", c.getName(),
                        "isHomeroom", homeroomAssignmentRepository.existsByTeacherIdAndClsIdAndAcademicYearId(
                                teacher.getId(), c.getId(), academicYearId)))
                .sorted(Comparator.comparing(m -> (String) m.get("name"))).toList();
    }

    @Override @Transactional(readOnly = true)
    public List<AnnouncementDto> getMyAnnouncements(Long userId) {
        Teacher teacher = teacherByUser(userId);
        return announcementRepository.findByTeacherIdOrderByCreatedAtDesc(teacher.getId()).stream()
                .map(a -> toDto(a, false)).toList();
    }

    @Override @Transactional(readOnly = true)
    public AnnouncementDto getAnnouncementDetail(Long id, Long userId, UserRole role) {
        Announcement ann = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", "id", id));
        if (role != UserRole.ADMIN && !canViewAnnouncement(ann, userId, role))
            throw new ForbiddenException("Bạn không có quyền xem thông báo này");
        return toDto(ann, announcementReadRepository.existsByAnnouncementIdAndUserId(id, userId));
    }

    @Override @Transactional(readOnly = true)
    public List<AnnouncementDto> getAnnouncements(Long userId, UserRole role) {
        List<Long> ids = getVisibleClassIds(userId, role);
        if (ids.isEmpty()) return List.of();
        return announcementRepository.findByClassesAndTargetRoles(ids, targetRolesFor(role)).stream()
                .map(a -> toDto(a, announcementReadRepository.existsByAnnouncementIdAndUserId(a.getId(), userId))).toList();
    }

    @Override public void markAsRead(Long id, Long userId, UserRole role) {
        Announcement ann = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", "id", id));
        if (!canViewAnnouncement(ann, userId, role)) throw new ForbiddenException("Bạn không có quyền đọc thông báo này");
        AnnouncementRead read = announcementReadRepository.findByAnnouncementIdAndUserId(id, userId).orElseGet(() -> {
            AnnouncementRead value = new AnnouncementRead(); value.setAnnouncement(ann);
            User user = new User(); user.setId(userId); value.setUser(user); return value;
        });
        read.setReadAt(LocalDateTime.now()); announcementReadRepository.save(read);
    }

    @Override @Transactional(readOnly = true)
    public long getUnreadCount(Long userId, UserRole role) {
        List<Long> ids = getVisibleClassIds(userId, role);
        return ids.isEmpty() ? 0 : announcementRepository.countUnread(ids, targetRolesFor(role), userId);
    }

    private Announcement base(String title, String body, TargetRole target, Long yearId, Long senderId) {
        Announcement a = new Announcement(); a.setTitle(title); a.setBody(body); a.setTargetRole(target);
        a.setAcademicYear(year(yearId)); a.setSender(userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", senderId))); return a;
    }
    private AcademicYear year(Long id) { return academicYearRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("AcademicYear", "id", id)); }
    private Long resolveYearId(Long requested, List<Long> classIds) {
        if (requested != null) return requested;
        SchoolClass first = schoolClassRepository.findById(classIds.getFirst())
                .orElseThrow(() -> new ResourceNotFoundException("Class", "id", classIds.getFirst()));
        return first.getAcademicYear().getId();
    }
    private Teacher teacherByUser(Long id) { return teacherRepository.findByUserId(id)
            .orElseThrow(() -> new ResourceNotFoundException("Teacher", "userId", id)); }
    private Announcement ownedByTeacher(Long id, Long userId) {
        Announcement a = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", "id", id));
        if (a.getTeacher() == null || !a.getSender().getId().equals(userId)) throw new ForbiddenException("Không có quyền sửa");
        return a;
    }
    private void validateTeacherClasses(Teacher t, Long yearId, List<Long> classIds) {
        Set<Long> allowed = new HashSet<>(teachingAssignmentRepository.findActiveClassIdsByTeacherAndYear(t.getId(), yearId));
        homeroomAssignmentRepository.findActiveByTeacherAndYear(t.getId(), yearId).forEach(h -> allowed.add(h.getCls().getId()));
        if (classIds.isEmpty() || !allowed.containsAll(classIds)) throw new ForbiddenException("Chỉ được gửi cho lớp được phân công");
        if (schoolClassRepository.findAllById(classIds).stream().anyMatch(c -> !c.getAcademicYear().getId().equals(yearId)))
            throw new ForbiddenException("Lớp không thuộc năm học đã chọn");
    }
    private void validateAdminClasses(Long yearId, List<Long> classIds) {
        if (classIds.isEmpty()) throw new IllegalArgumentException("Phải chọn ít nhất một lớp");
        List<SchoolClass> classes = schoolClassRepository.findAllById(classIds);
        if (classes.size() != new HashSet<>(classIds).size() || classes.stream().anyMatch(c -> !c.getAcademicYear().getId().equals(yearId)))
            throw new ForbiddenException("Lớp không thuộc năm học đã chọn");
    }
    private void collectClassRecipients(List<Long> classIds, TargetRole role, Set<Long> users) {
        for (Long classId : classIds) for (Student student : studentRepository.findByCurrentClassId(classId)) {
            if (role != TargetRole.PARENT && student.getUser() != null) users.add(student.getUser().getId());
            if (role != TargetRole.STUDENT) studentGuardianRepository.findByStudentId(student.getId()).stream()
                    .map(StudentGuardian::getGuardian).map(Parent::getUser).filter(Objects::nonNull)
                    .forEach(user -> users.add(user.getId()));
        }
    }
    private boolean isHomeroomTeacher(Long teacherId, Long yearId, List<Long> ids) {
        Set<Long> homeroom = homeroomAssignmentRepository.findActiveByTeacherAndYear(teacherId, yearId).stream()
                .map(h -> h.getCls().getId()).collect(Collectors.toSet());
        return homeroom.containsAll(ids);
    }
    private void replaceClasses(Announcement a, List<Long> ids) {
        announcementClassRepository.deleteAll(announcementClassRepository.findByAnnouncementId(a.getId()));
        for (Long id : ids) { AnnouncementClass ac = new AnnouncementClass(); ac.setAnnouncement(a);
            ac.setCls(schoolClassRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Class", "id", id)));
            announcementClassRepository.save(ac); }
    }
    private void notifyAdmins(Announcement a) { userRepository.findByRole(UserRole.ADMIN).forEach(u ->
            notificationService.createNotification(u.getId(), "Thông báo mới chờ phê duyệt",
                    a.getSender().getName() + ": " + a.getTitle(), "Chờ phê duyệt", a.getId(), "ANNOUNCEMENT_APPROVAL")); }
    private void publish(Announcement a) {
        Set<Long> users = new HashSet<>();
        for (AnnouncementClass ac : announcementClassRepository.findByAnnouncementId(a.getId())) {
            for (Student s : studentRepository.findByCurrentClassId(ac.getCls().getId())) {
                if (a.getTargetRole() != TargetRole.PARENT && s.getUser() != null) users.add(s.getUser().getId());
                if (a.getTargetRole() != TargetRole.STUDENT) studentGuardianRepository.findByStudentId(s.getId()).stream()
                        .map(StudentGuardian::getGuardian).map(Parent::getUser).filter(Objects::nonNull)
                        .forEach(u -> users.add(u.getId()));
            }
        }
        String tag = "HOMEROOM_TEACHER".equals(a.getSenderType()) ? "GVCN" : "GV bộ môn";
        users.forEach(id -> notificationService.createNotification(id, a.getTitle(), a.getBody(), tag, a.getId(), "ANNOUNCEMENT"));
        notificationService.createNotification(a.getSender().getId(), "Thông báo đã được duyệt", a.getTitle(), "Quản trị viên", a.getId(), "ANNOUNCEMENT");
    }
    private boolean canViewAnnouncement(Announcement a, Long userId, UserRole role) {
        if (role == UserRole.TEACHER) return a.getSender().getId().equals(userId);
        return "APPROVED".equals(a.getApprovalStatus()) && targetRolesFor(role).contains(a.getTargetRole()) &&
                a.getAnnouncementClasses().stream().anyMatch(ac -> getVisibleClassIds(userId, role).contains(ac.getCls().getId()));
    }
    private List<Long> getVisibleClassIds(Long userId, UserRole role) {
        if (role == UserRole.STUDENT) { Student s = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "userId", userId));
            return s.getCurrentClass() == null ? List.of() : List.of(s.getCurrentClass().getId()); }
        if (role == UserRole.PARENT) { Parent p = parentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent", "userId", userId));
            return studentGuardianRepository.findByGuardianId(p.getId()).stream().map(StudentGuardian::getStudent)
                    .filter(s -> s.getCurrentClass() != null).map(s -> s.getCurrentClass().getId()).distinct().toList(); }
        return List.of();
    }
    private List<TargetRole> targetRolesFor(UserRole role) {
        return role == UserRole.PARENT ? List.of(TargetRole.PARENT, TargetRole.ALL)
                : role == UserRole.STUDENT ? List.of(TargetRole.STUDENT, TargetRole.ALL) : List.of();
    }
    private AnnouncementDto toDto(Announcement a, boolean read) {
        List<String> names = announcementClassRepository.findByAnnouncementId(a.getId()).stream()
                .map(ac -> ac.getCls().getName()).distinct().toList();
        String recipientScope = "ADMIN".equals(a.getSenderType()) && names.isEmpty()
                && "CLASSES".equals(a.getRecipientScope()) ? "SCHOOL" : a.getRecipientScope();
        return new AnnouncementDto(a.getId(), a.getTitle(), a.getBody(), a.getTargetRole(), a.getRequiresReply(),
                a.getTeacher() == null ? null : a.getTeacher().getId(), a.getSender().getName(), names, read,
                0, (int) announcementReadRepository.countByAnnouncementIdAndReadAtIsNotNull(a.getId()), a.getCreatedAt(),
                a.getAcademicYear().getId(), a.getApprovalStatus(), a.getRejectionReason(), a.getSenderType(),
                recipientScope, a.getTeacherAudience(),
                a.getRecipientSubject() == null ? null : a.getRecipientSubject().getId(),
                a.getRecipientSubject() == null ? null : a.getRecipientSubject().getName());
    }
}
