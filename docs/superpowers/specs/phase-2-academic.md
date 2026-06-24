# Phase 2 — Academic (Grades + Attendance + Leave Requests)

> Tables: `grades`, `semester_results`, `attendance`, `leave_requests`
> Tổng: **4 tables**, **~40 API endpoints**

---

## 2A. Grades

### 2A.1. Entity

```java
@Entity
@Table(name = "grades",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"student_id", "subject_id", "semester_id"}))
public class Grade extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_grades_student"))
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_grades_subject"))
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_grades_semester"))
    private Semester semester;

    @Column(precision = 3, scale = 2)
    private BigDecimal oral;       // Điểm miệng

    @Column(name = "quiz_15m", precision = 3, scale = 2)
    private BigDecimal quiz15m;   // Điểm 15 phút

    @Column(name = "mid_term", precision = 3, scale = 2)
    private BigDecimal midTerm;   // Điểm 1 tiết

    @Column(precision = 3, scale = 2)
    private BigDecimal final;     // Điểm học kỳ

    @Column(precision = 3, scale = 2)
    private BigDecimal average;   // TBM môn (computed)
}
```

### 2A.2. DTOs

#### Request

```java
// --- Single Grade Update (GV nhập điểm 1 môn cho 1 HS) ---
public record UpdateGradeRequest(
    @NotNull Long studentId,
    @NotNull Long subjectId,
    @NotNull Long semesterId,
    @DecimalMin("0") @DecimalMax("10") BigDecimal oral,
    @DecimalMin("0") @DecimalMax("10") BigDecimal quiz15m,
    @DecimalMin("0") @DecimalMax("10") BigDecimal midTerm,
    @DecimalMin("0") @DecimalMax("10") BigDecimal finalScore   // "final" is reserved keyword
) {}

// --- Batch Grade Update (GV nhập điểm nhiều HS 1 môn) ---
public record BatchGradeUpdateRequest(
    @NotNull Long subjectId,
    @NotNull Long semesterId,
    @NotEmpty List<GradeEntry> grades
) {}

public record GradeEntry(
    @NotNull Long studentId,
    @DecimalMin("0") @DecimalMax("10") BigDecimal oral,
    @DecimalMin("0") @DecimalMax("10") BigDecimal quiz15m,
    @DecimalMin("0") @DecimalMax("10") BigDecimal midTerm,
    @DecimalMin("0") @DecimalMax("10") BigDecimal finalScore
) {}

// --- Simulation (PH/HS preview thay đổi điểm) ---
public record GradeSimulationRequest(
    @NotNull Long semesterId,
    List<SimulationEntry> simulations
) {}

public record SimulationEntry(
    @NotNull Long subjectId,
    BigDecimal oral,
    BigDecimal quiz15m,
    BigDecimal midTerm,
    BigDecimal finalScore,
    String conduct   // Hạnh kiểm giả lập: Tốt/Khá/TB/Yếu
) {}
```

#### Response

```java
// --- Grade for 1 subject ---
public record GradeDto(
    Long id,
    Long subjectId,
    String subjectName,
    String subjectCode,
    BigDecimal oral,
    BigDecimal quiz15m,
    BigDecimal midTerm,
    BigDecimal finalScore,
    BigDecimal average
) {}

// --- Student's all grades in a semester ---
public record StudentSemesterGradesDto(
    Long studentId,
    String studentName,
    String studentCode,
    Long semesterId,
    String semesterName,
    String academicYear,
    List<GradeDto> grades,
    SemesterResultDto summary   // GPA, rank, honor, etc.
) {}

// --- Teacher view: all students' grades for 1 subject in a semester ---
public record SubjectGradesDto(
    Long subjectId,
    String subjectName,
    String subjectCode,
    Long semesterId,
    String semesterName,
    Long classId,
    String className,
    List<StudentGradeRowDto> studentGrades
) {}

public record StudentGradeRowDto(
    Long studentId,
    String studentName,
    String studentCode,
    BigDecimal oral,
    BigDecimal quiz15m,
    BigDecimal midTerm,
    BigDecimal finalScore,
    BigDecimal average
) {}

// --- Simulation Result (computed, not saved) ---
public record SimulationResultDto(
    List<GradeDto> simulatedGrades,
    BigDecimal simulatedGpa,
    String simulatedAcademicAbility,
    String simulatedConduct,
    Integer simulatedRank
) {}
```

### 2A.3. Repository

```java
@Repository
public interface GradeRepository extends JpaRepository<Grade, Long> {

    // PH/HS xem điểm HK: WHERE student_id=? AND semester_id=?
    List<Grade> findByStudentIdAndSemesterId(Long studentId, Long semesterId);

    // GV xem điểm môn: WHERE subject_id=? AND semester_id=?
    List<Grade> findBySubjectIdAndSemesterId(Long subjectId, Long semesterId);

    // GV xem điểm 1 môn + 1 lớp: JOIN qua students.class_id
    @Query("SELECT g FROM Grade g " +
           "JOIN g.student s " +
           "WHERE g.subject.id = :subjectId AND g.semester.id = :semesterId " +
           "AND s.currentClass.id = :classId")
    List<Grade> findBySubjectSemesterClass(@Param("subjectId") Long subjectId,
                                            @Param("semesterId") Long semesterId,
                                            @Param("classId") Long classId);

    Optional<Grade> findByStudentIdAndSubjectIdAndSemesterId(
        Long studentId, Long subjectId, Long semesterId);

    boolean existsByStudentIdAndSubjectIdAndSemesterId(
        Long studentId, Long subjectId, Long semesterId);
}
```

### 2A.4. Service

```java
@Service
@Transactional
public class GradeService {

    private final GradeRepository gradeRepository;
    private final SemesterResultRepository semesterResultRepository;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final SemesterRepository semesterRepository;
    private final AttendanceRepository attendanceRepository;

    // ============================
    // GV FUNCTIONS
    // ============================

    // --- Get grades for teacher's subject ---
    // Input: subjectId, semesterId, classId
    // Returns: SubjectGradesDto (all students in class with their grades)
    // Only TEACHER who is assigned to this subject+class can access
    public SubjectGradesDto getSubjectGrades(Long subjectId, Long semesterId,
                                              Long classId, Long teacherId) {
        // 1. Verify teacher is assigned to this subject+class via class_subjects
        // 2. Load all students in class
        // 3. Load grades for subject+semester
        // 4. Merge: students without grade record → all nulls
        // 5. Return SubjectGradesDto
    }

    // --- Update single grade ---
    // GV nhập/sửa điểm 1 HS 1 môn
    // Auto-recalculate average after save
    // Auto-recalculate semester_results after save
    public GradeDto updateGrade(UpdateGradeRequest request, Long teacherId) {
        // 1. Verify teacher permission (class_subjects)
        // 2. Find or create Grade (upsert by student+subject+semester)
        // 3. Set values
        // 4. Calculate average
        // 5. Save
        // 6. Trigger semester result recalculation
        // 7. Return GradeDto
    }

    // --- Batch update grades ---
    // GV import điểm nhiều HS 1 môn (từ Excel hoặc form)
    public List<GradeDto> batchUpdateGrades(BatchGradeUpdateRequest request,
                                             Long teacherId) {
        // 1. For each entry: upsert grade
        // 2. Recalculate all averages
        // 3. Recalculate semester result
        // 4. Return updated grades
    }

    // ============================
    // PARENT/STUDENT FUNCTIONS
    // ============================

    // --- Get student's grades for a semester ---
    // PARENT: can only view their children's grades
    // STUDENT: can only view their own grades
    public StudentSemesterGradesDto getStudentGrades(Long studentId,
                                                      Long semesterId,
                                                      Long requestUserId) {
        // 1. Verify access (parent→guardian check, or student→self)
        // 2. Load all grades for student+semester
        // 3. Load semester result (GPA, rank, etc.)
        // 4. Return StudentSemesterGradesDto
    }

    // --- Simulation (client-side preview) ---
    // PH/HS simulate grade changes without saving to DB
    // Returns computed result (new average, GPA, rank, etc.)
    public SimulationResultDto simulateGrades(Long studentId,
                                               GradeSimulationRequest request) {
        // 1. Load existing grades
        // 2. Overlay simulated values
        // 3. Calculate new averages per subject
        // 4. Calculate new GPA (avg of all subject averages)
        // 5. Determine academic ability from GPA
        // 6. Calculate rank (requires counting students with higher GPA)
        // 7. Return SimulationResultDto (NOT persisted)
    }

    // ============================
    // COMPUTATION
    // ============================

    // --- Calculate subject average ---
    // Formula: (oral + quiz15m*2 + midTerm*3 + final*4) / 10
    // Only uses non-null values; if all null → average = null
    private BigDecimal calculateSubjectAverage(BigDecimal oral, BigDecimal quiz15m,
                                                BigDecimal midTerm, BigDecimal finalScore) {
        // Weighted average per DB stored procedure sp_calculate_subject_average
    }

    // --- Recalculate semester result ---
    // After any grade change, recalculate:
    // 1. GPA = AVG(all subject averages where average IS NOT NULL)
    // 2. academic_ability from GPA thresholds (≥8 Giỏi, ≥6.5 Khá, ≥5 TB, <5 Yếu)
    // 3. Rank = position in class by GPA descending
    // 4. Store in semester_results table
    private void recalculateSemesterResult(Long studentId, Long semesterId) {
        // 1. Load all grades for student+semester
        // 2. Compute GPA
        // 3. Find class from student.currentClass
        // 4. Count students in class with higher GPA → rank
        // 5. Upsert SemesterResult
    }
}
```

**Business Rules — Grades:**
- Grade values: 0.00 – 10.00, step 0.25 (DECIMAL(3,2))
- Average formula: `(oral + quiz15m×2 + midTerm×3 + final×4) / 10`
- If all components null → average = null (chưa có điểm)
- Partial null: only use non-null components in denominator
  - VD: chỉ có oral=8 + quiz15m=9 → `(8 + 9×2) / 3 = 8.67` (divide by 3 thay vì 10)
- **Quyết định**: Luôn chia cho 10, giá trị null = 0 trong tính toán
  → `(oral + quiz15m×2 + midTerm×3 + final×4) / 10`, null = 0
- Simulation mode: computed on-the-fly, KHÔNG lưu DB
- Only TEACHER assigned to subject+class có thể nhập/sửa điểm

---

## 2B. Semester Results

### 2B.1. Entity

```java
@Entity
@Table(name = "semester_results",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"student_id", "semester_id"}))
public class SemesterResult extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_sr_student"))
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_sr_semester"))
    private Semester semester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_sr_class"))
    private Class cls;

    @Column(precision = 3, scale = 2)
    private BigDecimal gpa;

    private Integer rank;

    @Column(length = 50)
    private String honor;  // Danh hiệu: Giỏi, Khá, TB

    @Column(length = 50)
    private String conduct;  // Hạnh kiểm: Tốt, Khá, TB, Yếu

    @Column(name = "academic_ability", length = 50)
    private String academicAbility;  // Học lực: Giỏi, Khá, TB, Yếu
}
```

### 2B.2. DTOs

```java
// --- Semester Result ---
public record SemesterResultDto(
    Long id,
    Long studentId,
    String studentName,
    Long semesterId,
    String semesterName,
    Long classId,
    String className,
    BigDecimal gpa,
    Integer rank,
    String honor,
    String conduct,
    String academicAbility
) {}

// --- Class ranking (for teacher) ---
public record ClassRankingDto(
    Long classId,
    String className,
    Long semesterId,
    String semesterName,
    List<ClassRankEntryDto> rankings
) {}

public record ClassRankEntryDto(
    Integer rank,
    Long studentId,
    String studentName,
    String studentCode,
    BigDecimal gpa,
    String academicAbility,
    String conduct
) {}
```

### 2B.3. Repository

```java
@Repository
public interface SemesterResultRepository extends JpaRepository<SemesterResult, Long> {

    Optional<SemesterResult> findByStudentIdAndSemesterId(Long studentId, Long semesterId);

    List<SemesterResult> findByClassIdAndSemesterIdOrderByRankAsc(Long classId, Long semesterId);

    List<SemesterResult> findBySemesterId(Long semesterId);

    // Find all semester results for a student (across semesters)
    List<SemesterResult> findByStudentIdOrderBySemesterIdDesc(Long studentId);

    // Upsert: find or create
    @Query("SELECT sr FROM SemesterResult sr WHERE sr.student.id = :studentId AND sr.semester.id = :semesterId")
    Optional<SemesterResult> findForUpdate(@Param("studentId") Long studentId,
                                            @Param("semesterId") Long semesterId);
}
```

### 2B.4. Service

```java
@Service
@Transactional
public class SemesterResultService {

    private final SemesterResultRepository semesterResultRepository;
    private final GradeRepository gradeRepository;
    private final StudentRepository studentRepository;

    // --- Get student's semester result ---
    public SemesterResultDto getStudentSemesterResult(Long studentId, Long semesterId) {
        // 1. Verify access
        // 2. Load or compute semester result
        // 3. Return SemesterResultDto
    }

    // --- Get class ranking ---
    // TEACHER only: xem xếp hạng cả lớp
    public ClassRankingDto getClassRanking(Long classId, Long semesterId) {
        // 1. Load all SemesterResult for class+semester, ordered by rank
        // 2. Return ClassRankingDto
    }

    // --- Recalculate (called by GradeService after grade changes) ---
    // 1. GPA = AVG(grades.average) where average IS NOT NULL
    // 2. academicAbility = CASE GPA thresholds
    // 3. Rank = COUNT(students in class with higher GPA) + 1
    // 4. honor = academicAbility (or separate rule)
    // 5. conduct = from attendance ratio (≥90% Tốt, ≥75% Khá, ≥60% TB, <60% Yếu)
    public void recalculate(Long studentId, Long semesterId) { ... }
}
```

**Business Rules — Semester Results:**
- GPA = AVG(all subject averages where not null)
- Academic ability: ≥8.0 Giỏi, ≥6.5 Khá, ≥5.0 TB, <5.0 Yếu
- Rank: determined by GPA descending within class; ties get same rank
- Conduct: derived from attendance ratio
  - ≥90% present → Tốt
  - ≥75% → Khá
  - ≥60% → Trung bình
  - <60% → Yếu
- Auto-recalculate whenever grades change

---

## 2C. Attendance

### 2C.1. Entity

```java
@Entity
@Table(name = "attendance",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"student_id", "date", "shift"}))
public class Attendance extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_att_student"))
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_att_class"))
    private Class cls;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_att_teacher"))
    private Teacher teacher;  // Người điểm danh

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id",
                foreignKey = @ForeignKey(name = "fk_att_schedule"))
    private Schedule schedule;  // optional

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leave_request_id",
                foreignKey = @ForeignKey(name = "fk_att_leave"))
    private LeaveRequest leaveRequest;  // optional

    @Column(nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Shift shift;  // MORNING | AFTERNOON

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AttendanceStatus status;
    // PRESENT | LATE | ABSENT_WITH_LEAVE | ABSENT_WITHOUT_LEAVE
}
```

### 2C.2. DTOs

```java
// --- Attendance record ---
public record AttendanceDto(
    Long id,
    Long studentId,
    String studentName,
    String studentCode,
    Long classId,
    String className,
    LocalDate date,
    Shift shift,
    AttendanceStatus status,
    Long leaveRequestId,     // nullable
    String teacherName,      // gv điểm danh
    LocalDateTime createdAt
) {}

// --- Teacher: daily attendance list for a class ---
public record DailyAttendanceDto(
    Long classId,
    String className,
    LocalDate date,
    Shift shift,
    List<AttendanceEntryDto> students
) {}

public record AttendanceEntryDto(
    Long studentId,
    String studentName,
    String studentCode,
    AttendanceStatus status,
    Long attendanceId   // null if not yet recorded
) {}

// --- Batch attendance (GV submit cả lớp) ---
public record SubmitAttendanceRequest(
    @NotNull Long classId,
    @NotNull LocalDate date,
    @NotNull Shift shift,
    @NotEmpty List<AttendanceEntry> entries
) {}

public record AttendanceEntry(
    @NotNull Long studentId,
    @NotNull AttendanceStatus status
) {}

// --- Student attendance stats ---
public record AttendanceStatsDto(
    Long studentId,
    String studentName,
    Long semesterId,
    String semesterName,
    int totalDays,
    int presentDays,
    int lateDays,
    int absentWithLeave,
    int absentWithoutLeave,
    double attendanceRate   //百分比
) {}

// --- Student attendance log ---
public record AttendanceLogDto(
    List<AttendanceDto> records,
    AttendanceStatsDto stats
) {}
```

### 2C.3. Repository

```java
@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    // GV xem điểm danh lớp theo ngày
    List<Attendance> findByClsIdAndDateAndShift(Long classId, LocalDate date, Shift shift);

    // PH/HS xem chuyên cần theo học sinh
    List<Attendance> findByStudentIdAndShiftOrderByDateDesc(
        Long studentId, Shift shift);  // shift null = all

    // Stats: count by status for student+semester
    @Query("SELECT a.status, COUNT(a) FROM Attendance a " +
           "WHERE a.student.id = :studentId " +
           "AND a.date BETWEEN :startDate AND :endDate " +
           "GROUP BY a.status")
    List<Object[]> countByStatusBetweenDates(@Param("studentId") Long studentId,
                                              @Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate);

    // Check if attendance already recorded for student+date+shift
    Optional<Attendance> findByStudentIdAndDateAndShift(
        Long studentId, LocalDate date, Shift shift);

    // Bulk insert/upsert for daily attendance
    @Modifying
    @Query("DELETE FROM Attendance a WHERE a.cls.id = :classId AND a.date = :date AND a.shift = :shift")
    int deleteByClassDateShift(@Param("classId") Long classId,
                                @Param("date") LocalDate date,
                                @Param("shift") Shift shift);
}
```

### 2C.4. Service

```java
@Service
@Transactional
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final StudentRepository studentRepository;
    private final ClassRepository classRepository;
    private final TeacherRepository teacherRepository;
    private final SemesterRepository semesterRepository;
    private final SemesterResultService semesterResultService;

    // ============================
    // TEACHER FUNCTIONS
    // ============================

    // --- Get daily attendance list ---
    // Returns all students in class with their attendance status for date+shift
    // If no record yet → status = null (chưa điểm danh)
    public DailyAttendanceDto getDailyAttendance(Long classId, LocalDate date,
                                                  Shift shift) {
        // 1. Load all students in class
        // 2. Load existing attendance records for class+date+shift
        // 3. Merge: students without record → status = null
        // 4. Return DailyAttendanceDto
    }

    // --- Submit daily attendance ---
    // GV submit điểm danh cả lớp 1 lần
    // Uses UPSERT: if record exists → update status, else → insert
    public List<AttendanceDto> submitAttendance(SubmitAttendanceRequest request,
                                                 Long teacherId) {
        // 1. Verify teacher is assigned to this class
        // 2. For each entry:
        //    a. Find existing (student, date, shift)
        //    b. If exists → update status
        //    c. If not → create new
        // 3. Return updated records
    }

    // --- Update single attendance ---
    public AttendanceDto updateAttendance(Long attendanceId,
                                           AttendanceStatus newStatus,
                                           Long teacherId) {
        // 1. Verify teacher permission
        // 2. Update status
        // 3. Return updated AttendanceDto
    }

    // ============================
    // PARENT/STUDENT FUNCTIONS
    // ============================

    // --- Get student attendance log ---
    // With stats summary
    public AttendanceLogDto getStudentAttendanceLog(Long studentId,
                                                     Long semesterId,
                                                     Long requestUserId) {
        // 1. Verify access (parent→guardian or student→self)
        // 2. Load semester date range
        // 3. Load all attendance records in range
        // 4. Calculate stats (present, late, absent counts + rate)
        // 5. Return AttendanceLogDto
    }

    // ============================
    // INTERNAL (called by LeaveRequestService)
    // ============================

    // --- Auto-update attendance when leave request approved ---
    // For each date in [dateFrom, dateTo]:
    //   For each affected shift (MORNING/AFTERNOON/FULL_DAY):
    //     UPSERT attendance with status = ABSENT_WITH_LEAVE
    public void autoUpdateForApprovedLeave(Long leaveRequestId) {
        // 1. Load leave request
        // 2. For each date in range:
        //    - If shift = FULL_DAY → update both MORNING and AFTERNOON
        //    - If shift = MORNING → update MORNING only
        //    - If shift = AFTERNOON → update AFTERNOON only
        //    - UPSERT: if no record → insert; if exists → update to ABSENT_WITH_LEAVE
        //    - Link leave_request_id
    }
}
```

**Business Rules — Attendance:**
- 1 record per student per date per shift (UNIQUE constraint)
- Status: PRESENT, LATE, ABSENT_WITH_LEAVE, ABSENT_WITHOUT_LEAVE
- teacher_id = người thực hiện điểm danh
- Khi GV approve đơn nghỉ → auto-update attendance sang ABSENT_WITH_LEAVE
- Attendance stats tính theo semester date range
- Attendance rate = (present + late) / total_days × 100%

---

## 2D. Leave Requests

### 2D.1. Entity

```java
@Entity
@Table(name = "leave_requests")
public class LeaveRequest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_lr_student"))
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_lr_parent"))
    private Parent parent;  // Người tạo đơn

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_lr_class"))
    private Class cls;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by",
                foreignKey = @ForeignKey(name = "fk_lr_teacher"))
    private Teacher approvedBy;  // GV duyệt đơn

    @Column(nullable = false)
    private LocalDate dateFrom;

    @Column(nullable = false)
    private LocalDate dateTo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LeaveShift shift;  // FULL_DAY | MORNING | AFTERNOON

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LeaveStatus status = LeaveStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String response;  // Phản hồi từ GV

    private LocalDateTime approvedAt;

    @OneToMany(mappedBy = "leaveRequest", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Attachment> attachments = new ArrayList<>();

    @OneToMany(mappedBy = "leaveRequest", fetch = FetchType.LAZY)
    private List<Attendance> attendanceRecords = new ArrayList<>();
}
```

### 2D.2. DTOs

```java
// --- Create leave request ---
public record CreateLeaveRequestRequest(
    @NotNull Long studentId,
    @NotNull LocalDate dateFrom,
    @NotNull LocalDate dateTo,
    @NotNull LeaveShift shift,
    @NotBlank String reason
) {}

// --- Approve/Reject ---
public record ReviewLeaveRequestRequest(
    @NotNull LeaveStatus status,   // APPROVED | REJECTED
    String response                // Phản hồi (optional, required for REJECTED)
) {}

// --- Leave request detail ---
public record LeaveRequestDto(
    Long id,
    Long studentId,
    String studentName,
    String studentCode,
    Long parentId,
    String parentName,
    Long classId,
    String className,
    LocalDate dateFrom,
    LocalDate dateTo,
    LeaveShift shift,
    String reason,
    LeaveStatus status,
    String response,
    Long approvedById,
    String approvedByName,
    LocalDateTime approvedAt,
    List<AttachmentDto> attachments,
    LocalDateTime createdAt
) {}

// --- Attachment ---
public record AttachmentDto(
    Long id,
    String fileUrl,
    String fileName,
    Long fileSize,
    String mimeType
) {}
```

### 2D.3. Repository

```java
@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    // PH xem đơn của con
    List<LeaveRequest> findByParentIdOrderByCreatedAtDesc(Long parentId);

    // HS xem đơn của mình
    List<LeaveRequest> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    // GV xem đơn chờ duyệt (cho lớp mình chủ nhiệm)
    @Query("SELECT lr FROM LeaveRequest lr " +
           "WHERE lr.cls.id = :classId AND lr.status = 'PENDING' " +
           "ORDER BY lr.createdAt DESC")
    List<LeaveRequest> findPendingByClass(@Param("classId") Long classId);

    // GV xem tất cả đơn đã gửi cho lớp mình
    List<LeaveRequest> findByClassIdOrderByCreatedAtDesc(Long classId);

    // Count pending by teacher's classes
    @Query("SELECT COUNT(lr) FROM LeaveRequest lr " +
           "WHERE lr.cls IN :classes AND lr.status = 'PENDING'")
    long countPendingByClasses(@Param("classes") List<Class> classes);

    // Check if student has pending request for date range (prevent duplicate)
    @Query("SELECT COUNT(lr) FROM LeaveRequest lr " +
           "WHERE lr.student.id = :studentId AND lr.status = 'PENDING' " +
           "AND lr.dateFrom <= :dateTo AND lr.dateTo >= :dateFrom")
    long countOverlappingPending(@Param("studentId") Long studentId,
                                  @Param("dateFrom") LocalDate dateFrom,
                                  @Param("dateTo") LocalDate dateTo);
}
```

### 2D.4. Service

```java
@Service
@Transactional
public class LeaveRequestService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;
    private final TeacherRepository teacherRepository;
    private final ClassRepository classRepository;
    private final AttendanceService attendanceService;
    private final NotificationService notificationService;

    // ============================
    // PARENT FUNCTIONS
    // ============================

    // --- Create leave request ---
    // PH tạo đơn nghỉ cho con
    public LeaveRequestDto createLeaveRequest(CreateLeaveRequestRequest request,
                                               Long parentUserId) {
        // 1. Find parent by userId
        // 2. Find student, verify guardian relationship
        // 3. Validate: student.class_id matches request
        // 4. Check no overlapping PENDING requests
        // 5. Validate: dateFrom <= dateTo
        // 6. Create LeaveRequest with status = PENDING
        // 7. Notify teacher (GV chủ nhiệm lớp)
        // 8. Return LeaveRequestDto
    }

    // --- Get my leave requests ---
    // PH xem danh sách đơn đã gửi
    public List<LeaveRequestDto> getParentLeaveRequests(Long parentUserId) {
        // 1. Find parent by userId
        // 2. Load all leave requests for this parent
        // 3. Return list
    }

    // --- Cancel leave request ---
    // PH hủy đơn đang PENDING
    public void cancelLeaveRequest(Long leaveRequestId, Long parentUserId) {
        // 1. Verify parent owns this request
        // 2. Verify status == PENDING
        // 3. Delete or set status to CANCELLED
        // 4. Notify teacher
    }

    // ============================
    // STUDENT FUNCTIONS
    // ============================

    // --- Get my leave requests ---
    public List<LeaveRequestDto> getStudentLeaveRequests(Long studentUserId) {
        // 1. Find student by userId
        // 2. Load all leave requests for this student
        // 3. Return list
    }

    // ============================
    // TEACHER FUNCTIONS
    // ============================

    // --- Get pending leave requests for my classes ---
    public List<LeaveRequestDto> getPendingLeaveRequests(Long teacherUserId) {
        // 1. Find teacher by userId
        // 2. Find classes where teacher is homeroom (class_subjects.is_homeroom = true)
        // 3. Load PENDING leave requests for those classes
        // 4. Return list
    }

    // --- Get all leave requests for a class ---
    public List<LeaveRequestDto> getClassLeaveRequests(Long classId,
                                                        LeaveStatus status,
                                                        Long teacherUserId) {
        // 1. Verify teacher is assigned to this class
        // 2. Load leave requests (optionally filtered by status)
        // 3. Return list
    }

    // --- Approve leave request ---
    public LeaveRequestDto approveLeaveRequest(Long leaveRequestId,
                                                String response,
                                                Long teacherUserId) {
        // 1. Verify teacher is homeroom teacher of the class
        // 2. Verify status == PENDING
        // 3. Set status = APPROVED, approved_by, approved_at
        // 4. Call AttendanceService.autoUpdateForApprovedLeave(leaveRequestId)
        // 5. Create notification for parent
        // 6. Return updated LeaveRequestDto
    }

    // --- Reject leave request ---
    public LeaveRequestDto rejectLeaveRequest(Long leaveRequestId,
                                               String response,
                                               Long teacherUserId) {
        // 1. Verify teacher permission
        // 2. Verify status == PENDING
        // 3. Set status = REJECTED, response
        // 4. Create notification for parent
        // 5. Return updated LeaveRequestDto
    }

    // --- Get pending count (for badge) ---
    public long getPendingCount(Long teacherUserId) {
        // 1. Find teacher's homeroom classes
        // 2. Count PENDING requests
        // 3. Return count
    }
}
```

### 2D.5. Controllers

```java
@RestController
@RequestMapping("/api/grades")
@Tag(name = "Grades", description = "Quản lý điểm số")
@SecurityRequirement(name = "Bearer Authentication")
public class GradeController {

    // GET /api/grades/semester?studentId=1&semesterId=2
    // PARENT/STUDENT: view grades for a semester
    @GetMapping("/semester")
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT')")
    public ResponseEntity<ApiResponse<StudentSemesterGradesDto>> getStudentGrades(
        @RequestParam Long studentId,
        @RequestParam Long semesterId) { ... }

    // GET /api/grades/subject?subjectId=1&semesterId=2&classId=1
    // TEACHER: view all students' grades for a subject
    @GetMapping("/subject")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<SubjectGradesDto>> getSubjectGrades(
        @RequestParam Long subjectId,
        @RequestParam Long semesterId,
        @RequestParam Long classId) { ... }

    // PUT /api/grades
    // TEACHER: update single grade
    @PutMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<GradeDto>> updateGrade(
        @Valid @RequestBody UpdateGradeRequest request) { ... }

    // POST /api/grades/batch
    // TEACHER: batch update grades
    @PostMapping("/batch")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<List<GradeDto>>> batchUpdateGrades(
        @Valid @RequestBody BatchGradeUpdateRequest request) { ... }

    // POST /api/grades/simulation
    // PARENT/STUDENT: simulate grade changes (not persisted)
    @PostMapping("/simulation")
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT')")
    public ResponseEntity<ApiResponse<SimulationResultDto>> simulateGrades(
        @Valid @RequestBody GradeSimulationRequest request) { ... }
}

@RestController
@RequestMapping("/api/semester-results")
@Tag(name = "Semester Results", description = "Tổng kết học kỳ")
@SecurityRequirement(name = "Bearer Authentication")
public class SemesterResultController {

    // GET /api/semester-results?studentId=1&semesterId=2
    @GetMapping
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT', 'TEACHER')")
    public ResponseEntity<ApiResponse<SemesterResultDto>> getSemesterResult(
        @RequestParam Long studentId,
        @RequestParam Long semesterId) { ... }

    // GET /api/semester-results/ranking?classId=1&semesterId=2
    // TEACHER: class ranking
    @GetMapping("/ranking")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<ClassRankingDto>> getClassRanking(
        @RequestParam Long classId,
        @RequestParam Long semesterId) { ... }
}

@RestController
@RequestMapping("/api/attendance")
@Tag(name = "Attendance", description = "Chuyên cần / Điểm danh")
@SecurityRequirement(name = "Bearer Authentication")
public class AttendanceController {

    // GET /api/attendance/daily?classId=1&date=2026-06-24&shift=MORNING
    // TEACHER: get daily attendance list
    @GetMapping("/daily")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<DailyAttendanceDto>> getDailyAttendance(
        @RequestParam Long classId,
        @RequestParam LocalDate date,
        @RequestParam Shift shift) { ... }

    // POST /api/attendance/submit
    // TEACHER: submit daily attendance
    @PostMapping("/submit")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<List<AttendanceDto>>> submitAttendance(
        @Valid @RequestBody SubmitAttendanceRequest request) { ... }

    // PUT /api/attendance/{id}
    // TEACHER: update single attendance
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<AttendanceDto>> updateAttendance(
        @PathVariable Long id,
        @RequestBody Map<String, AttendanceStatus> body) { ... }

    // GET /api/attendance/student?studentId=1&semesterId=2
    // PARENT/STUDENT: view student attendance log
    @GetMapping("/student")
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT')")
    public ResponseEntity<ApiResponse<AttendanceLogDto>> getStudentAttendance(
        @RequestParam Long studentId,
        @RequestParam Long semesterId) { ... }
}

@RestController
@RequestMapping("/api/leave-requests")
@Tag(name = "Leave Requests", description = "Đơn xin nghỉ học")
@SecurityRequirement(name = "Bearer Authentication")
public class LeaveRequestController {

    // POST /api/leave-requests
    // PARENT: create leave request
    @PostMapping
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<ApiResponse<LeaveRequestDto>> createLeaveRequest(
        @Valid @RequestBody CreateLeaveRequestRequest request) { ... }

    // GET /api/leave-requests/my
    // PARENT: my leave requests
    @GetMapping("/my")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<ApiResponse<List<LeaveRequestDto>>> getMyLeaveRequests() { ... }

    // GET /api/leave-requests/student?studentId=1
    // STUDENT: my leave requests
    @GetMapping("/student")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<List<LeaveRequestDto>>> getStudentLeaveRequests(
        @RequestParam Long studentId) { ... }

    // GET /api/leave-requests/pending
    // TEACHER: pending requests for my classes
    @GetMapping("/pending")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<List<LeaveRequestDto>>> getPendingRequests() { ... }

    // GET /api/leave-requests/class?classId=1&status=PENDING
    // TEACHER: all requests for a class
    @GetMapping("/class")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<List<LeaveRequestDto>>> getClassRequests(
        @RequestParam Long classId,
        @RequestParam(required = false) LeaveStatus status) { ... }

    // PUT /api/leave-requests/{id}/approve
    // TEACHER: approve
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<LeaveRequestDto>> approveRequest(
        @PathVariable Long id,
        @RequestBody(required = false) Map<String, String> body) { ... }

    // PUT /api/leave-requests/{id}/reject
    // TEACHER: reject
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<LeaveRequestDto>> rejectRequest(
        @PathVariable Long id,
        @RequestBody ReviewLeaveRequestRequest request) { ... }

    // DELETE /api/leave-requests/{id}
    // PARENT: cancel PENDING request
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<ApiResponse<Void>> cancelRequest(
        @PathVariable Long id) { ... }

    // GET /api/leave-requests/pending-count
    // TEACHER: count for badge
    @GetMapping("/pending-count")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<Long>> getPendingCount() { ... }
}
```

---

## Phase 2 — Summary

### API Endpoints

| # | Method | Endpoint | Auth | Description |
|---|--------|----------|------|-------------|
| 1 | GET | `/api/grades/semester` | PARENT/STUDENT | Xem điểm HK |
| 2 | GET | `/api/grades/subject` | TEACHER | Xem điểm môn |
| 3 | PUT | `/api/grades` | TEACHER | Nhập/sửa điểm 1 HS |
| 4 | POST | `/api/grades/batch` | TEACHER | Nhập điểm hàng loạt |
| 5 | POST | `/api/grades/simulation` | PARENT/STUDENT | Mô phỏng điểm |
| 6 | GET | `/api/semester-results` | Any | Tổng kết HK |
| 7 | GET | `/api/semester-results/ranking` | TEACHER | Xếp hạng lớp |
| 8 | GET | `/api/attendance/daily` | TEACHER | DS điểm danh ngày |
| 9 | POST | `/api/attendance/submit` | TEACHER | Gửi điểm danh |
| 10 | PUT | `/api/attendance/{id}` | TEACHER | Sửa 1 bản ghi CC |
| 11 | GET | `/api/attendance/student` | PARENT/STUDENT | Nhật ký CC |
| 12 | POST | `/api/leave-requests` | PARENT | Tạo đơn nghỉ |
| 13 | GET | `/api/leave-requests/my` | PARENT | Đơn của tôi |
| 14 | GET | `/api/leave-requests/student` | STUDENT | Đơn của tôi |
| 15 | GET | `/api/leave-requests/pending` | TEACHER | Đơn chờ duyệt |
| 16 | GET | `/api/leave-requests/class` | TEACHER | Đơn theo lớp |
| 17 | PUT | `/api/leave-requests/{id}/approve` | TEACHER | Duyệt đơn |
| 18 | PUT | `/api/leave-requests/{id}/reject` | TEACHER | Từ chối đơn |
| 19 | DELETE | `/api/leave-requests/{id}` | PARENT | Hủy đơn |
| 20 | GET | `/api/leave-requests/pending-count` | TEACHER | Đếm đơn chờ |

### Key Flows

```
PH tạo đơn → GV nhận notification → GV approve → Attendance auto-update ABSENT_WITH_LEAVE
                                                  → Parent notification

GV nhập điểm → Grade.average auto-calc → SemesterResult auto-recalc
                                          → GPA, rank, academic_ability updated
```

---

*Tiếp tục: [Phase 3 — Communication](phase-3-communication.md)*
