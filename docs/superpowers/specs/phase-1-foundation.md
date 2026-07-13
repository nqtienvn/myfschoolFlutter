# Phase 1 — Foundation (Auth + Users + Reference Data)

> Tables: `users`, `user_settings`, `parents`, `students`, `teachers`, `classes`, `subjects`, `semesters`, `student_guardians`, `student_classes`, `class_subjects`
> Tổng: **11 tables**, **~35 API endpoints**

---

## 1A. Authentication + User Management

### 1A.1. Entities

#### `User`

```java
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(nullable = false, unique = true, length = 15)
    private String phone;

    @Column(nullable = false)
    private String password;  // BCrypt hash

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;  // PARENT | STUDENT | TEACHER

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    // Relations: 1:1 UserSetting, 1:1 Parent/Student/Teacher (conditional)
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private UserSetting userSetting;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Parent parent;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Student student;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Teacher teacher;
}
```

#### `UserSetting`

```java
@Entity
@Table(name = "user_settings")
public class UserSetting extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true,
                foreignKey = @ForeignKey(name = "fk_us_user"))
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Theme theme = Theme.LIGHT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Language language = Language.VI;

    @Column(nullable = false)
    private Boolean notificationEnabled = true;
}
```

#### `Parent`

```java
@Entity
@Table(name = "parents")
public class Parent extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true,
                foreignKey = @ForeignKey(name = "fk_parents_user"))
    private User user;

    @Column(length = 500)
    private String address;

    @Column(length = 200)
    private String occupation;

    @OneToMany(mappedBy = "guardian", fetch = FetchType.LAZY)
    private List<StudentGuardian> studentGuardians = new ArrayList<>();
}
```

#### `Student`

```java
@Entity
@Table(name = "students")
public class Student extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true,
                foreignKey = @ForeignKey(name = "fk_students_user"))
    private User user;

    @Column(nullable = false, unique = true, length = 20)
    private String studentCode;  // VD: 12A-01

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id",
                foreignKey = @ForeignKey(name = "fk_students_class"))
    private Class currentClass;

    @Column(columnDefinition = "DATE")
    private LocalDate dateOfBirth;

    @OneToMany(mappedBy = "student", fetch = FetchType.LAZY)
    private List<StudentGuardian> studentGuardians = new ArrayList<>();

    @OneToMany(mappedBy = "student", fetch = FetchType.LAZY)
    private List<StudentClass> studentClasses = new ArrayList<>();
}
```

#### `Teacher`

```java
@Entity
@Table(name = "teachers")
public class Teacher extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true,
                foreignKey = @ForeignKey(name = "fk_teachers_user"))
    private User user;

    @Column(nullable = false, unique = true, length = 20)
    private String employeeCode;  // VD: GV001

    @Column(length = 100)
    private String department;  // VD: PRM393 - SE1913
}
```

---

### 1A.2. DTOs

#### Request DTOs

```java
// --- Login ---
public record LoginRequest(
    @NotBlank @Size(min = 10, max = 15) String phone,
    @NotBlank @Size(min = 6, max = 100) String password
) {}

// --- Register ---
public record RegisterRequest(
    @NotBlank @Size(min = 10, max = 15) String phone,
    @NotBlank @Size(min = 6, max = 100) String password,
    @NotBlank @Size(max = 100) String name,
    @Email String email,
    @NotNull UserRole role,    // PARENT | STUDENT | TEACHER
    // Role-specific fields (optional based on role)
    String studentCode,        // STUDENT only
    String employeeCode,       // TEACHER only
    String department,         // TEACHER only
    String address,            // PARENT only
    String occupation          // PARENT only
) {}

// --- Change Password ---
public record ChangePasswordRequest(
    @NotBlank String oldPassword,
    @NotBlank @Size(min = 6, max = 100) String newPassword
) {}

// --- Update Profile ---
public record UpdateProfileRequest(
    @Size(max = 100) String name,
    @Email String email,
    // Parent-specific
    String address,
    String occupation
) {}

// --- Update Settings ---
public record UpdateSettingsRequest(
    Theme theme,
    Language language,
    Boolean notificationEnabled
) {}
```

#### Response DTOs

```java
// --- Login Response ---
public record LoginResponse(
    String token,
    String tokenType,   // "Bearer"
    Long expiresIn,    // seconds
    UserDto user
) {}

// --- User Profile ---
public record UserDto(
    Long id,
    String phone,
    String name,
    String email,
    UserRole role,
    UserStatus status,
    LocalDateTime createdAt,
    // Role-specific profile
    ParentDto parentProfile,   // nullable
    StudentDto studentProfile, // nullable
    TeacherDto teacherProfile, // nullable
    UserSettingDto settings
) {}

// --- Parent Profile ---
public record ParentDto(
    Long id,
    String address,
    String occupation,
    List<StudentSummaryDto> children  // danh sách con
) {}

// --- Student Profile ---
public record StudentDto(
    Long id,
    String studentCode,
    String className,
    Long classId,
    LocalDate dateOfBirth
) {}

// --- Teacher Profile ---
public record TeacherDto(
    Long id,
    String employeeCode,
    String department
) {}

// --- Settings ---
public record UserSettingDto(
    Theme theme,
    Language language,
    Boolean notificationEnabled
) {}

// --- Student Summary (for parent's children list) ---
public record StudentSummaryDto(
    Long id,
    String name,
    String studentCode,
    String className
) {}
```

---

### 1A.3. Repository

```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByPhone(String phone);

    boolean existsByPhone(String phone);

    boolean existsByPhoneAndIdNot(String phone, Long id);

    List<User> findByRole(UserRole role);

    @Query("SELECT u FROM User u WHERE u.role = :role AND " +
           "(LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "u.phone LIKE CONCAT('%', :keyword, '%'))")
    List<User> searchByRoleAndKeyword(@Param("role") UserRole role,
                                       @Param("keyword") String keyword);
}

@Repository
public interface ParentRepository extends JpaRepository<Parent, Long> {
    Optional<Parent> findByUserId(Long userId);

    @Query("SELECT sg.student FROM StudentGuardian sg WHERE sg.guardian.id = :parentId")
    List<Student> findChildrenByParentId(@Param("parentId") Long parentId);
}

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByUserId(Long userId);

    Optional<Student> findByStudentCode(String studentCode);

    List<Student> findByCurrentClassId(Long classId);

    @Query("SELECT s FROM Student s WHERE s.currentClass.id = :classId AND " +
           "(LOWER(s.user.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "s.studentCode LIKE CONCAT('%', :keyword, '%'))")
    List<Student> searchByClassAndKeyword(@Param("classId") Long classId,
                                           @Param("keyword") String keyword);

    boolean existsByStudentCode(String studentCode);
}

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long> {
    Optional<Teacher> findByUserId(Long userId);

    Optional<Teacher> findByEmployeeCode(String employeeCode);

    boolean existsByEmployeeCode(String employeeCode);
}

@Repository
public interface UserSettingRepository extends JpaRepository<UserSetting, Long> {
    Optional<UserSetting> findByUserId(Long userId);
}

@Repository
public interface StudentGuardianRepository extends JpaRepository<StudentGuardian, Long> {

    List<StudentGuardian> findByGuardianId(Long guardianId);

    List<StudentGuardian> findByStudentId(Long studentId);

    Optional<StudentGuardian> findByStudentIdAndGuardianId(Long studentId, Long guardianId);

    boolean existsByStudentIdAndGuardianId(Long studentId, Long guardianId);

    @Query("SELECT sg.guardian FROM StudentGuardian sg WHERE sg.student.id = :studentId")
    List<Parent> findGuardiansByStudentId(@Param("studentId") Long studentId);
}
```

---

### 1A.4. Service

```java
@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final ParentRepository parentRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final UserSettingRepository userSettingRepository;
    private final StudentGuardianRepository studentGuardianRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    // --- Login ---
    // 1. Find user by phone
    // 2. Verify password with BCrypt
    // 3. Check status == ACTIVE
    // 4. Generate JWT with (userId, role, name)
    // 5. Return LoginResponse
    public LoginResponse login(LoginRequest request) { ... }

    // --- Register ---
    // 1. Validate phone unique
    // 2. Hash password
    // 3. Create User entity
    // 4. Based on role, create Parent/Student/Teacher sub-entity
    // 5. Create UserSetting with defaults
    // 6. If STUDENT: validate studentCode unique, link to class if classId provided
    // 7. If TEACHER: validate employeeCode unique
    // 8. Save all, return LoginResponse (auto-login after register)
    public LoginResponse register(RegisterRequest request) { ... }

    // --- Get Profile ---
    // 1. Find user by JWT userId
    // 2. Load role-specific profile (eager fetch relations)
    // 3. If PARENT: load children list via StudentGuardian
    // 4. Return UserDto
    public UserDto getProfile(Long userId) { ... }

    // --- Change Password ---
    // 1. Verify old password
    // 2. Hash new password
    // 3. Save
    public void changePassword(Long userId, ChangePasswordRequest request) { ... }

    // --- Update Profile ---
    // 1. Update name and email on User
    // 2. If PARENT: update address, occupation
    // 3. Save
    public UserDto updateProfile(Long userId, UpdateProfileRequest request) { ... }

    // --- Update Settings ---
    // 1. Find or create UserSetting
    // 2. Update theme, language, notificationEnabled
    // 3. Save
    public UserSettingDto updateSettings(Long userId, UpdateSettingsRequest request) { ... }

    // --- Link Guardian to Student (PARENT only) ---
    // 1. Validate parent exists and role == PARENT
    // 2. Validate student exists
    // 3. Check unique constraint (student_id, guardian_id)
    // 4. Create StudentGuardian with relationship
    public void linkGuardianStudent(Long parentId, Long studentId, Relationship relationship) { ... }
}
```

**Business Rules:**
- Phone number unique toàn hệ thống
- Password tối thiểu 6 ký tự, BCrypt hash
- Khi register STUDENT, studentCode phải unique
- Khi register TEACHER, employeeCode phải unique
- User mới tạo sẽ có UserSetting mặc định (LIGHT, VI, notification ON)
- PARENT register sẽ tự tạo Parent record + linkChildren nếu có studentIds

---

### 1A.5. Controller

```java
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Xác thực & Quản lý tài khoản")
public class AuthController {

    private final AuthService authService;

    // POST /api/auth/login
    // Public
    // Body: LoginRequest
    // Response: ApiResponse<LoginResponse>
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
        @Valid @RequestBody LoginRequest request) { ... }

    // POST /api/auth/register
    // Public
    // Body: RegisterRequest
    // Response: ApiResponse<LoginResponse>
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<LoginResponse>> register(
        @Valid @RequestBody RegisterRequest request) { ... }
}

@RestController
@RequestMapping("/api/user")
@Tag(name = "User", description = "Quản lý hồ sơ cá nhân")
@SecurityRequirement(name = "Bearer Authentication")
public class UserController {

    // GET /api/user/profile
    // Authenticated (any role)
    // Response: ApiResponse<UserDto>
    @GetMapping("/profile")
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT', 'TEACHER')")
    public ResponseEntity<ApiResponse<UserDto>> getProfile() { ... }

    // PUT /api/user/profile
    // Authenticated (any role)
    // Body: UpdateProfileRequest
    // Response: ApiResponse<UserDto>
    @PutMapping("/profile")
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT', 'TEACHER')")
    public ResponseEntity<ApiResponse<UserDto>> updateProfile(
        @Valid @RequestBody UpdateProfileRequest request) { ... }

    // PUT /api/user/password
    // Authenticated (any role)
    // Body: ChangePasswordRequest
    // Response: ApiResponse<Void>
    @PutMapping("/password")
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT', 'TEACHER')")
    public ResponseEntity<ApiResponse<Void>> changePassword(
        @Valid @RequestBody ChangePasswordRequest request) { ... }

    // GET /api/user/settings
    // Authenticated (any role)
    // Response: ApiResponse<UserSettingDto>
    @GetMapping("/settings")
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT', 'TEACHER')")
    public ResponseEntity<ApiResponse<UserSettingDto>> getSettings() { ... }

    // PUT /api/user/settings
    // Authenticated (any role)
    // Body: UpdateSettingsRequest
    // Response: ApiResponse<UserSettingDto>
    @PutMapping("/settings")
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT', 'TEACHER')")
    public ResponseEntity<ApiResponse<UserSettingDto>> updateSettings(
        @Valid @RequestBody UpdateSettingsRequest request) { ... }
}
```

---

### 1A.6. Security Config

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/ws/**").permitAll()  // WebSocket handled separately
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

```java
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;  // ms

    // Generate JWT
    public String generateToken(Long userId, UserRole role, String name) { ... }

    // Validate JWT
    public boolean validateToken(String token) { ... }

    // Extract userId from JWT
    public Long getUserIdFromToken(String token) { ... }

    // Extract role from JWT
    public UserRole getRoleFromToken(String token) { ... }
}
```

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) {
        // 1. Extract token from Authorization header
        // 2. Validate token
        // 3. Load UserDetails
        // 4. Set SecurityContext
        // 5. Continue filter chain
    }
}
```

---

## 1B. Reference Data CRUD

### 1B.1. Entities

#### `vn.edu.fpt.myfschool.entity.Class`

```java
@Entity
@Table(name = "classes",
       uniqueConstraints = @UniqueConstraint(columnNames = {"name", "academic_year"}))
public class Class extends BaseEntity {

    @Column(nullable = false, length = 20)
    private String name;  // VD: 12A

    @Column(nullable = false)
    private Integer gradeLevel;  // 1-12

    @Column(nullable = false, length = 9)
    private String academicYear;  // VD: 2026-2027

    @Column(nullable = false, length = 200)
    private String schoolName = "FPT Schools";

    @OneToMany(mappedBy = "currentClass", fetch = FetchType.LAZY)
    private List<Student> students = new ArrayList<>();

    @OneToMany(mappedBy = "cls", fetch = FetchType.LAZY)
    private List<ClassSubject> classSubjects = new ArrayList<>();
}
```

#### `Subject`

```java
@Entity
@Table(name = "subjects")
public class Subject extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;  // VD: Toán

    @Column(nullable = false, unique = true, length = 20)
    private String code;  // VD: TOAN12
}
```

#### `Semester`

```java
@Entity
@Table(name = "semesters",
       uniqueConstraints = @UniqueConstraint(columnNames = {"name", "academic_year"}))
public class Semester extends BaseEntity {

    @Column(nullable = false, length = 50)
    private String name;  // VD: HK I

    @Column(nullable = false, length = 9)
    private String academicYear;  // VD: 2026-2027

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private Boolean isCurrent = false;
}
```

#### `StudentGuardian`

```java
@Entity
@Table(name = "student_guardians",
       uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "guardian_id"}))
public class StudentGuardian extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_sg_student"))
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guardian_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_sg_guardian"))
    private Parent guardian;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Relationship relationship;  // FATHER | MOTHER | GUARDIAN
}
```

#### `StudentClass`

```java
@Entity
@Table(name = "student_classes",
       uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "academic_year"}))
public class StudentClass extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_sc_student"))
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_sc_class"))
    private Class cls;

    @Column(nullable = false, length = 9)
    private String academicYear;
}
```

#### `ClassSubject`

```java
@Entity
@Table(name = "class_subjects",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"class_id", "subject_id", "academic_year"}))
public class ClassSubject extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_cs_class"))
    private Class cls;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_cs_subject"))
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_cs_teacher"))
    private Teacher teacher;

    @Column(nullable = false)
    private Boolean isHomeroom = false;

    @Column(nullable = false, length = 9)
    private String academicYear;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id",
                foreignKey = @ForeignKey(name = "fk_cs_semester"))
    private Semester semester;  // nullable
}
```

---

### 1B.2. DTOs

#### Request DTOs

```java
// --- Class ---
public record CreateClassRequest(
    @NotBlank @Size(max = 20) String name,
    @NotNull @Min(1) @Max(12) Integer gradeLevel,
    @NotBlank @Size(max = 9) String academicYear,
    @Size(max = 200) String schoolName
) {}

public record UpdateClassRequest(
    @Size(max = 20) String name,
    @Min(1) @Max(12) Integer gradeLevel,
    @Size(max = 9) String academicYear,
    @Size(max = 200) String schoolName
) {}

// --- Subject ---
public record CreateSubjectRequest(
    @NotBlank @Size(max = 100) String name,
    @NotBlank @Size(max = 20) String code
) {}

// --- Semester ---
public record CreateSemesterRequest(
    @NotBlank @Size(max = 50) String name,
    @NotBlank @Size(max = 9) String academicYear,
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate,
    Boolean isCurrent
) {}

// --- ClassSubject ---
public record CreateClassSubjectRequest(
    @NotNull Long classId,
    @NotNull Long subjectId,
    @NotNull Long teacherId,
    Boolean isHomeroom,
    @NotBlank @Size(max = 9) String academicYear,
    Long semesterId
) {}

// --- StudentGuardian ---
public record LinkGuardianRequest(
    @NotNull Long studentId,
    @NotNull Long guardianId,
    @NotNull Relationship relationship
) {}
```

#### Response DTOs

```java
// --- Class ---
public record ClassDto(
    Long id,
    String name,
    Integer gradeLevel,
    String academicYear,
    String schoolName,
    Integer studentCount,
    LocalDateTime createdAt
) {}

// --- Class Detail (includes students + subjects) ---
public record ClassDetailDto(
    Long id,
    String name,
    Integer gradeLevel,
    String academicYear,
    String schoolName,
    List<StudentSummaryDto> students,
    List<ClassSubjectDto> subjects
) {}

public record ClassSubjectDto(
    Long id,
    SubjectDto subject,
    TeacherSummaryDto teacher,
    Boolean isHomeroom
) {}

// --- Subject ---
public record SubjectDto(
    Long id,
    String name,
    String code
) {}

// --- Semester ---
public record SemesterDto(
    Long id,
    String name,
    String academicYear,
    LocalDate startDate,
    LocalDate endDate,
    Boolean isCurrent
) {}

// --- Teacher Summary (for class subject listing) ---
public record TeacherSummaryDto(
    Long id,
    String name,
    String employeeCode,
    String department
) {}
```

---

### 1B.3. Repositories

```java
@Repository
public interface ClassRepository extends JpaRepository<Class, Long> {

    List<Class> findByAcademicYear(String academicYear);

    Optional<Class> findByNameAndAcademicYear(String name, String academicYear);

    boolean existsByNameAndAcademicYear(String name, String academicYear);

    @Query("SELECT c FROM Class c WHERE c.academicYear = :year AND " +
           "(LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Class> searchByYearAndKeyword(@Param("year") String year,
                                        @Param("keyword") String keyword);

    // Lớp mà teacher dạy (qua class_subjects)
    @Query("SELECT DISTINCT cs.cls FROM ClassSubject cs " +
           "WHERE cs.teacher.id = :teacherId AND cs.academicYear = :year")
    List<Class> findClassesByTeacher(@Param("teacherId") Long teacherId,
                                      @Param("year") String year);
}

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {

    Optional<Subject> findByCode(String code);

    boolean existsByCode(String code);

    @Query("SELECT s FROM Subject s WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(s.code) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Subject> search(@Param("keyword") String keyword);
}

@Repository
public interface SemesterRepository extends JpaRepository<Semester, Long> {

    List<Semester> findByAcademicYear(String academicYear);

    Optional<Semester> findByIsCurrentTrue();

    Optional<Semester> findByNameAndAcademicYear(String name, String academicYear);
}

@Repository
public interface ClassSubjectRepository extends JpaRepository<ClassSubject, Long> {

    List<ClassSubject> findByClsIdAndAcademicYear(Long classId, String academicYear);

    List<ClassSubject> findByTeacherIdAndAcademicYear(Long teacherId, String academicYear);

    Optional<ClassSubject> findByClsIdAndSubjectIdAndAcademicYear(
        Long classId, Long subjectId, String academicYear);

    // Subjects taught by teacher across all classes
    @Query("SELECT DISTINCT cs.subject FROM ClassSubject cs " +
           "WHERE cs.teacher.id = :teacherId AND cs.academicYear = :year")
    List<Subject> findSubjectsByTeacher(@Param("teacherId") Long teacherId,
                                         @Param("year") String year);
}

@Repository
public interface StudentClassRepository extends JpaRepository<StudentClass, Long> {

    List<StudentClass> findByStudentId(Long studentId);

    Optional<StudentClass> findByStudentIdAndAcademicYear(Long studentId, String academicYear);

    List<StudentClass> findByClsIdAndAcademicYear(Long classId, String academicYear);
}
```

---

### 1B.4. Services

```java
@Service
@Transactional
public class ClassService {

    // --- List Classes ---
    // Filter: academicYear (optional), keyword (optional)
    // PAGING support
    public PagedResponse<ClassDto> listClasses(String academicYear,
                                                String keyword,
                                                int page, int size) { ... }

    // --- Get Class Detail ---
    // Include students list + classSubjects list
    public ClassDetailDto getClassDetail(Long classId) { ... }

    // --- Create Class ---
    // TEACHER only (admin)
    // Validate: unique (name, academicYear)
    public ClassDto createClass(CreateClassRequest request) { ... }

    // --- Update Class ---
    // TEACHER only
    public ClassDto updateClass(Long classId, UpdateClassRequest request) { ... }

    // --- Delete Class ---
    // TEACHER only
    // Check: no students assigned (RESTRICT)
    public void deleteClass(Long classId) { ... }

    // --- Get Students in Class ---
    public List<StudentSummaryDto> getStudentsInClass(Long classId) { ... }

    // --- Get Classes for Teacher ---
    // Find classes where teacher is assigned (via class_subjects)
    public List<ClassDto> getClassesForTeacher(Long teacherId, String academicYear) { ... }

    // --- Assign Subject to Class ---
    // TEACHER only
    // Validate: teacher exists, subject exists, class exists, unique constraint
    public ClassSubjectDto assignSubject(CreateClassSubjectRequest request) { ... }

    // --- Remove Subject from Class ---
    public void removeSubject(Long classSubjectId) { ... }
}

@Service
@Transactional
public class SubjectService {

    public List<SubjectDto> listSubjects(String keyword) { ... }

    public SubjectDto getSubject(Long id) { ... }

    public SubjectDto createSubject(CreateSubjectRequest request) { ... }

    public SubjectDto updateSubject(Long id, CreateSubjectRequest request) { ... }

    public void deleteSubject(Long id) { ... }

    // Subjects assigned to a class
    public List<ClassSubjectDto> getSubjectsForClass(Long classId, String academicYear) { ... }

    // Subjects taught by a teacher
    public List<SubjectDto> getSubjectsForTeacher(Long teacherId, String academicYear) { ... }
}

@Service
@Transactional
public class SemesterService {

    public List<SemesterDto> listSemesters(String academicYear) { ... }

    public SemesterDto getCurrentSemester() { ... }

    public SemesterDto getSemester(Long id) { ... }

    public SemesterDto createSemester(CreateSemesterRequest request) { ... }

    public SemesterDto updateSemester(Long id, CreateSemesterRequest request) { ... }

    // Set isCurrent = true for this semester, false for all others
    public void setCurrentSemester(Long semesterId) { ... }
}
```

**Business Rules:**
- Class name + academicYear phải unique
- Subject code phải unique
- Không thể xóa Class nếu có Students đang thuộc lớp (RESTRICT FK)
- Semester chỉ có 1 cái `isCurrent = true` tại 1 thời điểm
- Teacher assign vào ClassSubject phải có role == TEACHER
- Khi tạo ClassSubject, check unique (class_id, subject_id, academic_year)

---

### 1B.5. Controllers

```java
@RestController
@RequestMapping("/api/classes")
@Tag(name = "Classes", description = "Quản lý lớp học")
@SecurityRequirement(name = "Bearer Authentication")
public class ClassController {

    private final ClassService classService;

    // GET /api/classes?academicYear=2026-2027&keyword=12A&page=0&size=20
    // TEACHER: all classes | PARENT/STUDENT: only their class
    @GetMapping
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT', 'TEACHER')")
    public ResponseEntity<ApiResponse<PagedResponse<ClassDto>>> listClasses(
        @RequestParam(required = false) String academicYear,
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) { ... }

    // GET /api/classes/{id}
    // TEACHER: any class | PARENT: only their child's class | STUDENT: only their class
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT', 'TEACHER')")
    public ResponseEntity<ApiResponse<ClassDetailDto>> getClassDetail(
        @PathVariable Long id) { ... }

    // POST /api/classes
    // TEACHER only
    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<ClassDto>> createClass(
        @Valid @RequestBody CreateClassRequest request) { ... }

    // PUT /api/classes/{id}
    // TEACHER only
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<ClassDto>> updateClass(
        @PathVariable Long id,
        @Valid @RequestBody UpdateClassRequest request) { ... }

    // DELETE /api/classes/{id}
    // TEACHER only
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<Void>> deleteClass(
        @PathVariable Long id) { ... }

    // GET /api/classes/{id}/students
    @GetMapping("/{id}/students")
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<StudentSummaryDto>>> getStudents(
        @PathVariable Long id) { ... }

    // POST /api/classes/{id}/subjects
    // TEACHER only
    @PostMapping("/{id}/subjects")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<ClassSubjectDto>> assignSubject(
        @PathVariable Long id,
        @Valid @RequestBody CreateClassSubjectRequest request) { ... }

    // DELETE /api/classes/subjects/{classSubjectId}
    @DeleteMapping("/subjects/{classSubjectId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<Void>> removeSubject(
        @PathVariable Long classSubjectId) { ... }
}

@RestController
@RequestMapping("/api/subjects")
@Tag(name = "Subjects", description = "Quản lý môn học")
@SecurityRequirement(name = "Bearer Authentication")
public class SubjectController {

    // GET /api/subjects?keyword=Toan
    @GetMapping
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<SubjectDto>>> listSubjects(
        @RequestParam(required = false) String keyword) { ... }

    // GET /api/subjects/{id}
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT', 'TEACHER')")
    public ResponseEntity<ApiResponse<SubjectDto>> getSubject(@PathVariable Long id) { ... }

    // POST /api/subjects — TEACHER only
    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<SubjectDto>> createSubject(
        @Valid @RequestBody CreateSubjectRequest request) { ... }

    // PUT /api/subjects/{id} — TEACHER only
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<SubjectDto>> updateSubject(
        @PathVariable Long id,
        @Valid @RequestBody CreateSubjectRequest request) { ... }

    // DELETE /api/subjects/{id} — TEACHER only
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<Void>> deleteSubject(@PathVariable Long id) { ... }
}

@RestController
@RequestMapping("/api/semesters")
@Tag(name = "Semesters", description = "Quản lý học kỳ")
@SecurityRequirement(name = "Bearer Authentication")
public class SemesterController {

    // GET /api/semesters?academicYear=2026-2027
    @GetMapping
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<SemesterDto>>> listSemesters(
        @RequestParam(required = false) String academicYear) { ... }

    // GET /api/semesters/current
    @GetMapping("/current")
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT', 'TEACHER')")
    public ResponseEntity<ApiResponse<SemesterDto>> getCurrentSemester() { ... }

    // GET /api/semesters/{id}
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT', 'TEACHER')")
    public ResponseEntity<ApiResponse<SemesterDto>> getSemester(
        @PathVariable Long id) { ... }

    // POST /api/semesters — TEACHER only
    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<SemesterDto>> createSemester(
        @Valid @RequestBody CreateSemesterRequest request) { ... }

    // PUT /api/semesters/{id} — TEACHER only
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<SemesterDto>> updateSemester(
        @PathVariable Long id,
        @Valid @RequestBody CreateSemesterRequest request) { ... }

    // PUT /api/semesters/{id}/set-current — TEACHER only
    @PutMapping("/{id}/set-current")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<Void>> setCurrentSemester(
        @PathVariable Long id) { ... }
}
```

---

## Phase 1 — Summary

### API Endpoints

| # | Method | Endpoint | Auth | Description |
|---|--------|----------|------|-------------|
| 1 | POST | `/api/auth/login` | Public | Đăng nhập |
| 2 | POST | `/api/auth/register` | Public | Đăng ký tài khoản |
| 3 | GET | `/api/user/profile` | Any | Xem hồ sơ |
| 4 | PUT | `/api/user/profile` | Any | Cập nhật hồ sơ |
| 5 | PUT | `/api/user/password` | Any | Đổi mật khẩu |
| 6 | GET | `/api/user/settings` | Any | Xem cài đặt |
| 7 | PUT | `/api/user/settings` | Any | Cập nhật cài đặt |
| 8 | GET | `/api/classes` | Any | Danh sách lớp |
| 9 | GET | `/api/classes/{id}` | Any | Chi tiết lớp |
| 10 | POST | `/api/classes` | TEACHER | Tạo lớp |
| 11 | PUT | `/api/classes/{id}` | TEACHER | Sửa lớp |
| 12 | DELETE | `/api/classes/{id}` | TEACHER | Xóa lớp |
| 13 | GET | `/api/classes/{id}/students` | Any | Học sinh trong lớp |
| 14 | POST | `/api/classes/{id}/subjects` | TEACHER | Assign môn |
| 15 | DELETE | `/api/classes/subjects/{id}` | TEACHER | Gỡ môn |
| 16 | GET | `/api/subjects` | Any | Danh sách môn |
| 17 | GET | `/api/subjects/{id}` | Any | Chi tiết môn |
| 18 | POST | `/api/subjects` | TEACHER | Tạo môn |
| 19 | PUT | `/api/subjects/{id}` | TEACHER | Sửa môn |
| 20 | DELETE | `/api/subjects/{id}` | TEACHER | Xóa môn |
| 21 | GET | `/api/semesters` | Any | Danh sách HK |
| 22 | GET | `/api/semesters/current` | Any | HK hiện tại |
| 23 | GET | `/api/semesters/{id}` | Any | Chi tiết HK |
| 24 | POST | `/api/semesters` | TEACHER | Tạo HK |
| 25 | PUT | `/api/semesters/{id}` | TEACHER | Sửa HK |
| 26 | PUT | `/api/semesters/{id}/set-current` | TEACHER | Set HK hiện tại |

### Flyway Migration: `V1__init.sql`

```sql
-- Copy toàn bộ DDL từ docs/database.md (27 tables)
-- Bao gồm: CREATE TABLE, INDEX, CONSTRAINT
-- KHÔNG include data.sql (sample data load riêng)
```

---

*Tiếp tục: [Phase 2 — Academic](phase-2-academic.md)*
