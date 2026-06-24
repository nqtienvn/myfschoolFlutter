# Phase 5 — Schedule & Reports (Dashboard Statistics)

> Tables: `schedules` (1 table mới) + Dashboard aggregation endpoints
> Tổng: **~20 API endpoints**

---

## 5A. Schedules (Thời khóa biểu)

### 5A.1. Entity

```java
@Entity
@Table(name = "schedules",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"class_id", "semester_id", "day_of_week", "period"}))
public class Schedule extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_sch_class"))
    private Class cls;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_sch_subject"))
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_sch_teacher"))
    private Teacher teacher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_sch_semester"))
    private Semester semester;

    // 1=CN, 2=T2, ..., 7=T7
    @Column(name = "day_of_week", nullable = false)
    private Integer dayOfWeek;

    // Tiết 1-10
    @Column(nullable = false)
    private Integer period;

    @Column(length = 20)
    private String room;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Shift shift = Shift.MORNING;
}
```

### 5A.2. DTOs

```java
// --- Create schedule entry ---
public record CreateScheduleRequest(
    @NotNull Long classId,
    @NotNull Long subjectId,
    @NotNull Long teacherId,
    @NotNull Long semesterId,
    @NotNull @Min(1) @Max(7) Integer dayOfWeek,    // 1=CN, 2=T2, ..., 7=T7
    @NotNull @Min(1) @Max(10) Integer period,
    @Size(max = 20) String room,
    @NotNull Shift shift
) {}

// --- Batch create (full week schedule) ---
public record BatchCreateScheduleRequest(
    @NotNull Long classId,
    @NotNull Long semesterId,
    @NotEmpty List<ScheduleEntry> entries
) {}

public record ScheduleEntry(
    @NotNull Long subjectId,
    @NotNull Long teacherId,
    @NotNull Integer dayOfWeek,
    @NotNull Integer period,
    String room,
    @NotNull Shift shift
) {}

// --- Schedule response ---
public record ScheduleDto(
    Long id,
    Long classId,
    String className,
    Long subjectId,
    String subjectName,
    String subjectCode,
    Long teacherId,
    String teacherName,
    Long semesterId,
    String semesterName,
    Integer dayOfWeek,
    String dayOfWeekName,    // "Thứ 2", "Thứ 3", ...
    Integer period,
    String room,
    Shift shift
) {}

// --- Full schedule view for a class ---
public record ClassScheduleDto(
    Long classId,
    String className,
    Long semesterId,
    String semesterName,
    // Grouped by day of week
    List<DayScheduleDto> days
) {}

public record DayScheduleDto(
    Integer dayOfWeek,
    String dayOfWeekName,     // "Thứ 2 (Monday)"
    List<ScheduleDto> morningSlots,   // shift = MORNING, sorted by period
    List<ScheduleDto> afternoonSlots  // shift = AFTERNOON, sorted by period
) {}

// --- Teacher's schedule ---
public record TeacherScheduleDto(
    Long teacherId,
    String teacherName,
    Long semesterId,
    String semesterName,
    List<DayScheduleDto> days
) {}

// --- Current week schedule (for student/parent) ---
public record WeekScheduleDto(
    Long classId,
    String className,
    LocalDate weekStart,    // Monday of current week
    LocalDate weekEnd,      // Sunday of current week
    List<DayScheduleDto> days
) {}
```

### 5A.3. Repository

```java
@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    // Class schedule for a semester
    List<Schedule> findByClassIdAndSemesterIdOrderByDayOfWeekAscPeriodAsc(
        Long classId, Long semesterId);

    // Teacher schedule for a semester
    List<Schedule> findByTeacherIdAndSemesterIdOrderByDayOfWeekAscPeriodAsc(
        Long teacherId, Long semesterId);

    // Specific day for a class
    List<Schedule> findByClassIdAndSemesterIdAndDayOfWeekOrderByPeriodAsc(
        Long classId, Long semesterId, Integer dayOfWeek);

    // Check conflict: same class+semester+day+period already has a subject
    Optional<Schedule> findByClassIdAndSemesterIdAndDayOfWeekAndPeriod(
        Long classId, Long semesterId, Integer dayOfWeek, Integer period);

    // Check teacher conflict: teacher already teaches at this time
    @Query("SELECT s FROM Schedule s WHERE s.teacher.id = :teacherId " +
           "AND s.semester.id = :semesterId " +
           "AND s.dayOfWeek = :dayOfWeek AND s.period = :period")
    Optional<Schedule> findTeacherConflict(@Param("teacherId") Long teacherId,
                                            @Param("semesterId") Long semesterId,
                                            @Param("dayOfWeek") Integer dayOfWeek,
                                            @Param("period") Integer period);

    // Subjects taught by teacher in a class
    @Query("SELECT DISTINCT s.subject FROM Schedule s " +
           "WHERE s.teacher.id = :teacherId AND s.semester.id = :semesterId " +
           "AND s.cls.id = :classId")
    List<Subject> findSubjectsByTeacherAndClass(@Param("teacherId") Long teacherId,
                                                 @Param("semesterId") Long semesterId,
                                                 @Param("classId") Long classId);

    // Delete all schedules for a class+semester (for batch replace)
    void deleteByClassIdAndSemesterId(Long classId, Long semesterId);
}
```

### 5A.4. Service

```java
@Service
@Transactional
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final ClassRepository classRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;
    private final SemesterRepository semesterRepository;

    // --- Get class schedule ---
    public ClassScheduleDto getClassSchedule(Long classId, Long semesterId) {
        // 1. Load all schedules for class+semester
        // 2. Group by dayOfWeek
        // 3. Within each day: split MORNING/AFTERNOON, sort by period
        // 4. Return ClassScheduleDto
    }

    // --- Get current week schedule ---
    // For student/parent: show this week's schedule
    public WeekScheduleDto getCurrentWeekSchedule(Long classId, Long semesterId) {
        // 1. Find semester to get date range
        // 2. Calculate current week's Monday-Sunday
        // 3. Load schedules for class+semester
        // 4. Map dayOfWeek → actual dates
        // 5. Return WeekScheduleDto
    }

    // --- Get teacher schedule ---
    public TeacherScheduleDto getTeacherSchedule(Long teacherId, Long semesterId) {
        // 1. Load all schedules where teacher is assigned
        // 2. Group by dayOfWeek, split by shift
        // 3. Return TeacherScheduleDto
    }

    // --- Create single schedule entry ---
    public ScheduleDto createSchedule(CreateScheduleRequest request) {
        // 1. Validate: class, subject, teacher, semester exist
        // 2. Check class conflict: same class+semester+day+period → error
        // 3. Check teacher conflict: teacher already teaches at this time → error
        // 4. Create Schedule
        // 5. Return ScheduleDto
    }

    // --- Batch create schedule ---
    public List<ScheduleDto> batchCreateSchedule(BatchCreateScheduleRequest request) {
        // 1. Clear existing schedule for class+semester (replace)
        // 2. For each entry: validate + check conflicts
        // 3. Save all
        // 4. Return list
    }

    // --- Update schedule entry ---
    public ScheduleDto updateSchedule(Long scheduleId, CreateScheduleRequest request) {
        // 1. Find schedule
        // 2. Check conflicts (excluding self)
        // 3. Update fields
        // 4. Return updated ScheduleDto
    }

    // --- Delete schedule entry ---
    public void deleteSchedule(Long scheduleId) {
        scheduleRepository.deleteById(scheduleId);
    }

    // --- Delete all for class+semester ---
    public void deleteClassSchedule(Long classId, Long semesterId) {
        scheduleRepository.deleteByClassIdAndSemesterId(classId, semesterId);
    }

    // --- Get available periods for a class on a day ---
    // Returns which periods are free (for UI scheduling)
    public List<Integer> getAvailablePeriods(Long classId, Long semesterId,
                                              Integer dayOfWeek, Shift shift) {
        // 1. Load all schedules for class+semester+day
        // 2. Filter by shift
        // 3. Return periods 1-10 minus occupied periods
    }
}
```

**Business Rules:**
- Unique: 1 class cannot have 2 subjects at same day+period+semester
- Teacher cannot teach 2 classes at same time
- Batch create replaces entire schedule (clear + insert)
- dayOfWeek: 1=CN, 2=T2, 3=T3, 4=T4, 5=T5, 6=T6, 7=T7
- Period: 1-10 (1-5 morning, 6-10 afternoon typically)
- Shift: MORNING or AFTERNOON

---

## 5B. Dashboard / Statistics

### 5B.1. DTOs

```java
// --- Dashboard for TEACHER (GVCN) ---
public record TeacherDashboardDto(
    String teacherName,
    String role,            // "GVCN" or "BM"
    // Class info
    Long classId,
    String className,
    int studentCount,
    // Pending items
    long pendingLeaveRequests,
    long unreadMessages,
    long unreadAnnouncements,
    // Today's schedule
    List<ScheduleDto> todaySchedule,
    // Quick stats
    DashboardStatsDto stats
) {}

// --- Dashboard for PARENT ---
public record ParentDashboardDto(
    String parentName,
    // Selected student info
    Long studentId,
    String studentName,
    String studentCode,
    String className,
    // Children list
    List<StudentSummaryDto> children,
    // Selected student stats
    DashboardStudentStatsDto studentStats,
    // Pending items
    long unpaidBills,
    long unreadNotifications,
    long pendingLeaveRequests
) {}

// --- Dashboard for STUDENT ---
public record StudentDashboardDto(
    String studentName,
    String studentCode,
    String className,
    // Today's schedule
    List<ScheduleDto> todaySchedule,
    // Quick stats
    DashboardStudentStatsDto stats,
    // Pending items
    long unreadNotifications,
    long unreadMessages,
    long unreadAnnouncements
) {}

// --- Common stats ---
public record DashboardStatsDto(
    double attendanceRate,      // Tỷ lệ đi học %
    int totalSubjects,
    BigDecimal averageGpa,
    String academicAbility      // Học lực hiện tại
) {}

public record DashboardStudentStatsDto(
    double attendanceRate,
    int presentDays,
    int absentDays,
    int lateDays,
    BigDecimal currentGpa,
    String academicAbility,
    String conduct,
    Integer classRank
) {}
```

### 5B.2. Controller

```java
@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Tổng quan / Thống kê")
@SecurityRequirement(name = "Bearer Authentication")
public class DashboardController {

    // GET /api/dashboard/teacher
    // TEACHER: dashboard data
    @GetMapping("/teacher")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<TeacherDashboardDto>> getTeacherDashboard() { ... }

    // GET /api/dashboard/parent?studentId=1
    // PARENT: dashboard data for selected student
    @GetMapping("/parent")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<ApiResponse<ParentDashboardDto>> getParentDashboard(
        @RequestParam Long studentId) { ... }

    // GET /api/dashboard/student
    // STUDENT: dashboard data
    @GetMapping("/student")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<StudentDashboardDto>> getStudentDashboard() { ... }

    // GET /api/dashboard/teacher/stats?classId=1&semesterId=2
    // TEACHER: class statistics (attendance + grades)
    @GetMapping("/teacher/stats")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<ClassStatsDto>> getClassStats(
        @RequestParam Long classId,
        @RequestParam Long semesterId) { ... }
}
```

### 5B.3. Dashboard Service

```java
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final UserRepository userRepository;
    private final ParentRepository parentRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final ScheduleRepository scheduleRepository;
    private final GradeRepository gradeRepository;
    private final SemesterResultRepository semesterResultRepository;
    private final AttendanceRepository attendanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final TuitionBillRepository tuitionBillRepository;
    private final NotificationRepository notificationRepository;
    private final AnnouncementRepository announcementRepository;
    private final ConversationService conversationService;
    private final SemesterRepository semesterRepository;

    // --- Teacher Dashboard ---
    public TeacherDashboardDto getTeacherDashboard(Long teacherUserId) {
        // 1. Find teacher by userId
        // 2. Find homeroom class (class_subjects.is_homeroom = true)
        // 3. Count: pending leave requests, unread messages, unread announcements
        // 4. Load today's schedule (dayOfWeek = current day)
        // 5. Load quick stats: attendance rate, student count, avg GPA
        // 6. Return TeacherDashboardDto
    }

    // --- Parent Dashboard ---
    public ParentDashboardDto getParentDashboard(Long parentUserId, Long selectedStudentId) {
        // 1. Find parent by userId
        // 2. Load children list
        // 3. Verify selected student is a child
        // 4. Load stats for selected student:
        //    - Attendance rate (current semester)
        //    - GPA, rank, conduct (semester_result)
        // 5. Count: unpaid bills, unread notifications, pending leave requests
        // 6. Return ParentDashboardDto
    }

    // --- Student Dashboard ---
    public StudentDashboardDto getStudentDashboard(Long studentUserId) {
        // 1. Find student by userId
        // 2. Load today's schedule
        // 3. Load stats: attendance rate, GPA, rank, conduct
        // 4. Count: unread notifications, messages, announcements
        // 5. Return StudentDashboardDto
    }

    // --- Class Stats (for teacher) ---
    public ClassStatsDto getClassStats(Long classId, Long semesterId) {
        // 1. Load all students in class
        // 2. Attendance stats: overall rate, by student
        // 3. Grade stats: average GPA, distribution (Giỏi/Khá/TB/Yếu counts)
        // 4. Return ClassStatsDto
    }
}
```

### 5B.4. Class Stats DTO

```java
public record ClassStatsDto(
    Long classId,
    String className,
    Long semesterId,
    String semesterName,
    int totalStudents,
    // Attendance stats
    double overallAttendanceRate,
    int totalPresent,
    int totalLate,
    int totalAbsent,
    // Grade distribution
    double averageGpa,
    int honorCount,        // Giỏi
    int goodCount,         // Khá
    int averageCount,      // Trung bình
    int weakCount,         // Yếu
    // Per-subject stats
    List<SubjectStatsDto> subjectStats
) {}

public record SubjectStatsDto(
    Long subjectId,
    String subjectName,
    double averageScore,
    double highestScore,
    double lowestScore,
    int passCount,        // >= 5.0
    int failCount         // < 5.0
) {}
```

---

## 5C. Schedule Controller

```java
@RestController
@RequestMapping("/api/schedules")
@Tag(name = "Schedules", description = "Thời khóa biểu")
@SecurityRequirement(name = "Bearer Authentication")
public class ScheduleController {

    private final ScheduleService scheduleService;

    // GET /api/schedules/class?classId=1&semesterId=2
    // Any role: class schedule
    @GetMapping("/class")
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT', 'TEACHER')")
    public ResponseEntity<ApiResponse<ClassScheduleDto>> getClassSchedule(
        @RequestParam Long classId,
        @RequestParam Long semesterId) { ... }

    // GET /api/schedules/class/current-week?classId=1&semesterId=2
    // Any role: current week view
    @GetMapping("/class/current-week")
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT', 'TEACHER')")
    public ResponseEntity<ApiResponse<WeekScheduleDto>> getCurrentWeekSchedule(
        @RequestParam Long classId,
        @RequestParam Long semesterId) { ... }

    // GET /api/schedules/teacher?teacherId=1&semesterId=2
    // TEACHER: own schedule
    @GetMapping("/teacher")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<TeacherScheduleDto>> getTeacherSchedule(
        @RequestParam Long teacherId,
        @RequestParam Long semesterId) { ... }

    // POST /api/schedules
    // TEACHER: create single entry
    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<ScheduleDto>> createSchedule(
        @Valid @RequestBody CreateScheduleRequest request) { ... }

    // POST /api/schedules/batch
    // TEACHER: batch create (replace full schedule)
    @PostMapping("/batch")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<List<ScheduleDto>>> batchCreateSchedule(
        @Valid @RequestBody BatchCreateScheduleRequest request) { ... }

    // PUT /api/schedules/{id}
    // TEACHER: update entry
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<ScheduleDto>> updateSchedule(
        @PathVariable Long id,
        @Valid @RequestBody CreateScheduleRequest request) { ... }

    // DELETE /api/schedules/{id}
    // TEACHER: delete entry
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<Void>> deleteSchedule(
        @PathVariable Long id) { ... }

    // DELETE /api/schedules/class?classId=1&semesterId=2
    // TEACHER: clear entire schedule
    @DeleteMapping("/class")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<Void>> deleteClassSchedule(
        @RequestParam Long classId,
        @RequestParam Long semesterId) { ... }

    // GET /api/schedules/available-periods?classId=1&semesterId=2&dayOfWeek=2&shift=MORNING
    // TEACHER: available periods for scheduling
    @GetMapping("/available-periods")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<List<Integer>>> getAvailablePeriods(
        @RequestParam Long classId,
        @RequestParam Long semesterId,
        @RequestParam Integer dayOfWeek,
        @RequestParam Shift shift) { ... }
}
```

---

## Phase 5 — Summary

### API Endpoints

| # | Method | Endpoint | Auth | Description |
|---|--------|----------|------|-------------|
| 1 | GET | `/api/schedules/class` | Any | TKB lớp |
| 2 | GET | `/api/schedules/class/current-week` | Any | TKB tuần hiện tại |
| 3 | GET | `/api/schedules/teacher` | TEACHER | TKB giáo viên |
| 4 | POST | `/api/schedules` | TEACHER | Tạo tiết TKB |
| 5 | POST | `/api/schedules/batch` | TEACHER | Tạo TKB hàng loạt |
| 6 | PUT | `/api/schedules/{id}` | TEACHER | Sửa tiết TKB |
| 7 | DELETE | `/api/schedules/{id}` | TEACHER | Xóa tiết TKB |
| 8 | DELETE | `/api/schedules/class` | TEACHER | Xóa toàn bộ TKB lớp |
| 9 | GET | `/api/schedules/available-periods` | TEACHER | Tiết trống |
| 10 | GET | `/api/dashboard/teacher` | TEACHER | Dashboard GV |
| 11 | GET | `/api/dashboard/parent` | PARENT | Dashboard PH |
| 12 | GET | `/api/dashboard/student` | STUDENT | Dashboard HS |
| 13 | GET | `/api/dashboard/teacher/stats` | TEACHER | Thống kê lớp |

---

## Final — All Phases Combined

### Total API Endpoints

| Phase | Endpoints | Key Features |
|-------|-----------|-------------|
| P1 Foundation | 26 | Auth, Users, Classes, Subjects, Semesters |
| P2 Academic | 20 | Grades, Attendance, Leave Requests, Semester Results |
| P3 Communication | 16 + WS | Conversations, Messages, Announcements, Notifications, WebSocket |
| P4 Business | 19 | Tuition, Payments, Clubs, Attachments |
| P5 Schedule+Reports | 13 | Schedule CRUD, Dashboard, Statistics |
| **TOTAL** | **~94 + WebSocket** | **27 tables, 50 FKs** |

### Complete Entity List (27)

```
P1: User, UserSetting, Parent, Student, Teacher,
    Class, Subject, Semester, StudentGuardian, StudentClass, ClassSubject
P2: Grade, SemesterResult, Attendance, LeaveRequest
P3: Conversation, ConversationParticipant, Message,
    Announcement, AnnouncementClass, AnnouncementRead, Notification
P4: TuitionBill, PaymentTransaction, ClubRegistration, Attachment
P5: Schedule
```

### Implementation Parallel Strategy

```
Phase 1 (Foundation):
├── P1A: Auth + Users + Security         [Subagent A]
└── P1B: Reference CRUD (Class/Subject/Semester)  [Subagent B]
    → P1A & P1B parallel (independent after DB migration)

Phase 2 (Academic):
├── P2A: Grades + SemesterResults        [Subagent C]
└── P2B: Attendance + LeaveRequests      [Subagent D]
    → P2A & P2B parallel (share DB, minimal cross-dependency)

Phase 3 (Communication):
├── P3A: Announcements + Notifications   [Subagent E]
└── P3B: Conversations + Messages + WS   [Subagent F]
    → P3A & P3B parallel

Phase 4 (Business):
├── P4A: Tuition + Payments              [Subagent G]
└── P4B: Clubs + Attachments             [Subagent H]
    → P4A & P4B parallel

Phase 5 (Schedule + Reports):
└── P5: Schedules + Dashboard            [Subagent I]
    → Sequential after P1 (needs classes, teachers)

Cross-phase dependencies:
- P2B LeaveRequest→Attendance auto-update (within P2)
- P2A Grade→SemesterResult auto-recalc (within P2)
- P3B WebSocket pushes (P3B only, P3A uses REST)
- P4A TuitionBill→Notification (calls NotificationService from P3A)
- P5 Dashboard reads from P2 (grades, attendance) + P3 (notifications)
```

---

*Spec hoàn tất. Tất cả 5 phases đã được viết chi tiết.*
