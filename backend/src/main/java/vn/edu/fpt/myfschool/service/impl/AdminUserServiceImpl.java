package vn.edu.fpt.myfschool.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.AdminUserDto;
import vn.edu.fpt.myfschool.common.dto.CreateTeacherAccountRequest;
import vn.edu.fpt.myfschool.common.dto.SubjectDto;
import vn.edu.fpt.myfschool.common.dto.TeacherAccountCredentialDto;
import vn.edu.fpt.myfschool.common.dto.TeacherHomeroomDto;
import vn.edu.fpt.myfschool.common.dto.TeacherManagementSummaryDto;
import vn.edu.fpt.myfschool.common.dto.TeacherSummaryDto;
import vn.edu.fpt.myfschool.common.dto.TeacherYearAssignmentDto;
import vn.edu.fpt.myfschool.common.dto.UpdateTeacherProfileRequest;
import vn.edu.fpt.myfschool.common.dto.UpdateTeacherSubjectsRequest;
import vn.edu.fpt.myfschool.common.enums.AssignmentStatus;
import vn.edu.fpt.myfschool.common.enums.UserRole;
import vn.edu.fpt.myfschool.common.enums.UserStatus;
import vn.edu.fpt.myfschool.common.exception.BadRequestException;
import vn.edu.fpt.myfschool.common.exception.ConflictException;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.common.util.CredentialGenerator;
import vn.edu.fpt.myfschool.entity.Subject;
import vn.edu.fpt.myfschool.entity.Teacher;
import vn.edu.fpt.myfschool.entity.TeachingAssignment;
import vn.edu.fpt.myfschool.entity.User;
import vn.edu.fpt.myfschool.repository.AcademicYearRepository;
import vn.edu.fpt.myfschool.repository.HomeroomAssignmentRepository;
import vn.edu.fpt.myfschool.repository.SubjectRepository;
import vn.edu.fpt.myfschool.repository.TeacherRepository;
import vn.edu.fpt.myfschool.repository.TeachingAssignmentRepository;
import vn.edu.fpt.myfschool.repository.UserRepository;
import vn.edu.fpt.myfschool.service.AdminUserService;
import vn.edu.fpt.myfschool.service.MailDeliveryService;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Locale;
import java.util.stream.Collectors;

@Service("adminUserService")
@RequiredArgsConstructor
@Transactional
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;
    private final SubjectRepository subjectRepository;
    private final TeachingAssignmentRepository teachingAssignmentRepository;
    private final HomeroomAssignmentRepository homeroomAssignmentRepository;
    private final AcademicYearRepository academicYearRepository;
    private final PasswordEncoder passwordEncoder;
    private final CredentialGenerator credentialGenerator;
    private final MailDeliveryService mailDeliveryService;

    @Transactional(readOnly = true)
    @Override
    public Page<AdminUserDto> listUsers(UserRole role, UserStatus status, String keyword, int page, int size) {
        String query = normalizeKeyword(keyword);
        PageRequest pageable = PageRequest.of(
                Math.max(page, 0),
                Math.max(1, Math.min(size, 100)),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return userRepository.searchAdminUsers(role, status, query, pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<TeacherSummaryDto> listTeachers(UserStatus status, String keyword, Long subjectId,
                                                Long academicYearId, int page, int size) {
        validateAcademicYear(academicYearId);
        PageRequest pageable = PageRequest.of(
                Math.max(page, 0),
                Math.max(1, Math.min(size, 100)),
                Sort.by(Sort.Direction.DESC, "id"));
        return teacherRepository.searchTeachers(status, normalizeKeyword(keyword), subjectId, pageable)
                .map(teacher -> toTeacherSummaryDto(teacher, academicYearId));
    }

    @Transactional(readOnly = true)
    @Override
    public TeacherManagementSummaryDto getTeacherSummary(Long academicYearId) {
        validateAcademicYear(academicYearId);
        long total = teacherRepository.count();
        long active = teacherRepository.countByUserStatus(UserStatus.ACTIVE);
        long locked = teacherRepository.countByUserStatus(UserStatus.LOCKED);
        if (academicYearId == null) {
            return new TeacherManagementSummaryDto(total, active, locked, 0, 0);
        }

        Set<Long> unassignedTeacherIds = new HashSet<>(teacherRepository.findIdsByUserStatus(UserStatus.ACTIVE));
        unassignedTeacherIds.removeAll(teachingAssignmentRepository.findActiveTeacherIdsByYear(academicYearId));
        long homeroom = new HashSet<>(homeroomAssignmentRepository.findTeacherIdsByYear(academicYearId)).size();
        return new TeacherManagementSummaryDto(total, active, locked, unassignedTeacherIds.size(), homeroom);
    }

    @Override
    public AdminUserDto updateUserStatus(Long userId, UserStatus status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        user.setStatus(status);
        return toDto(userRepository.save(user));
    }

    @Override
    public TeacherAccountCredentialDto createTeacherAccount(CreateTeacherAccountRequest request) {
        String phone = request.phone().trim();
        String email = normalizeEmail(request.email());
        List<Subject> subjects = loadSubjects(request.subjectIds());
        ensureUniqueContact(phone, email, null);

        String temporaryPassword = credentialGenerator.temporaryPassword();
        LocalDateTime createdAt = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        User user = new User();
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode(temporaryPassword));
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setRole(UserRole.TEACHER);
        user.setStatus(UserStatus.ACTIVE);
        user.setMustChangePassword(true);
        user.setEmailVerifiedAt(createdAt);
        user.setCredentialsUpdatedAt(createdAt);
        user = userRepository.save(user);

        Teacher teacher = new Teacher();
        teacher.setUser(user);
        teacher.setEmployeeCode("GV-%04d".formatted(user.getId()));
        teacher.setSubjects(new HashSet<>(subjects));
        teacher = teacherRepository.save(teacher);

        mailDeliveryService.sendAccountCreatedAfterCommit(
                email, user.getName(), user.getPhone(), temporaryPassword, UserRole.TEACHER);
        return new TeacherAccountCredentialDto(toTeacherSummaryDto(teacher, null), true);
    }

    @Override
    public TeacherSummaryDto updateTeacherProfile(Long teacherId, UpdateTeacherProfileRequest request,
                                                   Long academicYearId) {
        validateAcademicYear(academicYearId);
        Teacher teacher = findTeacher(teacherId);
        User user = teacher.getUser();
        String phone = request.phone().trim();
        String email = normalizeEmail(request.email());
        ensureUniqueContact(phone, email, user.getId());

        user.setName(request.name().trim());
        user.setPhone(phone);
        if (user.getEmail() == null || !user.getEmail().equalsIgnoreCase(email == null ? "" : email)) {
            user.setEmailVerifiedAt(email == null ? null : LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));
        }
        user.setEmail(email);
        userRepository.save(user);
        return toTeacherSummaryDto(teacher, academicYearId);
    }

    @Override
    public TeacherSummaryDto updateTeacherSubjects(Long teacherId, UpdateTeacherSubjectsRequest request) {
        Teacher teacher = findTeacher(teacherId);
        List<Subject> subjects = loadSubjects(request.subjectIds());
        Set<Long> nextIds = new HashSet<>(request.subjectIds());

        List<TeachingAssignment> conflicts = teachingAssignmentRepository
                .findByTeacherIdAndStatus(teacherId, AssignmentStatus.ACTIVE).stream()
                .filter(assignment -> !nextIds.contains(assignment.getSubject().getId()))
                .toList();
        if (!conflicts.isEmpty()) {
            String details = conflicts.stream()
                    .collect(Collectors.groupingBy(
                            TeachingAssignment::getSubject,
                            Collectors.mapping(assignment -> assignment.getCls().getName(), Collectors.toSet())))
                    .entrySet().stream()
                    .map(entry -> entry.getKey().getName() + " (lớp " + String.join(", ", entry.getValue()) + ")")
                    .sorted()
                    .collect(Collectors.joining("; "));
            throw new ConflictException("Không thể gỡ môn đang có phân công: " + details);
        }

        teacher.setSubjects(new HashSet<>(subjects));
        return toTeacherSummaryDto(teacherRepository.save(teacher), null);
    }

    private Teacher findTeacher(Long teacherId) {
        return teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", "id", teacherId));
    }

    private void validateAcademicYear(Long academicYearId) {
        if (academicYearId != null && !academicYearRepository.existsById(academicYearId)) {
            throw new ResourceNotFoundException("AcademicYear", "id", academicYearId);
        }
    }

    private void ensureUniqueContact(String phone, String email, Long excludedUserId) {
        boolean duplicatePhone = excludedUserId == null
                ? userRepository.existsByPhone(phone)
                : userRepository.existsByPhoneAndIdNot(phone, excludedUserId);
        if (duplicatePhone) {
            throw new ConflictException("Số điện thoại đã được đăng ký");
        }
        boolean duplicateEmail = email != null && (excludedUserId == null
                ? userRepository.existsByEmailIgnoreCase(email)
                : userRepository.existsByEmailIgnoreCaseAndIdNot(email, excludedUserId));
        if (duplicateEmail) {
            throw new ConflictException("Email giáo viên đã tồn tại");
        }
    }

    private List<Subject> loadSubjects(List<Long> subjectIds) {
        if (subjectIds == null || subjectIds.isEmpty()) {
            throw new BadRequestException("Vui lòng chọn ít nhất một môn học");
        }
        Set<Long> uniqueIds = new HashSet<>(subjectIds);
        List<Subject> subjects = subjectRepository.findAllById(uniqueIds);
        if (subjects.size() != uniqueIds.size()) {
            throw new ResourceNotFoundException("Subject", "ids", uniqueIds);
        }
        return subjects;
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null || keyword.isBlank() ? null : keyword.trim();
    }

    private String normalizeEmail(String email) {
        return email == null || email.isBlank() ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private AdminUserDto toDto(User user) {
        return new AdminUserDto(
                user.getId(), user.getPhone(), user.getName(), user.getEmail(),
                user.getRole(), user.getStatus(), user.getCreatedAt());
    }

    private TeacherSummaryDto toTeacherSummaryDto(Teacher teacher, Long academicYearId) {
        User user = teacher.getUser();
        List<SubjectDto> subjects = teacher.getSubjects().stream()
                .sorted(Comparator.comparing(Subject::getName))
                .map(subject -> new SubjectDto(subject.getId(), subject.getName(), subject.getCode()))
                .toList();
        List<TeacherYearAssignmentDto> assignments = academicYearId == null
                ? List.of()
                : teachingAssignmentRepository.findActiveByTeacherAndYear(teacher.getId(), academicYearId).stream()
                        .map(assignment -> new TeacherYearAssignmentDto(
                                assignment.getId(),
                                assignment.getCls().getId(),
                                assignment.getCls().getName(),
                                assignment.getSubject().getId(),
                                assignment.getSubject().getName(),
                                assignment.getSubject().getCode()))
                        .toList();
        List<TeacherHomeroomDto> homerooms = academicYearId == null
                ? List.of()
                : homeroomAssignmentRepository.findByTeacherIdAndAcademicYearId(teacher.getId(), academicYearId).stream()
                        .map(assignment -> new TeacherHomeroomDto(
                                assignment.getId(),
                                assignment.getCls().getId(),
                                assignment.getCls().getName()))
                        .toList();
        return new TeacherSummaryDto(
                teacher.getId(),
                user.getId(),
                user.getPhone(),
                user.getName(),
                user.getEmail(),
                user.getStatus(),
                teacher.getEmployeeCode(),
                subjects,
                assignments,
                homerooms);
    }
}
