package vn.edu.fpt.myfschool.service.impl;

import vn.edu.fpt.myfschool.common.dto.ChangePasswordRequest;
import vn.edu.fpt.myfschool.common.dto.LoginRequest;
import vn.edu.fpt.myfschool.common.dto.LoginResponse;
import vn.edu.fpt.myfschool.common.dto.ParentDto;
import vn.edu.fpt.myfschool.common.dto.ParentChildDto;
import vn.edu.fpt.myfschool.common.dto.RegisterRequest;
import vn.edu.fpt.myfschool.common.dto.StudentDto;
import vn.edu.fpt.myfschool.common.dto.TeacherDto;
import vn.edu.fpt.myfschool.common.dto.UpdateProfileRequest;
import vn.edu.fpt.myfschool.common.dto.UserDto;
import vn.edu.fpt.myfschool.entity.HomeroomAssignment;
import vn.edu.fpt.myfschool.entity.Parent;
import vn.edu.fpt.myfschool.entity.Student;
import vn.edu.fpt.myfschool.entity.StudentGuardian;
import vn.edu.fpt.myfschool.entity.Teacher;
import vn.edu.fpt.myfschool.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.enums.UserRole;
import vn.edu.fpt.myfschool.common.enums.UserStatus;
import vn.edu.fpt.myfschool.common.exception.BadRequestException;
import vn.edu.fpt.myfschool.common.exception.ConflictException;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.repository.HomeroomAssignmentRepository;
import vn.edu.fpt.myfschool.repository.ParentRepository;
import vn.edu.fpt.myfschool.repository.StudentGuardianRepository;
import vn.edu.fpt.myfschool.repository.StudentRepository;
import vn.edu.fpt.myfschool.repository.TeacherRepository;
import vn.edu.fpt.myfschool.repository.UserRepository;
import vn.edu.fpt.myfschool.security.JwtTokenProvider;
import vn.edu.fpt.myfschool.service.AuthService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service("authService")
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    @Value("${app.auth.self-registration-enabled:false}")
    private boolean selfRegistrationEnabled;

    private final UserRepository userRepository;
    private final ParentRepository parentRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final StudentGuardianRepository studentGuardianRepository;
    private final HomeroomAssignmentRepository homeroomAssignmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByPhone(request.phone())
            .orElseThrow(() -> new BadRequestException("Số điện thoại hoặc mật khẩu không đúng"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadRequestException("Số điện thoại hoặc mật khẩu không đúng");
        }

        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new BadRequestException("Tài khoản chưa được kích hoạt. Vui lòng liên hệ nhà trường");
        }
        if (user.getStatus() == UserStatus.LOCKED) {
            throw new BadRequestException("Tài khoản đã bị khóa. Vui lòng liên hệ nhà trường");
        }

        String token = jwtTokenProvider.generateToken(
                user.getId(), user.getRole(), user.getName(), user.getCredentialsUpdatedAt());
        UserDto profile = getProfile(user.getId());

        return new LoginResponse(token, "Bearer", jwtTokenProvider.getExpirationMs() / 1000, profile);
    }

    @Override
    public LoginResponse register(RegisterRequest request) {
        if (!selfRegistrationEnabled) {
            throw new BadRequestException("Tài khoản MyFschool do nhà trường cấp");
        }
        if (request.role() == UserRole.ADMIN || request.role() == UserRole.TEACHER) {
            throw new BadRequestException("Không thể tự đăng ký tài khoản ADMIN hoặc TEACHER");
        }
        if (userRepository.existsByPhone(request.phone())) {
            throw new ConflictException("Số điện thoại đã được đăng ký");
        }

        if (request.email() != null && !request.email().isBlank()
                && userRepository.existsByEmailIgnoreCase(request.email().trim())) {
            throw new ConflictException("Email đã được đăng ký");
        }

        // Create User
        User user = new User();
        user.setPhone(request.phone());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setName(request.name());
        user.setEmail(normalizeEmail(request.email()));
        user.setRole(request.role());
        user.setStatus(UserStatus.ACTIVE);
        user.setCredentialsUpdatedAt(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));
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
            case TEACHER, ADMIN -> throw new BadRequestException("Không thể tự đăng ký tài khoản ADMIN hoặc TEACHER");
        }

        String token = jwtTokenProvider.generateToken(
                user.getId(), user.getRole(), user.getName(), user.getCredentialsUpdatedAt());
        UserDto profile = getProfile(user.getId());

        return new LoginResponse(token, "Bearer", jwtTokenProvider.getExpirationMs() / 1000, profile);
    }

    @Transactional(readOnly = true)
    @Override
    public UserDto getProfile(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        ParentDto parentDto = null;
        StudentDto studentDto = null;
        TeacherDto teacherDto = null;

        switch (user.getRole()) {
            case PARENT -> {
                Parent parent = parentRepository.findByUserId(userId).orElse(null);
                if (parent != null) {
                    List<Student> children = parentRepository.findChildrenByParentId(parent.getId());
                    List<ParentChildDto> childSummaries = children.stream()
                        .map(this::toParentChildDto)
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
                    teacherDto = new TeacherDto(teacher.getId(), teacher.getEmployeeCode());
                }
            }
        }

        return new UserDto(
            user.getId(), user.getPhone(), user.getName(), user.getEmail(),
            user.getRole(), user.getStatus(), user.getCreatedAt(), user.getMustChangePassword(),
            parentDto, studentDto, teacherDto);
    }

    private ParentChildDto toParentChildDto(Student student) {
        HomeroomAssignment homeroom = findCurrentHomeroom(student);
        return new ParentChildDto(
            student.getId(),
            student.getUser().getName(),
            student.getStudentCode(),
            student.getCurrentClass() != null ? student.getCurrentClass().getName() : null,
            student.getCurrentClass() != null ? student.getCurrentClass().getId() : null,
            student.getCurrentClass() != null ? student.getCurrentClass().getSchoolName() : null,
            student.getCurrentClass() != null ? student.getCurrentClass().getAcademicYear().getName() : null,
            homeroom != null ? homeroom.getTeacher().getUser().getName() : null,
            homeroom != null ? homeroom.getTeacher().getUser().getPhone() : null,
            student.getDateOfBirth(),
            student.getGender(),
            student.getAddress(),
            student.getUser().getEmail(),
            student.getUser().getStatus());
    }

    private HomeroomAssignment findCurrentHomeroom(Student student) {
        if (student.getCurrentClass() == null) return null;
        var academicYear = student.getCurrentClass().getAcademicYear();
        LocalDate referenceDate = LocalDate.now();
        if (referenceDate.isBefore(academicYear.getStartDate())) {
            referenceDate = academicYear.getStartDate();
        } else if (referenceDate.isAfter(academicYear.getEndDate())) {
            referenceDate = academicYear.getEndDate();
        }
        LocalDate effectiveDate = referenceDate;
        return homeroomAssignmentRepository.findByClsIdAndAcademicYearId(
                student.getCurrentClass().getId(), academicYear.getId()).stream()
            .filter(item -> !item.getEffectiveFrom().isAfter(effectiveDate))
            .filter(item -> item.getEffectiveTo() == null
                || !item.getEffectiveTo().isBefore(effectiveDate))
            .max(Comparator.comparing(HomeroomAssignment::getEffectiveFrom))
            .orElse(null);
    }

    @Override
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            throw new BadRequestException("Mật khẩu cũ không đúng");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(false);
        user.setCredentialsUpdatedAt(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));
        userRepository.save(user);
    }

    @Override
    public UserDto updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (request.name() != null) user.setName(request.name());
        if (request.email() != null) {
            String email = normalizeEmail(request.email());
            if (email != null && !email.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmailIgnoreCase(email)) {
                throw new ConflictException("Email đã được đăng ký");
            }
            if (user.getEmail() == null || !user.getEmail().equalsIgnoreCase(email == null ? "" : email)) {
                user.setEmailVerifiedAt(null);
            }
            user.setEmail(email);
        }
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

    private String normalizeEmail(String email) {
        return email == null || email.isBlank() ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
