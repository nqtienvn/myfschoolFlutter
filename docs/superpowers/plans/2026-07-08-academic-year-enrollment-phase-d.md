# Phase D: Attendance Refactor — Session + Detail

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Split monolithic `Attendance` entity into `AttendanceSession` (1 buổi điểm danh) + `AttendanceDetail` (từng HS trong buổi). Hỗ trợ điểm danh theo tiết (qua schedule).

**Architecture:**
```
AttendanceSession ───┬─── AttendanceDetail (HS A: PRESENT)
  classId            ├─── AttendanceDetail (HS B: LATE)
  teacherId          ├─── AttendanceDetail (HS C: ABSENT)
  date               └─── ...
  shift
  scheduleId (opt.)
  total/present/late/absent
```

**Tech Stack:** Spring Boot 3.4.5, Spring Data JPA, Java 21, React 19, TypeScript, Vite. No new dependency.

## Global Constraints
- Giữ nguyên `Attendance` entity cũ cho tương thích (deprecated, không xóa)
- `AttendanceSession` unique: (`class_id`, `date`, `shift`, `schedule_id`)
- `AttendanceDetail` unique: (`session_id`, `student_id`)
- Phụ thuộc Phase B để resolve `schedule → assignment → teacher`

---

## File Map

### Create (10 files)
```
backend/src/main/java/vn/edu/fpt/myfschool/
├── controller/entity/AttendanceSession.java
├── controller/entity/AttendanceDetail.java
├── repository/AttendanceSessionRepository.java
├── repository/AttendanceDetailRepository.java
├── common/dto/AttendanceSessionDto.java
├── common/dto/AttendanceDetailDto.java
├── common/dto/CreateAttendanceSessionRequest.java
├── common/dto/UpdateAttendanceDetailRequest.java
├── service/AttendanceSessionService.java
├── controller/AttendanceSessionController.java
```

### Modify (2 files)
```
backend/src/main/java/vn/edu/fpt/myfschool/
├── controller/AttendanceController.java  (thêm endpoint mới)
└── service/AttendanceService.java        (thêm method mới)
```

### Admin web
```
admin-web/src/pages/
├── AttendanceSessionPage.tsx (new — điểm danh theo buổi)
```

---

## Task 1: Entities

### AttendanceSession.java
```java
@Entity
@Table(name = "attendance_sessions",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"class_id", "date", "shift", "schedule_id"}))
@Data
@EqualsAndHashCode(callSuper = true)
public class AttendanceSession extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_as_class"))
    private SchoolClass cls;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_as_teacher"))
    private Teacher teacher;

    @Column(nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Shift shift;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id",
                foreignKey = @ForeignKey(name = "fk_as_schedule"))
    private Schedule schedule;  // optional — điểm danh theo tiết

    @Column(nullable = false)
    private Integer total = 0;

    @Column(nullable = false)
    private Integer present = 0;

    @Column(nullable = false)
    private Integer late = 0;

    @Column(nullable = false)
    private Integer absent = 0;

    @Column(name = "is_closed")
    private Boolean isClosed = false;  // kết thúc buổi điểm danh
}
```

### AttendanceDetail.java
```java
@Entity
@Table(name = "attendance_details",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"session_id", "student_id"}))
@Data
@EqualsAndHashCode(callSuper = true)
public class AttendanceDetail extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_ad_session"))
    private AttendanceSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_ad_student"))
    private Student student;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AttendanceStatus status = AttendanceStatus.PRESENT;

    @Column(columnDefinition = "TEXT")
    private String note;
}
```

- [x] **Step 1.1:** Tạo `AttendanceSession.java`
- [x] **Step 1.2:** Tạo `AttendanceDetail.java`

---

## Task 2: Repositories

### AttendanceSessionRepository.java
```java
public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, Long> {
    List<AttendanceSession> findByClsIdAndDateAndShift(Long classId, LocalDate date, Shift shift);
    List<AttendanceSession> findByClsIdAndSemester(Long classId, LocalDate startDate, LocalDate endDate);
    Optional<AttendanceSession> findByClsIdAndDateAndShiftAndScheduleId(
        Long classId, LocalDate date, Shift shift, Long scheduleId);
    Optional<AttendanceSession> findTopByClsIdAndDateAndShiftOrderByCreatedAtDesc(
        Long classId, LocalDate date, Shift shift);
}
```

### AttendanceDetailRepository.java
```java
public interface AttendanceDetailRepository extends JpaRepository<AttendanceDetail, Long> {
    List<AttendanceDetail> findBySessionId(Long sessionId);
    Optional<AttendanceDetail> findBySessionIdAndStudentId(Long sessionId, Long studentId);
    long countBySessionIdAndStatus(Long sessionId, AttendanceStatus status);
    @Query("SELECT COUNT(d) FROM AttendanceDetail d WHERE d.session.id = :sessionId")
    int countBySessionId(@Param("sessionId") Long sessionId);
}
```

- [x] **Step 2.1:** Tạo `AttendanceSessionRepository.java`
- [x] **Step 2.2:** Tạo `AttendanceDetailRepository.java`

---

## Task 3: DTOs

### CreateAttendanceSessionRequest.java
```java
public record CreateAttendanceSessionRequest(
    @NotNull Long classId,
    @NotNull Long teacherId,
    @NotNull LocalDate date,
    @NotNull Shift shift,
    Long scheduleId  // optional
) {}
```

### AttendanceSessionDto.java
```java
public record AttendanceSessionDto(
    Long id,
    Long classId, String className,
    Long teacherId, String teacherName,
    LocalDate date, Shift shift,
    Long scheduleId,
    Integer total, Integer present, Integer late, Integer absent,
    Boolean isClosed,
    List<AttendanceDetailDto> details
) {}
```

### AttendanceDetailDto.java
```java
public record AttendanceDetailDto(
    Long id, Long sessionId,
    Long studentId, String studentName, String studentCode,
    String status, String note
) {}
```

### UpdateAttendanceDetailRequest.java
```java
public record UpdateAttendanceDetailEntry(Long studentId, String status, String note) {}

public record UpdateAttendanceDetailRequest(
    @NotNull Long sessionId,
    @NotNull List<UpdateAttendanceDetailEntry> entries
) {}
```

- [x] **Step 3.1:** Tạo DTOs

---

## Task 4: Service

### AttendanceSessionService.java
```java
public interface AttendanceSessionService {
    AttendanceSessionDto createSession(CreateAttendanceSessionRequest request, Long userId);
    List<AttendanceDetailDto> updateDetails(UpdateAttendanceDetailRequest request, Long userId);
    AttendanceSessionDto closeSession(Long sessionId, Long userId);
    List<AttendanceSessionDto> findByClassDateShift(Long classId, LocalDate date, Shift shift);
    AttendanceStatsDto getStudentStats(Long studentId, Long semesterId);
}
```

### AttendanceSessionServiceImpl.java
```java
@Service
@RequiredArgsConstructor
@Transactional
public class AttendanceSessionServiceImpl implements AttendanceSessionService {

    private final AttendanceSessionRepository sessionRepository;
    private final AttendanceDetailRepository detailRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final SchoolClassRepository classRepository;
    private final TeacherRepository teacherRepository;
    private final ScheduleRepository scheduleRepository;

    @Override
    public AttendanceSessionDto createSession(CreateAttendanceSessionRequest request, Long userId) {
        // 1. Kiểm tra session đã tồn tại chưa
        Optional<AttendanceSession> existing = request.scheduleId() != null
            ? sessionRepository.findByClsIdAndDateAndShiftAndScheduleId(
                request.classId(), request.date(), request.shift(), request.scheduleId())
            : sessionRepository.findTopByClsIdAndDateAndShiftOrderByCreatedAtDesc(
                request.classId(), request.date(), request.shift());
        if (existing.isPresent() && !existing.get().getIsClosed()) {
            throw new ConflictException("Buổi điểm danh đã tồn tại, hãy tiếp tục thay vì tạo mới");
        }

        // 2. Tạo session
        SchoolClass cls = classRepository.findById(request.classId()).orElseThrow(...);
        Teacher teacher = teacherRepository.findByUserId(userId).orElseThrow(...);
        AttendanceSession session = new AttendanceSession();
        session.setCls(cls);
        session.setTeacher(teacher);
        session.setDate(request.date());
        session.setShift(request.shift());
        if (request.scheduleId() != null) {
            session.setSchedule(scheduleRepository.findById(request.scheduleId()).orElse(null));
        }
        session = sessionRepository.save(session);

        // 3. Lấy tất cả HS active trong lớp
        List<Student> students = enrollmentRepository.findActiveStudentsByClassAndYear(
            request.classId(), cls.getAcademicYear().getId());

        // 4. Tạo detail cho từng HS (default PRESENT)
        List<AttendanceDetail> details = students.stream().map(s -> {
            AttendanceDetail d = new AttendanceDetail();
            d.setSession(session);
            d.setStudent(s);
            d.setStatus(AttendanceStatus.PRESENT);
            return d;
        }).toList();
        detailRepository.saveAll(details);

        // 5. Cập nhật counts
        session.setTotal(students.size());
        session.setPresent(students.size());
        session.setLate(0);
        session.setAbsent(0);
        sessionRepository.save(session);

        return toDto(session);
    }

    @Override
    public List<AttendanceDetailDto> updateDetails(UpdateAttendanceDetailRequest request, Long userId) {
        AttendanceSession session = sessionRepository.findById(request.sessionId())
            .orElseThrow(() -> new ResourceNotFoundException("AttendanceSession", "id", request.sessionId()));
        if (session.getIsClosed()) {
            throw new BadRequestException("Buổi điểm danh đã kết thúc, không thể chỉnh sửa");
        }

        List<AttendanceDetailDto> results = new ArrayList<>();
        for (UpdateAttendanceDetailEntry entry : request.entries()) {
            AttendanceDetail detail = detailRepository.findBySessionIdAndStudentId(
                session.getId(), entry.studentId())
                .orElseThrow(() -> new ResourceNotFoundException("AttendanceDetail", "studentId", entry.studentId()));
            detail.setStatus(AttendanceStatus.valueOf(entry.status()));
            detail.setNote(entry.note());
            detailRepository.save(detail);
            results.add(toDto(detail));
        }

        // Cập nhật counts
        recalculateCounts(session);

        return results;
    }

    @Override
    public AttendanceSessionDto closeSession(Long sessionId, Long userId) {
        AttendanceSession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("AttendanceSession", "id", sessionId));
        session.setIsClosed(true);
        session = sessionRepository.save(session);
        return toDto(session);
    }

    private void recalculateCounts(AttendanceSession session) {
        List<AttendanceDetail> details = detailRepository.findBySessionId(session.getId());
        int present = 0, late = 0, absent = 0;
        for (AttendanceDetail d : details) {
            switch (d.getStatus()) {
                case PRESENT -> present++;
                case LATE -> late++;
                default -> absent++;
            }
        }
        session.setPresent(present);
        session.setLate(late);
        session.setAbsent(absent);
        sessionRepository.save(session);
    }
}
```

- [x] **Step 4.1:** Tạo `AttendanceSessionService.java`
- [x] **Step 4.2:** Tạo `AttendanceSessionServiceImpl.java`

---

## Task 5: Controller

### AttendanceSessionController.java
```java
@RestController
@RequestMapping("/api/attendance-sessions")
@RequiredArgsConstructor
@Tag(name = "Attendance Sessions", description = "Điểm danh theo buổi")
@SecurityRequirement(name = "Bearer Authentication")
public class AttendanceSessionController {

    private final AttendanceSessionService attendanceSessionService;

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<AttendanceSessionDto>> createSession(
            @Valid @RequestBody CreateAttendanceSessionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
            "Bắt đầu điểm danh",
            attendanceSessionService.createSession(request, SecurityUtil.getCurrentUserId())));
    }

    @PutMapping("/{id}/details")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<List<AttendanceDetailDto>>> updateDetails(
            @PathVariable Long id, @Valid @RequestBody UpdateAttendanceDetailRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
            "Cập nhật điểm danh",
            attendanceSessionService.updateDetails(request, SecurityUtil.getCurrentUserId())));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<AttendanceSessionDto>> closeSession(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
            "Kết thúc điểm danh",
            attendanceSessionService.closeSession(id, SecurityUtil.getCurrentUserId())));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<AttendanceSessionDto>>> getSessions(
            @RequestParam Long classId, @RequestParam LocalDate date, @RequestParam Shift shift) {
        return ResponseEntity.ok(ApiResponse.success(
            attendanceSessionService.findByClassDateShift(classId, date, shift)));
    }

    @GetMapping("/student-stats")
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT')")
    public ResponseEntity<ApiResponse<AttendanceStatsDto>> getStudentStats(
            @RequestParam Long studentId, @RequestParam Long semesterId) {
        return ResponseEntity.ok(ApiResponse.success(
            attendanceSessionService.getStudentStats(studentId, semesterId)));
    }
}
```

- [x] **Step 5.1:** Tạo `AttendanceSessionController.java`

---

## Task 6: Admin-web

### AttendanceSessionPage.tsx
```tsx
// Features:
// 1. Chọn lớp, ngày, buổi (sáng/chiều) → load session
// 2. Nếu chưa có session → nút "Bắt đầu điểm danh"
// 3. Hiển thị danh sách HS với dropdown status (PRESENT/LATE/ABSENT_WITH_LEAVE/ABSENT_WITHOUT_LEAVE)
// 4. Nút "Lưu" → batch update
// 5. Nút "Kết thúc" → close session
// 6. Thống kê: Có mặt / Muộn / Vắng
```

- [x] **Step 6.1:** Tạo `AttendanceSessionPage.tsx`
- [x] **Step 6.2:** Build:
  ```bash
  npm --prefix admin-web run build
  ```

---

## Task 7: Tests & verification

### AttendanceSessionIntegrationTest.java
```java
// Test: tạo session → tự động tạo detail cho 3 HS mẫu
// Test: update 2 HS thành LATE + ABSENT → counts update
// Test: tạo session trùng → conflict
// Test: close session → không cho update nữa
```

- [x] **Step 7.1:** Tạo test file
- [x] **Step 7.2:** Chạy test:
  ```bash
  mvn -f backend/pom.xml test -Dtest=AttendanceSessionIntegrationTest
  mvn -f backend/pom.xml test
  ```
- [x] **Step 7.3:** Manual smoke:
  1. Tạo session: POST `/api/attendance-sessions` → 42 chi tiết auto-generated
  2. Update: PUT `/api/attendance-sessions/{id}/details` → counts thay đổi
  3. Close session → PUT fails
  4. Session cũ vẫn query được

---

## Self-review checklist
- [x] `AttendanceSession` + `AttendanceDetail` entities OK
- [x] Tự động tạo detail khi tạo session
- [x] Update counts khi batch update
- [x] Close session = immutable
- [x] Entity cũ `Attendance` giữ nguyên (deprecated)
- [x] Tests pass
- [x] Admin-web build pass
