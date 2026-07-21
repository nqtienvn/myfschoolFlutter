package vn.edu.fpt.myfschool.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.CreateStudentEnrollmentRequest;
import vn.edu.fpt.myfschool.common.dto.StudentAccountByClassDto;
import vn.edu.fpt.myfschool.common.dto.StudentEnrollmentResultDto;
import vn.edu.fpt.myfschool.common.enums.AcademicYearStatus;
import vn.edu.fpt.myfschool.common.enums.EnrollmentStatus;
import vn.edu.fpt.myfschool.common.enums.UserRole;
import vn.edu.fpt.myfschool.common.enums.UserStatus;
import vn.edu.fpt.myfschool.common.exception.BadRequestException;
import vn.edu.fpt.myfschool.common.exception.ConflictException;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.common.util.CredentialGenerator;
import vn.edu.fpt.myfschool.entity.AcademicYear;
import vn.edu.fpt.myfschool.entity.Enrollment;
import vn.edu.fpt.myfschool.entity.Parent;
import vn.edu.fpt.myfschool.entity.SchoolClass;
import vn.edu.fpt.myfschool.entity.Student;
import vn.edu.fpt.myfschool.entity.StudentGuardian;
import vn.edu.fpt.myfschool.entity.User;
import vn.edu.fpt.myfschool.repository.AcademicYearRepository;
import vn.edu.fpt.myfschool.repository.ClassRepository;
import vn.edu.fpt.myfschool.repository.EnrollmentRepository;
import vn.edu.fpt.myfschool.repository.ParentRepository;
import vn.edu.fpt.myfschool.repository.StudentGuardianRepository;
import vn.edu.fpt.myfschool.repository.StudentRepository;
import vn.edu.fpt.myfschool.repository.UserRepository;
import vn.edu.fpt.myfschool.service.MailDeliveryService;
import vn.edu.fpt.myfschool.service.StudentEnrollmentService;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class StudentEnrollmentServiceImpl implements StudentEnrollmentService {
    private static final int MAX_LOGIN_GENERATION_ATTEMPTS = 100;

    private final SecureRandom secureRandom = new SecureRandom();
    private final AcademicYearRepository yearRepository;
    private final ClassRepository classRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;
    private final StudentGuardianRepository guardianRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final CredentialGenerator credentialGenerator;
    private final MailDeliveryService mailDeliveryService;

    @Override
    public StudentEnrollmentResultDto create(CreateStudentEnrollmentRequest request) {
        AcademicYear year = yearRepository.findById(request.academicYearId())
            .orElseThrow(() -> new ResourceNotFoundException("AcademicYear", "id", request.academicYearId()));
        if (year.getStatus() == AcademicYearStatus.COMPLETED) {
            throw new ConflictException("Không được thêm học sinh khi năm học đã hoàn tất");
        }
        SchoolClass cls = classRepository.findById(request.classId())
            .orElseThrow(() -> new ResourceNotFoundException("Class", "id", request.classId()));
        if (!cls.getAcademicYear().getId().equals(year.getId())) {
            throw new ConflictException("Lớp không thuộc năm học đã chọn");
        }

        String code = request.studentCode().trim().toUpperCase(Locale.ROOT);
        if (studentRepository.existsByStudentCode(code)) throw new ConflictException("Mã học sinh đã tồn tại");
        requireUniqueCitizen(request.studentCitizenId());
        String studentEmail = normalizeEmail(request.studentEmail());
        if (studentEmail == null) {
            throw new BadRequestException("Email học sinh là bắt buộc khi tạo tài khoản");
        }
        requireUniqueEmail(studentEmail);

        String studentLogin = generateUniqueStudentLogin();
        String studentPassword = credentialGenerator.temporaryPassword();
        User studentUser = newUser(studentLogin, request.studentName(), studentEmail,
                request.studentCitizenId(), UserRole.STUDENT, studentPassword);
        Student student = new Student();
        student.setUser(studentUser);
        student.setStudentCode(code);
        student.setCurrentClass(cls);
        student.setDateOfBirth(request.dateOfBirth());
        student.setGender(request.gender());
        student.setAddress(clean(request.studentAddress()));
        student = studentRepository.save(student);

        String parentPhone = request.parentPhone().trim();
        User parentUser = userRepository.findByPhone(parentPhone).orElse(null);
        boolean parentReused = parentUser != null;
        String parentPassword = null;
        Parent parent;
        if (parentReused) {
            if (parentUser.getRole() != UserRole.PARENT) {
                throw new ConflictException("Số điện thoại phụ huynh đang thuộc tài khoản vai trò khác");
            }
            parent = parentRepository.findByUserId(parentUser.getId())
                .orElseThrow(() -> new ConflictException("Tài khoản phụ huynh chưa có hồ sơ hợp lệ"));
            String requestedParentEmail = normalizeEmail(request.parentEmail());
            if (requestedParentEmail != null
                    && !requestedParentEmail.equalsIgnoreCase(parentUser.getEmail() == null ? "" : parentUser.getEmail())) {
                requireUniqueEmail(requestedParentEmail);
                parentUser.setEmail(requestedParentEmail);
                parentUser.setEmailVerifiedAt(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));
                userRepository.save(parentUser);
            }
        } else {
            String parentEmail = normalizeEmail(request.parentEmail());
            if (parentEmail == null) {
                throw new BadRequestException("Email phụ huynh là bắt buộc khi tạo tài khoản mới");
            }
            requireUniqueEmail(parentEmail);
            requireUniqueCitizen(request.parentCitizenId());
            parentPassword = credentialGenerator.temporaryPassword();
            parentUser = newUser(parentPhone, request.parentName(), parentEmail,
                    request.parentCitizenId(), UserRole.PARENT, parentPassword);
            parent = new Parent();
            parent.setUser(parentUser);
            parent.setAddress(clean(request.parentAddress()));
            parent.setOccupation(clean(request.parentOccupation()));
            parent = parentRepository.save(parent);
        }

        StudentGuardian guardian = new StudentGuardian();
        guardian.setStudent(student);
        guardian.setGuardian(parent);
        guardian.setRelationship(request.relationship());
        guardianRepository.save(guardian);

        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCls(cls);
        enrollment.setAcademicYear(year);
        enrollment.setJoinDate(year.getStartDate());
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        enrollmentRepository.save(enrollment);

        String studentRecipient = verifiedEmail(studentUser);
        boolean studentCredentialsEmailed = studentRecipient != null;
        if (studentCredentialsEmailed) {
            mailDeliveryService.sendAccountCreatedAfterCommit(studentRecipient, studentUser.getName(),
                    studentUser.getPhone(), studentPassword, UserRole.STUDENT);
        }

        boolean parentCredentialsEmailed = !parentReused && verifiedEmail(parentUser) != null;
        if (parentCredentialsEmailed) {
            mailDeliveryService.sendAccountCreatedAfterCommit(parentUser.getEmail(), parentUser.getName(),
                    parentUser.getPhone(), parentPassword, UserRole.PARENT);
        }

        return new StudentEnrollmentResultDto(
            student.getId(), code, studentUser.getPhone(), studentCredentialsEmailed,
            cls.getId(), cls.getName(), parent.getId(), parentUser.getPhone(),
            parentCredentialsEmailed, parentReused);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentAccountByClassDto> listAccounts(Long academicYearId, Long classId) {
        SchoolClass cls = classRepository.findById(classId)
            .orElseThrow(() -> new ResourceNotFoundException("Class", "id", classId));
        if (!cls.getAcademicYear().getId().equals(academicYearId)) {
            throw new ConflictException("Lớp không thuộc năm học đã chọn");
        }

        return enrollmentRepository.findActiveStudentsByClassAndYear(classId, academicYearId).stream()
            .map(student -> new StudentAccountByClassDto(
                student.getId(), student.getStudentCode(), student.getUser().getName(),
                student.getUser().getPhone(), student.getUser().getEmail(),
                guardianRepository.findByStudentId(student.getId()).stream()
                    .map(link -> new StudentAccountByClassDto.GuardianAccountDto(
                        link.getGuardian().getId(), link.getGuardian().getUser().getName(),
                        link.getGuardian().getUser().getPhone(), link.getGuardian().getUser().getEmail(),
                        link.getRelationship()))
                    .toList()))
            .toList();
    }

    private User newUser(String login, String name, String email, String citizenId,
                         UserRole role, String temporaryPassword) {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        User user = new User();
        user.setPhone(login);
        user.setName(name.trim());
        user.setEmail(normalizeEmail(email));
        user.setCitizenId(clean(citizenId));
        user.setPassword(passwordEncoder.encode(temporaryPassword));
        user.setMustChangePassword(true);
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        user.setCredentialsUpdatedAt(now);
        if (user.getEmail() != null) user.setEmailVerifiedAt(now);
        return userRepository.save(user);
    }

    private String generateUniqueStudentLogin() {
        for (int attempt = 0; attempt < MAX_LOGIN_GENERATION_ATTEMPTS; attempt++) {
            StringBuilder login = new StringBuilder(10);
            login.append(secureRandom.nextInt(9) + 1);
            for (int index = 1; index < 10; index++) login.append(secureRandom.nextInt(10));
            String value = login.toString();
            if (!userRepository.existsByPhone(value)) return value;
        }
        throw new ConflictException("Không thể tạo số đăng nhập học sinh không trùng. Vui lòng thử lại");
    }

    private void requireUniqueCitizen(String value) {
        String id = clean(value);
        if (id != null && userRepository.existsByCitizenId(id)) {
            throw new ConflictException("CCCD/định danh đã tồn tại");
        }
    }

    private void requireUniqueEmail(String email) {
        if (email != null && userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Email đã tồn tại");
        }
    }

    private String verifiedEmail(User user) {
        return user != null && user.getEmailVerifiedAt() != null ? user.getEmail() : null;
    }

    private String normalizeEmail(String value) {
        return value == null || value.isBlank() ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
