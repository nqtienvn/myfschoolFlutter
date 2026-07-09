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
import vn.edu.fpt.myfschool.common.dto.TeacherSummaryDto;
import vn.edu.fpt.myfschool.common.dto.UpdateTeacherSubjectsRequest;
import vn.edu.fpt.myfschool.common.enums.AssignmentStatus;
import vn.edu.fpt.myfschool.common.enums.UserRole;
import vn.edu.fpt.myfschool.common.enums.UserStatus;
import vn.edu.fpt.myfschool.common.exception.BadRequestException;
import vn.edu.fpt.myfschool.common.exception.ConflictException;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.entity.Subject;
import vn.edu.fpt.myfschool.entity.Teacher;
import vn.edu.fpt.myfschool.entity.User;
import vn.edu.fpt.myfschool.entity.UserSetting;
import vn.edu.fpt.myfschool.repository.SubjectRepository;
import vn.edu.fpt.myfschool.repository.TeacherRepository;
import vn.edu.fpt.myfschool.repository.TeachingAssignmentRepository;
import vn.edu.fpt.myfschool.repository.UserRepository;
import vn.edu.fpt.myfschool.repository.UserSettingRepository;
import vn.edu.fpt.myfschool.service.AdminUserService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service("adminUserService")
@RequiredArgsConstructor
@Transactional
public class AdminUserServiceImpl implements AdminUserService {

    private static final String DEFAULT_TEACHER_PASSWORD = "12345678";

    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;
    private final SubjectRepository subjectRepository;
    private final UserSettingRepository userSettingRepository;
    private final TeachingAssignmentRepository teachingAssignmentRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    @Override
    public Page<AdminUserDto> listUsers(UserRole role, UserStatus status, String keyword, int page, int size) {
        String query = keyword == null || keyword.isBlank() ? null : keyword.trim();
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 100));
        PageRequest pageable = PageRequest.of(
            safePage,
            safeSize,
            Sort.by(Sort.Direction.DESC, "createdAt")
        );
        return userRepository.searchAdminUsers(role, status, query, pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<TeacherSummaryDto> listTeachers(UserStatus status, String keyword, Long subjectId, int page, int size) {
        String query = keyword == null || keyword.isBlank() ? null : keyword.trim();
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 100));
        PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "id"));
        return teacherRepository.searchTeachers(status, query, subjectId, pageable).map(this::toTeacherSummaryDto);
    }

    @Override
    public AdminUserDto updateUserStatus(Long userId, UserStatus status) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        user.setStatus(status);
        user = userRepository.save(user);
        return toDto(user);
    }

    @Override
    public TeacherSummaryDto createTeacherAccount(CreateTeacherAccountRequest request) {
        String phone = request.phone().trim();
        String employeeCode = request.employeeCode().trim();
        String email = request.email() == null || request.email().isBlank() ? null : request.email().trim();
        String department = request.department() == null || request.department().isBlank() ? null : request.department().trim();
        List<Subject> subjects = loadSubjects(request.subjectIds());
        if (userRepository.existsByPhone(phone)) {
            throw new ConflictException("Số điện thoại đã được đăng ký");
        }
        if (teacherRepository.existsByEmployeeCode(employeeCode)) {
            throw new ConflictException("Mã giáo viên đã tồn tại");
        }

        User user = new User();
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode(DEFAULT_TEACHER_PASSWORD));
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setRole(UserRole.TEACHER);
        user.setStatus(UserStatus.ACTIVE);
        user = userRepository.save(user);

        Teacher teacher = new Teacher();
        teacher.setUser(user);
        teacher.setEmployeeCode(employeeCode);
        teacher.setDepartment(department);
        teacher.setSubjects(new HashSet<>(subjects));
        teacher = teacherRepository.save(teacher);

        UserSetting settings = new UserSetting();
        settings.setUser(user);
        userSettingRepository.save(settings);

        return toTeacherSummaryDto(teacher);
    }

    @Override
    public TeacherSummaryDto updateTeacherSubjects(Long teacherId, UpdateTeacherSubjectsRequest request) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", "id", teacherId));
        List<Subject> subjects = loadSubjects(request.subjectIds());
        Set<Long> nextIds = new HashSet<>(request.subjectIds());

        for (Subject subject : teacher.getSubjects()) {
            if (!nextIds.contains(subject.getId())
                    && teachingAssignmentRepository.existsByTeacherIdAndSubjectIdAndStatus(
                            teacherId, subject.getId(), AssignmentStatus.ACTIVE)) {
                throw new ConflictException("Không thể gỡ môn đang có phân công active");
            }
        }

        teacher.setSubjects(new HashSet<>(subjects));
        return toTeacherSummaryDto(teacherRepository.save(teacher));
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

    private AdminUserDto toDto(User user) {
        return new AdminUserDto(
            user.getId(), user.getPhone(), user.getName(), user.getEmail(),
            user.getRole(), user.getStatus(), user.getCreatedAt());
    }

    private TeacherSummaryDto toTeacherSummaryDto(Teacher teacher) {
        User user = teacher.getUser();
        List<SubjectDto> subjects = teacher.getSubjects().stream()
                .map(s -> new SubjectDto(s.getId(), s.getName(), s.getCode()))
                .toList();
        return new TeacherSummaryDto(
                teacher.getId(),
                user.getId(),
                user.getPhone(),
                user.getName(),
                user.getEmail(),
                user.getStatus(),
                teacher.getEmployeeCode(),
                teacher.getDepartment(),
                user.getAvatar(),
                subjects);
    }
}
