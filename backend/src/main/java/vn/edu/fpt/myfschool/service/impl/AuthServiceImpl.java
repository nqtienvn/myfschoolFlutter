package vn.edu.fpt.myfschool.service.impl;

import vn.edu.fpt.myfschool.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.enums.UserRole;
import vn.edu.fpt.myfschool.common.enums.UserStatus;
import vn.edu.fpt.myfschool.common.exception.BadRequestException;
import vn.edu.fpt.myfschool.common.exception.ConflictException;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.entity.*;
import vn.edu.fpt.myfschool.repository.*;
import vn.edu.fpt.myfschool.security.JwtTokenProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service("authService")
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final ParentRepository parentRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final UserSettingRepository userSettingRepository;
    private final StudentGuardianRepository studentGuardianRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByPhone(request.phone())
            .orElseThrow(() -> new BadRequestException("Số điện thoại hoặc mật khẩu không đúng"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadRequestException("Số điện thoại hoặc mật khẩu không đúng");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BadRequestException("Tài khoản đã bị khóa hoặc vô hiệu hóa");
        }

        String token = jwtTokenProvider.generateToken(user.getId(), user.getRole(), user.getName());
        UserDto profile = getProfile(user.getId());

        return new LoginResponse(token, "Bearer", jwtTokenProvider.getExpirationMs() / 1000, profile);
    }

    @Override
    public LoginResponse register(RegisterRequest request) {
        if (userRepository.existsByPhone(request.phone())) {
            throw new ConflictException("Số điện thoại đã được đăng ký");
        }

        // Create User
        User user = new User();
        user.setPhone(request.phone());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setName(request.name());
        user.setEmail(request.email());
        user.setRole(request.role());
        user.setStatus(UserStatus.ACTIVE);
        user = userRepository.save(user);

        // Create role-specific profile
        switch (request.role()) {
            case PARENT -> {
                Parent parent = new Parent();
                parent.setUser(user);
                parent.setAddress(request.address());
                parent.setOccupation(request.occupation());
                parentRepository.save(parent);
            }
            case STUDENT -> {
                if (request.studentCode() == null || request.studentCode().isBlank()) {
                    throw new BadRequestException("Mã học sinh không được để trống");
                }
                if (studentRepository.existsByStudentCode(request.studentCode())) {
                    throw new ConflictException("Mã học sinh đã tồn tại");
                }
                Student student = new Student();
                student.setUser(user);
                student.setStudentCode(request.studentCode());
                studentRepository.save(student);
            }
            case TEACHER -> {
                if (request.employeeCode() == null || request.employeeCode().isBlank()) {
                    throw new BadRequestException("Mã giáo viên không được để trống");
                }
                if (teacherRepository.existsByEmployeeCode(request.employeeCode())) {
                    throw new ConflictException("Mã giáo viên đã tồn tại");
                }
                Teacher teacher = new Teacher();
                teacher.setUser(user);
                teacher.setEmployeeCode(request.employeeCode());
                teacher.setDepartment(request.department());
                teacherRepository.save(teacher);
            }
            case ADMIN -> {
                // ponytail: admin has no profile table, user record is sufficient
            }
        }

        // Create default settings
        UserSetting settings = new UserSetting();
        settings.setUser(user);
        userSettingRepository.save(settings);

        String token = jwtTokenProvider.generateToken(user.getId(), user.getRole(), user.getName());
        UserDto profile = getProfile(user.getId());

        return new LoginResponse(token, "Bearer", jwtTokenProvider.getExpirationMs() / 1000, profile);
    }

    @Transactional(readOnly = true)
    @Override
    public UserDto getProfile(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        UserSetting setting = userSettingRepository.findByUserId(userId).orElse(null);
        UserSettingDto settingDto = setting != null
            ? new UserSettingDto(setting.getTheme(), setting.getLanguage(), setting.getNotificationEnabled())
            : null;

        ParentDto parentDto = null;
        StudentDto studentDto = null;
        TeacherDto teacherDto = null;

        switch (user.getRole()) {
            case PARENT -> {
                Parent parent = parentRepository.findByUserId(userId).orElse(null);
                if (parent != null) {
                    List<Student> children = parentRepository.findChildrenByParentId(parent.getId());
                    List<StudentSummaryDto> childSummaries = children.stream()
                        .map(s -> new StudentSummaryDto(
                            s.getId(),
                            s.getUser().getName(),
                            s.getStudentCode(),
                            s.getCurrentClass() != null ? s.getCurrentClass().getName() : null,
                            s.getUser().getAvatar()))
                        .collect(Collectors.toList());
                    parentDto = new ParentDto(parent.getId(), parent.getAddress(), parent.getOccupation(), childSummaries);
                }
            }
            case STUDENT -> {
                Student student = studentRepository.findByUserId(userId).orElse(null);
                if (student != null) {
                    studentDto = new StudentDto(
                        student.getId(),
                        student.getStudentCode(),
                        student.getCurrentClass() != null ? student.getCurrentClass().getName() : null,
                        student.getCurrentClass() != null ? student.getCurrentClass().getId() : null,
                        student.getDateOfBirth());
                }
            }
            case TEACHER -> {
                Teacher teacher = teacherRepository.findByUserId(userId).orElse(null);
                if (teacher != null) {
                    teacherDto = new TeacherDto(teacher.getId(), teacher.getEmployeeCode(), teacher.getDepartment());
                }
            }
        }

        return new UserDto(
            user.getId(), user.getPhone(), user.getName(), user.getEmail(),
            user.getAvatar(), user.getRole(), user.getStatus(), user.getCreatedAt(),
            parentDto, studentDto, teacherDto, settingDto);
    }

    @Override
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            throw new BadRequestException("Mật khẩu cũ không đúng");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    @Override
    public UserDto updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (request.name() != null) user.setName(request.name());
        if (request.email() != null) user.setEmail(request.email());
        if (request.avatar() != null) user.setAvatar(request.avatar());
        userRepository.save(user);

        if (user.getRole() == UserRole.PARENT) {
            Parent parent = parentRepository.findByUserId(userId).orElse(null);
            if (parent != null) {
                if (request.address() != null) parent.setAddress(request.address());
                if (request.occupation() != null) parent.setOccupation(request.occupation());
                parentRepository.save(parent);
            }
        }

        return getProfile(userId);
    }

    @Override
    public UserSettingDto updateSettings(Long userId, UpdateSettingsRequest request) {
        UserSetting setting = userSettingRepository.findByUserId(userId)
            .orElseGet(() -> {
                User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
                UserSetting newSetting = new UserSetting();
                newSetting.setUser(user);
                return newSetting;
            });

        if (request.theme() != null) setting.setTheme(request.theme());
        if (request.language() != null) setting.setLanguage(request.language());
        if (request.notificationEnabled() != null) setting.setNotificationEnabled(request.notificationEnabled());
        userSettingRepository.save(setting);

        return new UserSettingDto(setting.getTheme(), setting.getLanguage(), setting.getNotificationEnabled());
    }

    @Override
    public void linkGuardianStudent(Long parentId, Long studentId,
                                     vn.edu.fpt.myfschool.common.enums.Relationship relationship) {
        Parent parent = parentRepository.findById(parentId)
            .orElseThrow(() -> new ResourceNotFoundException("Parent", "id", parentId));
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("Student", "id", studentId));

        if (studentGuardianRepository.existsByStudentIdAndGuardianId(studentId, parentId)) {
            throw new ConflictException("Phụ huynh đã được liên kết với học sinh này");
        }

        StudentGuardian sg = new StudentGuardian();
        sg.setStudent(student);
        sg.setGuardian(parent);
        sg.setRelationship(relationship);
        studentGuardianRepository.save(sg);
    }
}
