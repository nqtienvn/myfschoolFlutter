package vn.edu.fpt.myfschool.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.AnnouncementDto;
import vn.edu.fpt.myfschool.common.dto.AnnouncementPolicyDto;
import vn.edu.fpt.myfschool.common.dto.AnnouncementPolicyRuleDto;
import vn.edu.fpt.myfschool.common.dto.AnnouncementPolicyRuleRequest;
import vn.edu.fpt.myfschool.common.dto.AnnouncementPolicyUpdateRequest;
import vn.edu.fpt.myfschool.common.dto.AnnouncementRecipientDto;
import vn.edu.fpt.myfschool.common.dto.AnnouncementSubmissionResultDto;
import vn.edu.fpt.myfschool.common.dto.AnnouncementSummaryDto;
import vn.edu.fpt.myfschool.common.dto.AnnouncementViolationDto;
import vn.edu.fpt.myfschool.common.enums.AnnouncementDeliveryStatus;
import vn.edu.fpt.myfschool.common.enums.AnnouncementPolicyMatchType;
import vn.edu.fpt.myfschool.common.enums.AnnouncementPolicyScope;
import vn.edu.fpt.myfschool.common.enums.TargetRole;
import vn.edu.fpt.myfschool.common.enums.UserRole;
import vn.edu.fpt.myfschool.common.exception.BadRequestException;
import vn.edu.fpt.myfschool.common.exception.ForbiddenException;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.entity.AcademicYear;
import vn.edu.fpt.myfschool.entity.Announcement;
import vn.edu.fpt.myfschool.entity.AnnouncementClass;
import vn.edu.fpt.myfschool.entity.AnnouncementContentRule;
import vn.edu.fpt.myfschool.entity.AnnouncementPolicySetting;
import vn.edu.fpt.myfschool.entity.AnnouncementPolicyViolation;
import vn.edu.fpt.myfschool.entity.AnnouncementRead;
import vn.edu.fpt.myfschool.entity.Parent;
import vn.edu.fpt.myfschool.entity.SchoolClass;
import vn.edu.fpt.myfschool.entity.Student;
import vn.edu.fpt.myfschool.entity.StudentGuardian;
import vn.edu.fpt.myfschool.entity.Teacher;
import vn.edu.fpt.myfschool.entity.User;
import vn.edu.fpt.myfschool.repository.AcademicYearRepository;
import vn.edu.fpt.myfschool.repository.AnnouncementClassRepository;
import vn.edu.fpt.myfschool.repository.AnnouncementContentRuleRepository;
import vn.edu.fpt.myfschool.repository.AnnouncementPolicySettingRepository;
import vn.edu.fpt.myfschool.repository.AnnouncementPolicyViolationRepository;
import vn.edu.fpt.myfschool.repository.AnnouncementReadRepository;
import vn.edu.fpt.myfschool.repository.AnnouncementRepository;
import vn.edu.fpt.myfschool.repository.ClassRepository;
import vn.edu.fpt.myfschool.repository.EnrollmentRepository;
import vn.edu.fpt.myfschool.repository.HomeroomAssignmentRepository;
import vn.edu.fpt.myfschool.repository.StudentGuardianRepository;
import vn.edu.fpt.myfschool.repository.TeacherRepository;
import vn.edu.fpt.myfschool.repository.TeachingAssignmentRepository;
import vn.edu.fpt.myfschool.repository.UserRepository;
import vn.edu.fpt.myfschool.service.AnnouncementService;
import vn.edu.fpt.myfschool.service.NotificationService;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service("announcementService")
@RequiredArgsConstructor
@Transactional
public class AnnouncementServiceImpl implements AnnouncementService {

    static final String DEFAULT_REJECTION_MESSAGE =
            "Thông báo này đã vi phạm câu từ trong chính sách của nhà trường.";

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementClassRepository announcementClassRepository;
    private final AnnouncementReadRepository announcementReadRepository;
    private final AnnouncementPolicySettingRepository policySettingRepository;
    private final AnnouncementContentRuleRepository contentRuleRepository;
    private final AnnouncementPolicyViolationRepository policyViolationRepository;
    private final TeacherRepository teacherRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentGuardianRepository studentGuardianRepository;
    private final TeachingAssignmentRepository teachingAssignmentRepository;
    private final HomeroomAssignmentRepository homeroomAssignmentRepository;
    private final ClassRepository schoolClassRepository;
    private final AcademicYearRepository academicYearRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Override
    public AnnouncementSubmissionResultDto createAnnouncement(String title, String body,
            TargetRole targetRole, Long academicYearId, List<Long> classIds,
            Long retryOfAnnouncementId, Long teacherUserId) {
        Teacher teacher = teacherByUser(teacherUserId);
        Long resolvedYearId = resolveYearId(academicYearId, classIds);
        validateTeacherClasses(teacher, resolvedYearId, classIds);

        Announcement retryOf = resolveRetry(retryOfAnnouncementId, teacherUserId, resolvedYearId);
        PolicyEvaluation policy = evaluatePolicy(resolvedYearId, title, body);

        Announcement announcement = base(title.trim(), body.trim(), targetRole,
                resolvedYearId, teacherUserId);
        announcement.setTeacher(teacher);
        announcement.setRecipientScope("CLASSES");
        announcement.setSenderType(isHomeroomTeacher(teacher.getId(), resolvedYearId, classIds)
                ? "HOMEROOM_TEACHER" : "SUBJECT_TEACHER");
        announcement.setRetryOfAnnouncement(retryOf);
        announcement.setDeliveryStatus(policy.matches().isEmpty()
                ? AnnouncementDeliveryStatus.PUBLISHED
                : AnnouncementDeliveryStatus.SYSTEM_REJECTED);
        announcement.setSystemRejectionMessage(policy.matches().isEmpty() ? null : policy.rejectionMessage());
        announcement = announcementRepository.save(announcement);
        replaceClasses(announcement, classIds);

        if (policy.matches().isEmpty()) {
            publish(announcement);
        } else {
            saveViolations(announcement, policy.matches());
        }

        AnnouncementDto dto = toDto(announcement, (AnnouncementRead) null);
        String message = announcement.getDeliveryStatus() == AnnouncementDeliveryStatus.PUBLISHED
                ? "Thông báo đã được gửi thành công."
                : announcement.getSystemRejectionMessage();
        return new AnnouncementSubmissionResultDto(announcement.getDeliveryStatus(), message,
                dto, dto.violations());
    }

    @Override
    public void deleteAnnouncement(Long id, Long userId, UserRole role) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", "id", id));
        if (role != UserRole.ADMIN && !announcement.getSender().getId().equals(userId)) {
            throw new ForbiddenException("Bạn không có quyền xóa thông báo này");
        }
        notificationService.deleteByReference(announcement.getId(), "ANNOUNCEMENT");
        announcementRepository.delete(announcement);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AnnouncementDto> getAdminAnnouncements(Long academicYearId,
            AnnouncementDeliveryStatus status, String keyword, int page, int size) {
        year(academicYearId);
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 100));
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        PageRequest pageable = PageRequest.of(safePage, safeSize,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        Page<Announcement> result = announcementRepository.searchAdminAnnouncements(
                academicYearId, status, normalizedKeyword, pageable);
        List<Long> announcementIds = result.getContent().stream().map(Announcement::getId).toList();
        if (announcementIds.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, result.getTotalElements());
        }
        Map<Long, List<AnnouncementClass>> classesByAnnouncement = announcementClassRepository
                .findByAnnouncementIdIn(announcementIds).stream()
                .collect(Collectors.groupingBy(link -> link.getAnnouncement().getId()));
        Map<Long, List<AnnouncementPolicyViolation>> violationsByAnnouncement = policyViolationRepository
                .findByAnnouncementIdInOrderById(announcementIds).stream()
                .collect(Collectors.groupingBy(violation -> violation.getAnnouncement().getId(),
                        LinkedHashMap::new, Collectors.toList()));
        List<AnnouncementDto> content = result.getContent().stream()
                .map(announcement -> toDto(announcement, null,
                        classesByAnnouncement.getOrDefault(announcement.getId(), List.of()),
                        violationsByAnnouncement.getOrDefault(announcement.getId(), List.of())))
                .toList();
        return new PageImpl<>(content, pageable, result.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public AnnouncementSummaryDto getAdminSummary(Long academicYearId) {
        year(academicYearId);
        return new AnnouncementSummaryDto(
                announcementRepository.countByAcademicYearId(academicYearId),
                announcementRepository.countByAcademicYearIdAndDeliveryStatus(
                        academicYearId, AnnouncementDeliveryStatus.PUBLISHED),
                announcementRepository.countByAcademicYearIdAndDeliveryStatus(
                        academicYearId, AnnouncementDeliveryStatus.SYSTEM_REJECTED));
    }

    @Override
    @Transactional(readOnly = true)
    public AnnouncementPolicyDto getPolicy(Long academicYearId) {
        year(academicYearId);
        AnnouncementPolicySetting setting = policySettingRepository
                .findByAcademicYearId(academicYearId).orElse(null);
        List<AnnouncementPolicyRuleDto> rules = contentRuleRepository
                .findByAcademicYearIdOrderById(academicYearId).stream()
                .map(this::toPolicyRuleDto).toList();
        return new AnnouncementPolicyDto(academicYearId,
                setting == null || setting.isEnabled(),
                setting == null ? DEFAULT_REJECTION_MESSAGE : setting.getRejectionMessage(),
                rules, setting == null ? null : setting.getUpdatedAt());
    }

    @Override
    public AnnouncementPolicyDto updatePolicy(AnnouncementPolicyUpdateRequest request,
            Long adminUserId) {
        AcademicYear academicYear = year(request.academicYearId());
        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", adminUserId));
        if (admin.getRole() != UserRole.ADMIN) {
            throw new ForbiddenException("Chỉ quản trị viên được cấu hình chính sách thông báo");
        }

        List<PreparedRule> preparedRules = prepareRules(request.rules());
        AnnouncementPolicySetting setting = policySettingRepository
                .findByAcademicYearId(request.academicYearId())
                .orElseGet(AnnouncementPolicySetting::new);
        setting.setAcademicYear(academicYear);
        setting.setEnabled(request.enabled());
        setting.setRejectionMessage(request.rejectionMessage().trim());
        setting.setUpdatedBy(admin);
        policySettingRepository.save(setting);

        List<AnnouncementPolicyViolation> historicalViolations = policyViolationRepository
                .findWithRuleByAcademicYearId(request.academicYearId());
        historicalViolations.forEach(violation -> violation.setRule(null));
        policyViolationRepository.saveAllAndFlush(historicalViolations);
        contentRuleRepository.deleteByAcademicYearId(request.academicYearId());
        contentRuleRepository.flush();

        for (PreparedRule prepared : preparedRules) {
            AnnouncementContentRule rule = new AnnouncementContentRule();
            rule.setAcademicYear(academicYear);
            rule.setPhrase(prepared.phrase());
            rule.setNormalizedPhrase(prepared.normalizedPhrase());
            rule.setScope(prepared.scope());
            rule.setMatchType(prepared.matchType());
            rule.setUpdatedBy(admin);
            contentRuleRepository.save(rule);
        }
        return getPolicy(request.academicYearId());
    }

    @Override
    public AnnouncementDto createAdminAnnouncement(String title, String body,
            Long academicYearId, Long adminUserId) {
        year(academicYearId);
        Announcement announcement = base(title.trim(), body.trim(), TargetRole.ALL,
                academicYearId, adminUserId);
        announcement.setDeliveryStatus(AnnouncementDeliveryStatus.PUBLISHED);
        announcement.setSenderType("ADMIN");
        announcement.setRecipientScope("SCHOOL");
        announcement = announcementRepository.save(announcement);

        Map<Long, RecipientSnapshot> recipients = new LinkedHashMap<>();
        userRepository.findAll().stream()
                .filter(user -> user.getRole() != UserRole.ADMIN)
                .forEach(user -> addRecipient(recipients, user, null, null));
        saveRecipientSnapshot(announcement, recipients);
        Long announcementId = announcement.getId();
        recipients.keySet().forEach(id -> notificationService.createNotification(id,
                title, body, "Nhà trường", announcementId, "ANNOUNCEMENT"));
        return toDto(announcement, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getEligibleClasses(Long academicYearId, Long teacherUserId) {
        year(academicYearId);
        Teacher teacher = teacherByUser(teacherUserId);
        Set<Long> ids = new LinkedHashSet<>(teachingAssignmentRepository
                .findActiveClassIdsByTeacherAndYear(teacher.getId(), academicYearId));
        homeroomAssignmentRepository.findActiveByTeacherAndYear(teacher.getId(), academicYearId)
                .forEach(h -> ids.add(h.getCls().getId()));
        return schoolClassRepository.findAllById(ids).stream()
                .map(c -> Map.<String, Object>of("id", c.getId(), "name", c.getName(),
                        "isHomeroom", homeroomAssignmentRepository
                                .existsByTeacherIdAndClsIdAndAcademicYearId(
                                        teacher.getId(), c.getId(), academicYearId)))
                .sorted(Comparator.comparing(m -> (String) m.get("name"))).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnnouncementDto> getMyAnnouncements(Long userId, Long academicYearId) {
        Teacher teacher = teacherByUser(userId);
        return announcementRepository.findByTeacherIdOrderByCreatedAtDesc(teacher.getId()).stream()
                .filter(a -> academicYearId == null
                        || a.getAcademicYear().getId().equals(academicYearId))
                .map(a -> toDto(a, false)).toList();
    }

    @Override
    public AnnouncementDto getAnnouncementDetail(Long id, Long userId, UserRole role) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", "id", id));
        if (role != UserRole.ADMIN && !canViewAnnouncement(announcement, userId, role)) {
            throw new ForbiddenException("Bạn không có quyền xem thông báo này");
        }
        if (role == UserRole.PARENT || role == UserRole.STUDENT
                || (role == UserRole.TEACHER && announcementReadRepository
                        .existsByAnnouncementIdAndUserId(id, userId))) {
            markAsRead(id, userId, role);
        }
        AnnouncementRead recipient = announcementReadRepository
                .findByAnnouncementIdAndUserId(id, userId).orElse(null);
        return toDto(announcement, recipient);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnnouncementDto> getAnnouncements(Long userId, UserRole role,
            Long academicYearId) {
        requireRecipientRole(role);
        if (role == UserRole.TEACHER && academicYearId == null) {
            throw new BadRequestException("Giáo viên phải chọn năm học khi xem thông báo");
        }
        return announcementReadRepository.findByUserIdOrderByAnnouncementCreatedAtDesc(userId).stream()
                .filter(read -> read.getAnnouncement().getDeliveryStatus()
                        == AnnouncementDeliveryStatus.PUBLISHED)
                .filter(read -> academicYearId == null
                        || read.getAnnouncement().getAcademicYear().getId().equals(academicYearId))
                .map(read -> toDto(read.getAnnouncement(), read)).toList();
    }

    @Override
    public void markAsRead(Long id, Long userId, UserRole role) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", "id", id));
        if (!canViewAnnouncement(announcement, userId, role)) {
            throw new ForbiddenException("Bạn không có quyền đọc thông báo này");
        }
        AnnouncementRead read = requireRecipient(id, userId, role);
        if (read.getReadAt() == null) {
            read.setReadAt(LocalDateTime.now());
            announcementReadRepository.save(read);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId, UserRole role, Long academicYearId) {
        requireRecipientRole(role);
        if (role == UserRole.TEACHER && academicYearId == null) {
            throw new BadRequestException("Giáo viên phải chọn năm học khi đếm thông báo chưa đọc");
        }
        return announcementReadRepository.findByUserIdOrderByAnnouncementCreatedAtDesc(userId).stream()
                .filter(read -> read.getAnnouncement().getDeliveryStatus()
                        == AnnouncementDeliveryStatus.PUBLISHED)
                .filter(read -> read.getReadAt() == null)
                .filter(read -> academicYearId == null
                        || read.getAnnouncement().getAcademicYear().getId().equals(academicYearId))
                .count();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AnnouncementRecipientDto> getRecipients(Long announcementId,
            Long academicYearId, Long classId, UserRole role, String status,
            String keyword, int page, int size, Long requesterId, UserRole requesterRole) {
        Announcement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", "id", announcementId));
        if (!announcement.getAcademicYear().getId().equals(academicYearId)) {
            throw new ForbiddenException("Thông báo không thuộc năm học đã chọn");
        }
        if (requesterRole != UserRole.TEACHER
                || !announcement.getSender().getId().equals(requesterId)) {
            throw new ForbiddenException("Giáo viên chỉ được xem người nhận thông báo do mình gửi");
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
        List<AnnouncementRecipientDto> filtered = announcementReadRepository
                .findByAnnouncementId(announcementId).stream()
                .filter(item -> role == null || item.getRecipientRole() == role)
                .filter(item -> classId == null || splitLongs(item.getClassIds()).contains(classId))
                .filter(item -> normalizedKeyword.isEmpty()
                        || recipientSearchText(item).contains(normalizedKeyword))
                .filter(item -> normalizedStatus == null || matchesStatus(item, normalizedStatus))
                .map(this::toRecipientDto).toList();
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 100));
        int start = Math.min(safePage * safeSize, filtered.size());
        int end = Math.min(start + safeSize, filtered.size());
        return new PageImpl<>(filtered.subList(start, end),
                PageRequest.of(safePage, safeSize), filtered.size());
    }

    static String normalizePolicyText(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC)
                .replaceAll("\\p{Cf}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[\\p{P}\\p{S}\\p{Z}\\s]+", " ")
                .trim();
        return normalized.replaceAll("\\s+", " ");
    }

    private PolicyEvaluation evaluatePolicy(Long academicYearId, String title, String body) {
        AnnouncementPolicySetting setting = policySettingRepository
                .findByAcademicYearId(academicYearId).orElse(null);
        if (setting != null && !setting.isEnabled()) {
            return new PolicyEvaluation(setting.getRejectionMessage(), List.of());
        }
        String normalizedTitle = normalizePolicyText(title);
        String normalizedBody = normalizePolicyText(body);
        List<PolicyMatch> matches = new ArrayList<>();
        for (AnnouncementContentRule rule : contentRuleRepository
                .findByAcademicYearIdOrderById(academicYearId)) {
            if (rule.getScope() == AnnouncementPolicyScope.TITLE
                    || rule.getScope() == AnnouncementPolicyScope.ALL) {
                if (matches(rule, normalizedTitle)) {
                    matches.add(new PolicyMatch(rule, "TITLE"));
                }
            }
            if (rule.getScope() == AnnouncementPolicyScope.BODY
                    || rule.getScope() == AnnouncementPolicyScope.ALL) {
                if (matches(rule, normalizedBody)) {
                    matches.add(new PolicyMatch(rule, "BODY"));
                }
            }
        }
        return new PolicyEvaluation(setting == null
                ? DEFAULT_REJECTION_MESSAGE : setting.getRejectionMessage(), matches);
    }

    private boolean matches(AnnouncementContentRule rule, String content) {
        if (rule.getMatchType() == AnnouncementPolicyMatchType.EXACT) {
            return content.equals(rule.getNormalizedPhrase());
        }
        return (" " + content + " ").contains(" " + rule.getNormalizedPhrase() + " ");
    }

    private List<PreparedRule> prepareRules(List<AnnouncementPolicyRuleRequest> requests) {
        Set<String> uniqueKeys = new HashSet<>();
        List<PreparedRule> prepared = new ArrayList<>();
        for (AnnouncementPolicyRuleRequest request : requests) {
            String phrase = request.phrase().trim();
            String normalizedPhrase = normalizePolicyText(phrase);
            if (normalizedPhrase.isBlank()) {
                throw new BadRequestException("Câu từ không hợp lệ không được để trống");
            }
            if (normalizedPhrase.length() > 250) {
                throw new BadRequestException("Câu từ không hợp lệ không được vượt quá 250 ký tự sau khi chuẩn hóa");
            }
            String key = normalizedPhrase + "|" + request.scope() + "|" + request.matchType();
            if (!uniqueKeys.add(key)) {
                throw new BadRequestException("Cấu hình đang có câu từ trùng nhau: " + phrase);
            }
            prepared.add(new PreparedRule(phrase, normalizedPhrase,
                    request.scope(), request.matchType()));
        }
        return prepared;
    }

    private Announcement resolveRetry(Long retryId, Long teacherUserId, Long academicYearId) {
        if (retryId == null) {
            return null;
        }
        Announcement previous = announcementRepository.findById(retryId)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", "id", retryId));
        if (!previous.getSender().getId().equals(teacherUserId)
                || previous.getDeliveryStatus() != AnnouncementDeliveryStatus.SYSTEM_REJECTED
                || !previous.getAcademicYear().getId().equals(academicYearId)) {
            throw new ForbiddenException("Chỉ được gửi lại thông báo bị hệ thống từ chối của chính bạn trong cùng năm học");
        }
        return previous;
    }

    private void saveViolations(Announcement announcement, List<PolicyMatch> matches) {
        for (PolicyMatch match : matches) {
            AnnouncementPolicyViolation violation = new AnnouncementPolicyViolation();
            violation.setAnnouncement(announcement);
            violation.setRule(match.rule());
            violation.setMatchedField(match.field());
            violation.setMatchedPhrase(match.rule().getPhrase());
            policyViolationRepository.save(violation);
        }
    }

    private Announcement base(String title, String body, TargetRole target,
            Long yearId, Long senderId) {
        Announcement announcement = new Announcement();
        announcement.setTitle(title);
        announcement.setBody(body);
        announcement.setTargetRole(target);
        announcement.setAcademicYear(year(yearId));
        announcement.setSender(userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", senderId)));
        return announcement;
    }

    private AcademicYear year(Long id) {
        return academicYearRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AcademicYear", "id", id));
    }

    private Long resolveYearId(Long requested, List<Long> classIds) {
        if (requested != null) {
            return requested;
        }
        SchoolClass first = schoolClassRepository.findById(classIds.getFirst())
                .orElseThrow(() -> new ResourceNotFoundException("Class", "id", classIds.getFirst()));
        return first.getAcademicYear().getId();
    }

    private Teacher teacherByUser(Long id) {
        return teacherRepository.findByUserId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", "userId", id));
    }

    private void validateTeacherClasses(Teacher teacher, Long yearId, List<Long> classIds) {
        year(yearId);
        Set<Long> allowed = new HashSet<>(teachingAssignmentRepository
                .findActiveClassIdsByTeacherAndYear(teacher.getId(), yearId));
        homeroomAssignmentRepository.findActiveByTeacherAndYear(teacher.getId(), yearId)
                .forEach(h -> allowed.add(h.getCls().getId()));
        if (classIds.isEmpty() || !allowed.containsAll(classIds)) {
            throw new ForbiddenException("Chỉ được gửi cho lớp được phân công");
        }
        if (schoolClassRepository.findAllById(classIds).size() != new LinkedHashSet<>(classIds).size()
                || schoolClassRepository.findAllById(classIds).stream()
                        .anyMatch(c -> !c.getAcademicYear().getId().equals(yearId))) {
            throw new ForbiddenException("Lớp không thuộc năm học đã chọn");
        }
    }

    private void collectClassRecipients(List<Long> classIds, TargetRole role,
            Long academicYearId, Map<Long, RecipientSnapshot> recipients) {
        for (Long classId : classIds) {
            SchoolClass schoolClass = schoolClassRepository.findById(classId)
                    .orElseThrow(() -> new ResourceNotFoundException("Class", "id", classId));
            if (!schoolClass.getAcademicYear().getId().equals(academicYearId)) {
                throw new ForbiddenException("Lớp không thuộc năm học đã chọn");
            }
            for (Student student : enrollmentRepository
                    .findActiveStudentsByClassAndYear(classId, academicYearId)) {
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

    private void addRecipient(Map<Long, RecipientSnapshot> recipients, User user,
            Student student, SchoolClass schoolClass) {
        if (user == null) {
            return;
        }
        RecipientSnapshot snapshot = recipients.computeIfAbsent(user.getId(),
                ignored -> new RecipientSnapshot(user));
        if (student != null) {
            snapshot.studentNames.add(student.getUser().getName());
        }
        if (schoolClass != null) {
            snapshot.classIds.add(schoolClass.getId());
            snapshot.classNames.add(schoolClass.getName());
        }
    }

    private void saveRecipientSnapshot(Announcement announcement,
            Map<Long, RecipientSnapshot> recipients) {
        for (RecipientSnapshot snapshot : recipients.values()) {
            if (announcementReadRepository.existsByAnnouncementIdAndUserId(
                    announcement.getId(), snapshot.user.getId())) {
                continue;
            }
            AnnouncementRead read = new AnnouncementRead();
            read.setAnnouncement(announcement);
            read.setUser(snapshot.user);
            read.setRecipientRole(snapshot.user.getRole());
            read.setUserName(snapshot.user.getName());
            read.setStudentNames(String.join("\n", snapshot.studentNames));
            read.setClassNames(String.join("\n", snapshot.classNames));
            read.setClassIds(snapshot.classIds.stream().map(String::valueOf)
                    .collect(Collectors.joining(",")));
            announcementReadRepository.save(read);
        }
    }

    private boolean isHomeroomTeacher(Long teacherId, Long yearId, List<Long> ids) {
        Set<Long> homeroom = homeroomAssignmentRepository
                .findActiveByTeacherAndYear(teacherId, yearId).stream()
                .map(h -> h.getCls().getId()).collect(Collectors.toSet());
        return homeroom.containsAll(ids);
    }

    private void replaceClasses(Announcement announcement, List<Long> ids) {
        for (Long id : new LinkedHashSet<>(ids)) {
            AnnouncementClass link = new AnnouncementClass();
            link.setAnnouncement(announcement);
            link.setCls(schoolClassRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Class", "id", id)));
            announcement.getAnnouncementClasses().add(link);
            announcementClassRepository.save(link);
        }
    }

    private void publish(Announcement announcement) {
        Map<Long, RecipientSnapshot> recipients = new LinkedHashMap<>();
        List<Long> classIds = announcementClassRepository
                .findByAnnouncementId(announcement.getId()).stream()
                .map(ac -> ac.getCls().getId()).toList();
        collectClassRecipients(classIds, announcement.getTargetRole(),
                announcement.getAcademicYear().getId(), recipients);
        saveRecipientSnapshot(announcement, recipients);
        String tag = "HOMEROOM_TEACHER".equals(announcement.getSenderType())
                ? "GVCN" : "GV bộ môn";
        recipients.keySet().forEach(id -> notificationService.createNotification(id,
                announcement.getTitle(), announcement.getBody(), tag,
                announcement.getId(), "ANNOUNCEMENT"));
    }

    private boolean canViewAnnouncement(Announcement announcement, Long userId, UserRole role) {
        if (role == UserRole.TEACHER && announcement.getSender().getId().equals(userId)) {
            return true;
        }
        return announcement.getDeliveryStatus() == AnnouncementDeliveryStatus.PUBLISHED
                && announcementReadRepository.existsByAnnouncementIdAndUserId(
                        announcement.getId(), userId);
    }

    private AnnouncementDto toDto(Announcement announcement, boolean ignoredReadFlag) {
        return toDto(announcement, (AnnouncementRead) null);
    }

    private AnnouncementDto toDto(Announcement announcement, AnnouncementRead recipient) {
        return toDto(announcement, recipient,
                announcementClassRepository.findByAnnouncementId(announcement.getId()),
                policyViolationRepository.findByAnnouncementIdOrderById(announcement.getId()));
    }

    private AnnouncementDto toDto(Announcement announcement, AnnouncementRead recipient,
            List<AnnouncementClass> links, List<AnnouncementPolicyViolation> policyViolations) {
        List<String> classNames = links.stream().map(ac -> ac.getCls().getName()).distinct().toList();
        List<Long> classIds = links.stream().map(ac -> ac.getCls().getId()).distinct().toList();
        List<AnnouncementViolationDto> violations = policyViolations.stream()
                .map(v -> new AnnouncementViolationDto(v.getRule() == null ? null : v.getRule().getId(),
                        v.getMatchedField(), v.getMatchedPhrase()))
                .toList();
        return new AnnouncementDto(announcement.getId(), announcement.getTitle(),
                announcement.getBody(), announcement.getTargetRole(),
                announcement.getTeacher() == null ? null : announcement.getTeacher().getId(),
                announcement.getSender().getName(), classNames, classIds,
                recipient != null && recipient.getReadAt() != null, announcement.getCreatedAt(),
                announcement.getAcademicYear().getId(), announcement.getDeliveryStatus(),
                announcement.getSystemRejectionMessage(), announcement.getSenderType(),
                announcement.getRecipientScope(),
                announcement.getRetryOfAnnouncement() == null
                        ? null : announcement.getRetryOfAnnouncement().getId(),
                violations);
    }

    private AnnouncementPolicyRuleDto toPolicyRuleDto(AnnouncementContentRule rule) {
        return new AnnouncementPolicyRuleDto(rule.getId(), rule.getPhrase(),
                rule.getScope(), rule.getMatchType());
    }

    private AnnouncementRead requireRecipient(Long announcementId, Long userId, UserRole role) {
        requireRecipientRole(role);
        AnnouncementRead recipient = announcementReadRepository
                .findByAnnouncementIdAndUserId(announcementId, userId)
                .orElseThrow(() -> new ForbiddenException(
                        "Bạn không thuộc danh sách người nhận thông báo"));
        if (recipient.getAnnouncement().getDeliveryStatus()
                != AnnouncementDeliveryStatus.PUBLISHED) {
            throw new ForbiddenException("Thông báo không được phát hành");
        }
        return recipient;
    }

    private void requireRecipientRole(UserRole role) {
        if (role != UserRole.PARENT && role != UserRole.STUDENT && role != UserRole.TEACHER) {
            throw new ForbiddenException("Tài khoản không thuộc nhóm người nhận thông báo");
        }
    }

    private String normalizeRecipientStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("UNREAD", "READ").contains(normalized)) {
            throw new BadRequestException("Trạng thái người nhận không hợp lệ");
        }
        return normalized;
    }

    private boolean matchesStatus(AnnouncementRead recipient, String status) {
        return recipientStatus(recipient).equals(status);
    }

    private String recipientStatus(AnnouncementRead recipient) {
        return recipient.getReadAt() != null ? "READ" : "UNREAD";
    }

    private AnnouncementRecipientDto toRecipientDto(AnnouncementRead recipient) {
        return new AnnouncementRecipientDto(recipient.getUser().getId(), recipient.getUserName(),
                recipient.getRecipientRole(), splitText(recipient.getStudentNames()),
                splitText(recipient.getClassNames()), recipient.getReadAt(),
                recipientStatus(recipient));
    }

    private String recipientSearchText(AnnouncementRead recipient) {
        return String.join(" ", Objects.toString(recipient.getUserName(), ""),
                Objects.toString(recipient.getStudentNames(), ""),
                Objects.toString(recipient.getClassNames(), "")).toLowerCase(Locale.ROOT);
    }

    private List<String> splitText(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split("\\n")).map(String::trim)
                .filter(item -> !item.isEmpty()).toList();
    }

    private Set<Long> splitLongs(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(value.split(",")).map(String::trim)
                .filter(item -> !item.isEmpty()).map(Long::valueOf)
                .collect(Collectors.toSet());
    }

    private record PreparedRule(String phrase, String normalizedPhrase,
            AnnouncementPolicyScope scope, AnnouncementPolicyMatchType matchType) {}

    private record PolicyMatch(AnnouncementContentRule rule, String field) {}

    private record PolicyEvaluation(String rejectionMessage, List<PolicyMatch> matches) {}

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
