package vn.edu.fpt.myfschool.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.AnnouncementDto;
import vn.edu.fpt.myfschool.common.dto.AnnouncementRecipientDto;
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
import java.time.LocalDate;
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
    private final EnrollmentRepository enrollmentRepository;
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
            boolean requiresReply, Long academicYearId, List<Long> classIds, Long userId) {
        Announcement ann = ownedByTeacher(id, userId);
        academicYearId = resolveYearId(academicYearId, classIds);
        if ("APPROVED".equals(ann.getApprovalStatus())) throw new ForbiddenException("Thông báo đã duyệt không thể sửa");
        validateTeacherClasses(ann.getTeacher(), academicYearId, classIds);
        ann.setTitle(title); ann.setBody(body); ann.setTargetRole(targetRole); ann.setRequiresReply(requiresReply);
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
            String teacherAudience, Long subjectId, boolean requiresReply, Long adminUserId) {
        year(academicYearId);
        String scope = recipientScope == null ? "SCHOOL" : recipientScope.toUpperCase(Locale.ROOT);
        if (!Set.of("SCHOOL", "CLASSES", "TEACHERS").contains(scope))
            throw new IllegalArgumentException("Phạm vi người nhận không hợp lệ");
        TargetRole role = targetRole == null ? TargetRole.ALL : targetRole;
        Announcement ann = base(title, body, role, academicYearId, adminUserId);
        ann.setApprovalStatus("APPROVED"); ann.setSenderType("ADMIN"); ann.setRequiresReply(requiresReply);
        ann.setRecipientScope(scope);
        Map<Long, RecipientSnapshot> recipients = new LinkedHashMap<>();

        if ("SCHOOL".equals(scope)) {
            collectYearTeacherRecipients(academicYearId, recipients);
            List<Long> yearClassIds = schoolClassRepository.findByAcademicYearId(academicYearId).stream()
                    .map(SchoolClass::getId).toList();
            collectClassRecipients(yearClassIds, TargetRole.ALL, academicYearId, recipients);
            recipients.remove(adminUserId);
        } else if ("CLASSES".equals(scope)) {
            List<Long> selectedClasses = classIds == null ? List.of() : classIds;
            validateAdminClasses(academicYearId, selectedClasses);
            ann = announcementRepository.save(ann);
            replaceClasses(ann, selectedClasses);
            collectClassRecipients(selectedClasses, role, academicYearId, recipients);
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
                        .filter(assignment -> assignment.getStatus() == AssignmentStatus.ACTIVE)
                        .filter(assignment -> assignment.getSubject().getId().equals(subjectId))
                        .map(TeachingAssignment::getTeacher).map(Teacher::getUser).filter(Objects::nonNull)
                        .forEach(user -> addRecipient(recipients, user, null, null));
            } else if ("HOMEROOM".equals(audience)) {
                homeroomAssignmentRepository.findAll().stream()
                        .filter(h -> h.getAcademicYear().getId().equals(academicYearId))
                        .filter(this::isActiveHomeroomAssignment)
                        .forEach(h -> addRecipient(recipients, h.getTeacher().getUser(), null, h.getCls()));
            } else {
                collectYearTeacherRecipients(academicYearId, recipients);
            }
        }
        if (ann.getId() == null) ann = announcementRepository.save(ann);
        saveRecipientSnapshot(ann, recipients);
        Long announcementId = ann.getId();
        recipients.keySet().forEach(id -> notificationService.createNotification(id, title, body,
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
    public List<AnnouncementDto> getMyAnnouncements(Long userId, Long academicYearId) {
        Teacher teacher = teacherByUser(userId);
        return announcementRepository.findByTeacherIdOrderByCreatedAtDesc(teacher.getId()).stream()
                .filter(a -> academicYearId == null || a.getAcademicYear().getId().equals(academicYearId))
                .map(a -> toDto(a, false)).toList();
    }

    @Override
    public AnnouncementDto getAnnouncementDetail(Long id, Long userId, UserRole role) {
        Announcement ann = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", "id", id));
        if (role != UserRole.ADMIN && !canViewAnnouncement(ann, userId, role))
            throw new ForbiddenException("Bạn không có quyền xem thông báo này");
        if (role == UserRole.PARENT || role == UserRole.STUDENT) {
            markAsRead(id, userId, role);
        }
        AnnouncementRead recipient = announcementReadRepository.findByAnnouncementIdAndUserId(id, userId).orElse(null);
        return toDto(ann, recipient);
    }

    @Override @Transactional(readOnly = true)
    public List<AnnouncementDto> getAnnouncements(Long userId, UserRole role) {
        requireRecipientRole(role);
        return announcementReadRepository.findByUserIdOrderByAnnouncementCreatedAtDesc(userId).stream()
                .filter(read -> "APPROVED".equals(read.getAnnouncement().getApprovalStatus()))
                .map(read -> toDto(read.getAnnouncement(), read))
                .toList();
    }

    @Override public void markAsRead(Long id, Long userId, UserRole role) {
        Announcement ann = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", "id", id));
        if (!canViewAnnouncement(ann, userId, role)) throw new ForbiddenException("Bạn không có quyền đọc thông báo này");
        AnnouncementRead read = requireRecipient(id, userId, role);
        if (read.getReadAt() == null) {
            read.setReadAt(LocalDateTime.now());
            announcementReadRepository.save(read);
        }
    }

    @Override @Transactional(readOnly = true)
    public long getUnreadCount(Long userId, UserRole role) {
        requireRecipientRole(role);
        return announcementReadRepository.countByUserIdAndReadAtIsNull(userId);
    }

    @Override
    public void acknowledge(Long id, Long userId, UserRole role) {
        AnnouncementRead recipient = requireRecipient(id, userId, role);
        if (!Boolean.TRUE.equals(recipient.getAnnouncement().getRequiresReply())) {
            throw new IllegalArgumentException("Thông báo này không yêu cầu xác nhận");
        }
        LocalDateTime now = LocalDateTime.now();
        if (recipient.getReadAt() == null) recipient.setReadAt(now);
        if (recipient.getAcknowledgedAt() == null) recipient.setAcknowledgedAt(now);
        announcementReadRepository.save(recipient);
    }

    @Override
    public void reply(Long id, String replyText, Long userId, UserRole role) {
        AnnouncementRead recipient = requireRecipient(id, userId, role);
        if (!Boolean.TRUE.equals(recipient.getAnnouncement().getRequiresReply())) {
            throw new IllegalArgumentException("Thông báo này không yêu cầu phản hồi");
        }
        String value = replyText == null ? "" : replyText.trim();
        if (value.isEmpty() || value.length() > 1000) {
            throw new IllegalArgumentException("Phản hồi phải có từ 1 đến 1.000 ký tự");
        }
        LocalDateTime now = LocalDateTime.now();
        if (recipient.getReadAt() == null) recipient.setReadAt(now);
        if (recipient.getAcknowledgedAt() == null) recipient.setAcknowledgedAt(now);
        recipient.setReplyText(value);
        recipient.setRepliedAt(now);
        announcementReadRepository.save(recipient);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AnnouncementRecipientDto> getRecipients(Long announcementId, Long academicYearId,
            Long classId, UserRole role, String status, String keyword, int page, int size,
            Long requesterId, UserRole requesterRole) {
        Announcement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", "id", announcementId));
        if (!announcement.getAcademicYear().getId().equals(academicYearId)) {
            throw new ForbiddenException("Thông báo không thuộc năm học đã chọn");
        }
        if (requesterRole == UserRole.TEACHER && !announcement.getSender().getId().equals(requesterId)) {
            throw new ForbiddenException("Giáo viên chỉ được xem người nhận thông báo do mình gửi");
        }
        if (requesterRole != UserRole.ADMIN && requesterRole != UserRole.TEACHER) {
            throw new ForbiddenException("Không có quyền xem danh sách người nhận");
        }
        if (classId != null) {
            SchoolClass selectedClass = schoolClassRepository.findById(classId)
                    .orElseThrow(() -> new ResourceNotFoundException("Class", "id", classId));
            if (!selectedClass.getAcademicYear().getId().equals(academicYearId)) {
                throw new ForbiddenException("Lớp không thuộc năm học đã chọn");
            }
        }
        String normalizedStatus = normalizeRecipientStatus(status);
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        List<AnnouncementRecipientDto> filtered = announcementReadRepository.findByAnnouncementId(announcementId).stream()
                .filter(item -> role == null || item.getRecipientRole() == role)
                .filter(item -> classId == null || splitLongs(item.getClassIds()).contains(classId))
                .filter(item -> normalizedKeyword.isEmpty() || recipientSearchText(item).contains(normalizedKeyword))
                .filter(item -> normalizedStatus == null || matchesStatus(item, normalizedStatus))
                .map(this::toRecipientDto)
                .toList();
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 100));
        int start = Math.min(safePage * safeSize, filtered.size());
        int end = Math.min(start + safeSize, filtered.size());
        return new PageImpl<>(filtered.subList(start, end), PageRequest.of(safePage, safeSize), filtered.size());
    }

    @Override
    @Transactional(readOnly = true)
    public long getPendingActionCount(Long userId, UserRole role) {
        requireRecipientRole(role);
        return announcementReadRepository.countPendingActionByUserId(userId);
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
    private void collectClassRecipients(List<Long> classIds, TargetRole role, Long academicYearId,
            Map<Long, RecipientSnapshot> recipients) {
        for (Long classId : classIds) {
            SchoolClass schoolClass = schoolClassRepository.findById(classId)
                    .orElseThrow(() -> new ResourceNotFoundException("Class", "id", classId));
            if (!schoolClass.getAcademicYear().getId().equals(academicYearId)) {
                throw new ForbiddenException("Lớp không thuộc năm học đã chọn");
            }
            for (Student student : enrollmentRepository.findActiveStudentsByClassAndYear(classId, academicYearId)) {
                if (role != TargetRole.PARENT && student.getUser() != null) {
                    addRecipient(recipients, student.getUser(), student, schoolClass);
                }
                if (role != TargetRole.STUDENT) {
                    studentGuardianRepository.findByStudentId(student.getId()).stream()
                            .map(StudentGuardian::getGuardian)
                            .map(Parent::getUser)
                            .filter(Objects::nonNull)
                            .forEach(user -> addRecipient(recipients, user, student, schoolClass));
                }
            }
        }
    }

    private void collectYearTeacherRecipients(Long academicYearId, Map<Long, RecipientSnapshot> recipients) {
        teachingAssignmentRepository.findByAcademicYearId(academicYearId).stream()
                .filter(assignment -> assignment.getStatus() == AssignmentStatus.ACTIVE)
                .forEach(assignment -> addRecipient(recipients, assignment.getTeacher().getUser(), null,
                        assignment.getCls()));
        homeroomAssignmentRepository.findAll().stream()
                .filter(assignment -> assignment.getAcademicYear().getId().equals(academicYearId))
                .filter(this::isActiveHomeroomAssignment)
                .forEach(assignment -> addRecipient(recipients, assignment.getTeacher().getUser(), null,
                        assignment.getCls()));
    }

    private void addRecipient(Map<Long, RecipientSnapshot> recipients, User user, Student student,
            SchoolClass schoolClass) {
        if (user == null) return;
        RecipientSnapshot snapshot = recipients.computeIfAbsent(user.getId(), ignored -> new RecipientSnapshot(user));
        if (student != null) snapshot.studentNames.add(student.getUser().getName());
        if (schoolClass != null) {
            snapshot.classIds.add(schoolClass.getId());
            snapshot.classNames.add(schoolClass.getName());
        }
    }

    private boolean isActiveHomeroomAssignment(HomeroomAssignment assignment) {
        LocalDate today = LocalDate.now();
        return !assignment.getEffectiveFrom().isAfter(today)
                && (assignment.getEffectiveTo() == null || !assignment.getEffectiveTo().isBefore(today));
    }

    private void saveRecipientSnapshot(Announcement announcement, Map<Long, RecipientSnapshot> recipients) {
        for (RecipientSnapshot snapshot : recipients.values()) {
            if (announcementReadRepository.existsByAnnouncementIdAndUserId(announcement.getId(), snapshot.user.getId())) {
                continue;
            }
            AnnouncementRead read = new AnnouncementRead();
            read.setAnnouncement(announcement);
            read.setUser(snapshot.user);
            read.setRecipientRole(snapshot.user.getRole());
            read.setUserName(snapshot.user.getName());
            read.setStudentNames(String.join("\n", snapshot.studentNames));
            read.setClassNames(String.join("\n", snapshot.classNames));
            read.setClassIds(snapshot.classIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
            announcementReadRepository.save(read);
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
        Map<Long, RecipientSnapshot> recipients = new LinkedHashMap<>();
        List<Long> classIds = announcementClassRepository.findByAnnouncementId(a.getId()).stream()
                .map(ac -> ac.getCls().getId()).toList();
        collectClassRecipients(classIds, a.getTargetRole(), a.getAcademicYear().getId(), recipients);
        saveRecipientSnapshot(a, recipients);
        String tag = "HOMEROOM_TEACHER".equals(a.getSenderType()) ? "GVCN" : "GV bộ môn";
        recipients.keySet().forEach(id -> notificationService.createNotification(id, a.getTitle(), a.getBody(), tag,
                a.getId(), "ANNOUNCEMENT"));
        notificationService.createNotification(a.getSender().getId(), "Thông báo đã được duyệt", a.getTitle(), "Quản trị viên", a.getId(), "ANNOUNCEMENT");
    }
    private boolean canViewAnnouncement(Announcement a, Long userId, UserRole role) {
        if (role == UserRole.TEACHER && a.getSender().getId().equals(userId)) return true;
        return "APPROVED".equals(a.getApprovalStatus())
                && announcementReadRepository.existsByAnnouncementIdAndUserId(a.getId(), userId);
    }
    private AnnouncementDto toDto(Announcement announcement, boolean ignoredReadFlag) {
        return toDto(announcement, (AnnouncementRead) null);
    }

    private AnnouncementDto toDto(Announcement a, AnnouncementRead recipient) {
        List<String> names = announcementClassRepository.findByAnnouncementId(a.getId()).stream()
                .map(ac -> ac.getCls().getName()).distinct().toList();
        List<Long> classIds = announcementClassRepository.findByAnnouncementId(a.getId()).stream()
                .map(ac -> ac.getCls().getId()).distinct().toList();
        String recipientScope = "ADMIN".equals(a.getSenderType()) && names.isEmpty()
                && "CLASSES".equals(a.getRecipientScope()) ? "SCHOOL" : a.getRecipientScope();
        int totalRecipients = (int) announcementReadRepository.countByAnnouncementId(a.getId());
        int readCount = (int) announcementReadRepository.countByAnnouncementIdAndReadAtIsNotNull(a.getId());
        int acknowledgedCount = (int) announcementReadRepository.countByAnnouncementIdAndAcknowledgedAtIsNotNull(a.getId());
        int repliedCount = (int) announcementReadRepository.countByAnnouncementIdAndRepliedAtIsNotNull(a.getId());
        return new AnnouncementDto(a.getId(), a.getTitle(), a.getBody(), a.getTargetRole(), a.getRequiresReply(),
                a.getTeacher() == null ? null : a.getTeacher().getId(), a.getSender().getName(), names, classIds,
                recipient != null && recipient.getReadAt() != null,
                recipient != null && recipient.getAcknowledgedAt() != null,
                recipient == null ? null : recipient.getReplyText(),
                recipient == null ? null : recipient.getRepliedAt(),
                recipient == null ? null : recipientStatus(recipient),
                totalRecipients, readCount, acknowledgedCount, repliedCount, a.getCreatedAt(),
                a.getAcademicYear().getId(), a.getApprovalStatus(), a.getRejectionReason(), a.getSenderType(),
                recipientScope, a.getTeacherAudience(),
                a.getRecipientSubject() == null ? null : a.getRecipientSubject().getId(),
                a.getRecipientSubject() == null ? null : a.getRecipientSubject().getName());
    }

    private AnnouncementRead requireRecipient(Long announcementId, Long userId, UserRole role) {
        requireRecipientRole(role);
        AnnouncementRead recipient = announcementReadRepository.findByAnnouncementIdAndUserId(announcementId, userId)
                .orElseThrow(() -> new ForbiddenException("Bạn không thuộc danh sách người nhận thông báo"));
        if (!"APPROVED".equals(recipient.getAnnouncement().getApprovalStatus())) {
            throw new ForbiddenException("Thông báo chưa được phê duyệt");
        }
        return recipient;
    }

    private void requireRecipientRole(UserRole role) {
        if (role != UserRole.PARENT && role != UserRole.STUDENT) {
            throw new ForbiddenException("Chức năng này chỉ dành cho phụ huynh và học sinh");
        }
    }

    private String normalizeRecipientStatus(String status) {
        if (status == null || status.isBlank()) return null;
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("UNREAD", "READ", "ACKNOWLEDGED", "REPLIED", "PENDING").contains(normalized)) {
            throw new IllegalArgumentException("Trạng thái người nhận không hợp lệ");
        }
        return normalized;
    }

    private boolean matchesStatus(AnnouncementRead recipient, String status) {
        if ("PENDING".equals(status)) {
            return Boolean.TRUE.equals(recipient.getAnnouncement().getRequiresReply())
                    && (recipient.getRecipientRole() == UserRole.PARENT || recipient.getRecipientRole() == UserRole.STUDENT)
                    && recipient.getAcknowledgedAt() == null && recipient.getRepliedAt() == null;
        }
        return recipientStatus(recipient).equals(status);
    }

    private String recipientStatus(AnnouncementRead recipient) {
        if (recipient.getRepliedAt() != null) return "REPLIED";
        if (recipient.getAcknowledgedAt() != null) return "ACKNOWLEDGED";
        if (recipient.getReadAt() != null) return "READ";
        return "UNREAD";
    }

    private AnnouncementRecipientDto toRecipientDto(AnnouncementRead recipient) {
        return new AnnouncementRecipientDto(recipient.getUser().getId(), recipient.getUserName(),
                recipient.getRecipientRole(), splitText(recipient.getStudentNames()), splitText(recipient.getClassNames()),
                recipient.getReadAt(), recipient.getAcknowledgedAt(), recipient.getReplyText(), recipient.getRepliedAt(),
                recipientStatus(recipient));
    }

    private String recipientSearchText(AnnouncementRead recipient) {
        return String.join(" ", Objects.toString(recipient.getUserName(), ""),
                Objects.toString(recipient.getStudentNames(), ""), Objects.toString(recipient.getClassNames(), ""))
                .toLowerCase(Locale.ROOT);
    }

    private List<String> splitText(String value) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split("\\n")).map(String::trim).filter(item -> !item.isEmpty()).toList();
    }

    private Set<Long> splitLongs(String value) {
        if (value == null || value.isBlank()) return Set.of();
        return Arrays.stream(value.split(",")).map(String::trim).filter(item -> !item.isEmpty())
                .map(Long::valueOf).collect(Collectors.toSet());
    }

    private static final class RecipientSnapshot {
        private final User user;
        private final Set<String> studentNames = new LinkedHashSet<>();
        private final Set<String> classNames = new LinkedHashSet<>();
        private final Set<Long> classIds = new LinkedHashSet<>();

        private RecipientSnapshot(User user) {
            this.user = user;
        }
    }
}
