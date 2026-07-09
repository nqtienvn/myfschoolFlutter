package vn.edu.fpt.myfschool.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.AdminUserDto;
import vn.edu.fpt.myfschool.common.dto.CreateTeacherAccountRequest;
import vn.edu.fpt.myfschool.common.enums.UserRole;
import vn.edu.fpt.myfschool.common.enums.UserStatus;
import vn.edu.fpt.myfschool.common.exception.ConflictException;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.entity.Teacher;
import vn.edu.fpt.myfschool.entity.User;
import vn.edu.fpt.myfschool.entity.UserSetting;
import vn.edu.fpt.myfschool.repository.TeacherRepository;
import vn.edu.fpt.myfschool.repository.UserRepository;
import vn.edu.fpt.myfschool.repository.UserSettingRepository;
import vn.edu.fpt.myfschool.service.AdminUserService;

@Service("adminUserService")
@RequiredArgsConstructor
@Transactional
public class AdminUserServiceImpl implements AdminUserService {

    private static final String DEFAULT_TEACHER_PASSWORD = "12345678";

    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;
    private final UserSettingRepository userSettingRepository;
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

    @Override
    public AdminUserDto updateUserStatus(Long userId, UserStatus status) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        user.setStatus(status);
        user = userRepository.save(user);
        return toDto(user);
    }

    @Override
    public AdminUserDto createTeacherAccount(CreateTeacherAccountRequest request) {
        String phone = request.phone().trim();
        String employeeCode = request.employeeCode().trim();
        String email = request.email() == null || request.email().isBlank() ? null : request.email().trim();
        String department = request.department() == null || request.department().isBlank() ? null : request.department().trim();
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
        teacherRepository.save(teacher);

        UserSetting settings = new UserSetting();
        settings.setUser(user);
        userSettingRepository.save(settings);

        return toDto(user);
    }

    private AdminUserDto toDto(User user) {
        return new AdminUserDto(
            user.getId(), user.getPhone(), user.getName(), user.getEmail(),
            user.getRole(), user.getStatus(), user.getCreatedAt());
    }
}
