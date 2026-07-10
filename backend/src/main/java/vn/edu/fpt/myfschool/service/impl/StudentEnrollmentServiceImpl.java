package vn.edu.fpt.myfschool.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.enums.*;
import vn.edu.fpt.myfschool.common.exception.ConflictException;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.entity.*;
import vn.edu.fpt.myfschool.repository.*;
import vn.edu.fpt.myfschool.service.StudentEnrollmentService;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class StudentEnrollmentServiceImpl implements StudentEnrollmentService {
    private static final String DEFAULT_PASSWORD = "12345678";
    private final AcademicYearRepository yearRepository;
    private final ClassRepository classRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;
    private final StudentGuardianRepository guardianRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public StudentEnrollmentResultDto create(CreateStudentEnrollmentRequest request) {
        AcademicYear year = yearRepository.findById(request.academicYearId())
            .orElseThrow(() -> new ResourceNotFoundException("AcademicYear", "id", request.academicYearId()));
        if (year.getStatus() != AcademicYearStatus.DRAFT) throw new ConflictException("Chỉ được thêm học sinh khi năm học ở trạng thái DRAFT");
        SchoolClass cls = classRepository.findById(request.classId())
            .orElseThrow(() -> new ResourceNotFoundException("Class", "id", request.classId()));
        if (!cls.getAcademicYear().getId().equals(year.getId())) throw new ConflictException("Lớp không thuộc năm học đã chọn");

        String code = request.studentCode().trim().toUpperCase();
        if (studentRepository.existsByStudentCode(code) || userRepository.existsByPhone(code)) throw new ConflictException("Mã học sinh đã tồn tại");
        requireUniqueCitizen(request.studentCitizenId());

        User studentUser = newUser(code, request.studentName(), null, request.studentCitizenId(), UserRole.STUDENT);
        Student student = new Student();
        student.setUser(studentUser); student.setStudentCode(code); student.setCurrentClass(cls);
        student.setDateOfBirth(request.dateOfBirth()); student.setGender(request.gender()); student.setAddress(clean(request.studentAddress()));
        student = studentRepository.save(student);

        String parentPhone = request.parentPhone().trim();
        User parentUser = userRepository.findByPhone(parentPhone).orElse(null);
        boolean parentReused = parentUser != null;
        Parent parent;
        if (parentReused) {
            if (parentUser.getRole() != UserRole.PARENT) throw new ConflictException("Số điện thoại phụ huynh đang thuộc tài khoản vai trò khác");
            parent = parentRepository.findByUserId(parentUser.getId())
                .orElseThrow(() -> new ConflictException("Tài khoản phụ huynh chưa có hồ sơ hợp lệ"));
        } else {
            String email = clean(request.parentEmail());
            if (email != null && userRepository.existsByEmail(email)) throw new ConflictException("Email phụ huynh đã tồn tại");
            requireUniqueCitizen(request.parentCitizenId());
            parentUser = newUser(parentPhone, request.parentName(), email, request.parentCitizenId(), UserRole.PARENT);
            parent = new Parent(); parent.setUser(parentUser); parent.setAddress(clean(request.parentAddress())); parent.setOccupation(clean(request.parentOccupation()));
            parent = parentRepository.save(parent);
        }

        StudentGuardian guardian = new StudentGuardian(); guardian.setStudent(student); guardian.setGuardian(parent); guardian.setRelationship(request.relationship());
        guardianRepository.save(guardian);
        Enrollment enrollment = new Enrollment(); enrollment.setStudent(student); enrollment.setCls(cls); enrollment.setAcademicYear(year);
        enrollment.setJoinDate(year.getStartDate()); enrollment.setStatus(EnrollmentStatus.ACTIVE); enrollmentRepository.save(enrollment);
        return new StudentEnrollmentResultDto(student.getId(), code, cls.getId(), cls.getName(), parent.getId(), parentReused);
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
                student.getId(), student.getStudentCode(), student.getUser().getName(), student.getUser().getPhone(),
                guardianRepository.findByStudentId(student.getId()).stream()
                    .map(link -> new StudentAccountByClassDto.GuardianAccountDto(
                        link.getGuardian().getId(), link.getGuardian().getUser().getName(),
                        link.getGuardian().getUser().getPhone(), link.getGuardian().getUser().getEmail(),
                        link.getRelationship()))
                    .toList()))
            .toList();
    }

    private User newUser(String login, String name, String email, String citizenId, UserRole role) {
        User user = new User(); user.setPhone(login); user.setName(name.trim()); user.setEmail(clean(email)); user.setCitizenId(clean(citizenId));
        user.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD)); user.setMustChangePassword(true); user.setRole(role); user.setStatus(UserStatus.ACTIVE);
        return userRepository.save(user);
    }
    private void requireUniqueCitizen(String value) { String id = clean(value); if (id != null && userRepository.existsByCitizenId(id)) throw new ConflictException("CCCD/định danh đã tồn tại"); }
    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
