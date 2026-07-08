# Phase B: Teaching Assignment & Schedule Refactor

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Split `ClassSubject.isHomeroom` into dedicated `HomeroomAssignment` + `TeachingAssignment` entities. Refactor `Schedule` to FK `TeachingAssignment` instead of storing `class_id/subject_id/teacher_id/semester_id` directly.

**Architecture:**
- `ClassSubject` → deleted (replaced by `TeachingAssignment`)
- New `TeachingAssignment` — maps (class, subject, teacher, semester, effective_from, effective_to)
- New `HomeroomAssignment` — maps (class, teacher, academic_year, effective_from, effective_to)
- `Schedule` — FK to `TeachingAssignment`, keep `day_of_week + period + room + shift`

**Tech Stack:** Spring Boot 3.4.5, Spring Data JPA, Java 21, React 19, TypeScript, Vite. No new dependency.

## Global Constraints
- `TeachingAssignment` unique: (`class_id`, `subject_id`, `semester_id`, `effective_from`)
- `HomeroomAssignment` unique: (`class_id`, `academic_year_id`, `effective_from`)
- `Schedule` unique: (`assignment_id`, `day_of_week`, `period`) — vì assignment đã biết class + semester
- Do not modify `Attendance`, `Grade`, `GradeBook`, `FeeTemplate` in this phase
- No `isHomeroom` field on `TeachingAssignment`

---

## File Map

### Create (14 files)
```
backend/src/main/java/vn/edu/fpt/myfschool/
├── common/enums/AssignmentStatus.java
├── controller/entity/TeachingAssignment.java
├── controller/entity/HomeroomAssignment.java
├── repository/TeachingAssignmentRepository.java
├── repository/HomeroomAssignmentRepository.java
├── common/dto/TeachingAssignmentDto.java
├── common/dto/TeachingAssignmentDetailDto.java
├── common/dto/CreateTeachingAssignmentRequest.java
├── common/dto/HomeroomAssignmentDto.java
├── common/dto/CreateHomeroomAssignmentRequest.java
├── service/TeachingAssignmentService.java
├── service/HomeroomAssignmentService.java
├── controller/TeachingAssignmentController.java
├── controller/HomeroomAssignmentController.java
```

### Modify (12 files)
```
backend/src/main/java/vn/edu/fpt/myfschool/
├── controller/entity/Schedule.java
├── controller/entity/ClassSubject.java (field changes)
├── repository/ScheduleRepository.java
├── common/dto/ScheduleDto.java
├── common/dto/ScheduleRequest.java
├── common/dto/ClassDto.java
├── common/dto/ClassDetailDto.java
├── service/ScheduleService.java
├── service/impl/ScheduleServiceImpl.java
├── controller/ScheduleController.java
├── controller/ClassController.java
├── service/ClassService.java
```

### Delete (2 files)
```
backend/src/main/java/vn/edu/fpt/myfschool/
├── controller/entity/ClassSubject.java (entirely)
└── repository/ClassSubjectRepository.java
```

### Admin web (create/modify)
```
admin-web/src/pages/
├── TeachingAssignmentsPage.tsx (new)
├── HomeroomAssignmentsPage.tsx (new)
├── SchedulesPage.tsx (modify)
├── AssignmentsPage.tsx (delete or replace)
```

---

## Task 1: Add enums, entities, repositories

### Step 1: Create AssignmentStatus.java
```java
package vn.edu.fpt.myfschool.common.enums;

public enum AssignmentStatus {
    ACTIVE,
    INACTIVE  // soft delete / hết hiệu lực
}
```

### Step 2: Create TeachingAssignment entity
```java
package vn.edu.fpt.myfschool.controller.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import vn.edu.fpt.myfschool.common.enums.AssignmentStatus;
import java.time.LocalDate;

@Entity
@Table(name = "teaching_assignments",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"class_id", "subject_id", "semester_id", "effective_from"}))
@Data
@EqualsAndHashCode(callSuper = true)
public class TeachingAssignment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_ta_class"))
    private SchoolClass cls;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_ta_subject"))
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_ta_teacher"))
    private Teacher teacher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_ta_semester"))
    private Semester semester;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;  // null = đang hiệu lực

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AssignmentStatus status = AssignmentStatus.ACTIVE;
}
```

### Step 3: Create HomeroomAssignment entity
```java
package vn.edu.fpt.myfschool.controller.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;

@Entity
@Table(name = "homeroom_assignments",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"class_id", "academic_year_id", "effective_from"}))
@Data
@EqualsAndHashCode(callSuper = true)
public class HomeroomAssignment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_ha_class"))
    private SchoolClass cls;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_ha_teacher"))
    private Teacher teacher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_ha_academic_year"))
    private AcademicYear academicYear;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;
}
```

### Step 4: Create repositories

**TeachingAssignmentRepository.java:**
```java
package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.myfschool.common.enums.AssignmentStatus;
import vn.edu.fpt.myfschool.controller.entity.TeachingAssignment;
import java.util.List;
import java.util.Optional;

@Repository
public interface TeachingAssignmentRepository extends JpaRepository<TeachingAssignment, Long> {

    List<TeachingAssignment> findByClsIdAndSemesterIdAndStatus(
        Long classId, Long semesterId, AssignmentStatus status);

    List<TeachingAssignment> findByTeacherIdAndSemesterIdAndStatus(
        Long teacherId, Long semesterId, AssignmentStatus status);

    List<TeachingAssignment> findBySemesterIdAndStatus(
        Long semesterId, AssignmentStatus status);

    @Query("SELECT ta FROM TeachingAssignment ta " +
           "WHERE ta.cls.id = :classId AND ta.subject.id = :subjectId " +
           "AND ta.semester.id = :semesterId AND ta.status = 'ACTIVE' " +
           "AND (ta.effectiveTo IS NULL OR ta.effectiveTo >= CURRENT_DATE)")
    List<TeachingAssignment> findActiveByClassSubjectSemester(
        @Param("classId") Long classId,
        @Param("subjectId") Long subjectId,
        @Param("semesterId") Long semesterId);

    @Query("SELECT ta FROM TeachingAssignment ta " +
           "WHERE ta.cls.academicYear.id = :academicYearId " +
           "AND ta.status = 'ACTIVE'")
    List<TeachingAssignment> findByAcademicYearId(
        @Param("academicYearId") Long academicYearId);

    boolean existsByClsIdAndSubjectIdAndSemesterIdAndEffectiveFrom(
        Long classId, Long subjectId, Long semesterId, LocalDate effectiveFrom);
}
```

**HomeroomAssignmentRepository.java:**
```java
package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.myfschool.controller.entity.HomeroomAssignment;
import java.util.List;
import java.util.Optional;

@Repository
public interface HomeroomAssignmentRepository extends JpaRepository<HomeroomAssignment, Long> {

    List<HomeroomAssignment> findByClsIdAndAcademicYearId(Long classId, Long academicYearId);

    List<HomeroomAssignment> findByTeacherIdAndAcademicYearId(Long teacherId, Long academicYearId);

    @Query("SELECT ha FROM HomeroomAssignment ha " +
           "WHERE ha.cls.id = :classId AND ha.academicYear.id = :academicYearId " +
           "AND (ha.effectiveTo IS NULL OR ha.effectiveTo >= CURRENT_DATE)")
    Optional<HomeroomAssignment> findActiveByClassAndYear(
        @Param("classId") Long classId,
        @Param("academicYearId") Long academicYearId);
}
```

- [ ] **Step 1.1:** Create `AssignmentStatus.java`
- [ ] **Step 1.2:** Create `TeachingAssignment.java`
- [ ] **Step 1.3:** Create `HomeroomAssignment.java`
- [ ] **Step 1.4:** Create `TeachingAssignmentRepository.java`
- [ ] **Step 1.5:** Create `HomeroomAssignmentRepository.java`
- [ ] **Step 1.6:** Run compile check: `mvn -f backend/pom.xml compile`

---

## Task 2: Create DTOs

### TeachingAssignmentDto.java
```java
package vn.edu.fpt.myfschool.common.dto;

import vn.edu.fpt.myfschool.common.enums.AssignmentStatus;
import java.time.LocalDate;

public record TeachingAssignmentDto(
    Long id,
    Long classId, String className, Integer gradeLevel,
    Long subjectId, String subjectName, String subjectCode,
    Long teacherId, String teacherName, String teacherCode,
    Long semesterId, String semesterName,
    LocalDate effectiveFrom, LocalDate effectiveTo,
    AssignmentStatus status
) {}
```

### TeachingAssignmentDetailDto.java
```java
public record TeachingAssignmentDetailDto(
    TeachingAssignmentDto assignment,
    List<ScheduleDto> schedules  // các tiết TKB của assignment này
) {}
```

### CreateTeachingAssignmentRequest.java
```java
package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateTeachingAssignmentRequest(
    @NotNull Long classId,
    @NotNull Long subjectId,
    @NotNull Long teacherId,
    @NotNull Long semesterId,
    @NotNull LocalDate effectiveFrom
) {}
```

### HomeroomAssignmentDto.java
```java
public record HomeroomAssignmentDto(
    Long id,
    Long classId, String className,
    Long teacherId, String teacherName,
    Long academicYearId, String academicYearName,
    LocalDate effectiveFrom, LocalDate effectiveTo
) {}
```

### CreateHomeroomAssignmentRequest.java
```java
public record CreateHomeroomAssignmentRequest(
    @NotNull Long classId,
    @NotNull Long teacherId,
    @NotNull Long academicYearId,
    @NotNull LocalDate effectiveFrom
) {}
```

- [ ] **Step 2.1:** Create `TeachingAssignmentDto.java`
- [ ] **Step 2.2:** Create `TeachingAssignmentDetailDto.java`
- [ ] **Step 2.3:** Create `CreateTeachingAssignmentRequest.java`
- [ ] **Step 2.4:** Create `HomeroomAssignmentDto.java`
- [ ] **Step 2.5:** Create `CreateHomeroomAssignmentRequest.java`

---

## Task 3: Create Services

### TeachingAssignmentService.java
```java
package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.enums.AssignmentStatus;
import java.util.List;

public interface TeachingAssignmentService {
    List<TeachingAssignmentDto> listByClass(Long classId, Long semesterId, AssignmentStatus status);
    List<TeachingAssignmentDto> listByTeacher(Long teacherId, Long semesterId, AssignmentStatus status);
    TeachingAssignmentDto getById(Long id);
    TeachingAssignmentDetailDto getDetail(Long id);
    TeachingAssignmentDto create(CreateTeachingAssignmentRequest request);
    TeachingAssignmentDto update(Long id, CreateTeachingAssignmentRequest request);
    void deactivate(Long id); // soft delete, set status = INACTIVE
    void reactivate(Long id);
}
```

**TeachingAssignmentServiceImpl.java** key logic:
```java
@Override
public TeachingAssignmentDto create(CreateTeachingAssignmentRequest request) {
    SchoolClass cls = classRepository.findById(request.classId())
        .orElseThrow(() -> new ResourceNotFoundException("Class", "id", request.classId()));
    Subject subject = subjectRepository.findById(request.subjectId())
        .orElseThrow(() -> new ResourceNotFoundException("Subject", "id", request.subjectId()));
    Teacher teacher = teacherRepository.findById(request.teacherId())
        .orElseThrow(() -> new ResourceNotFoundException("Teacher", "id", request.teacherId()));
    Semester semester = semesterRepository.findById(request.semesterId())
        .orElseThrow(() -> new ResourceNotFoundException("Semester", "id", request.semesterId()));

    // Check conflict: same class+subject+semester with overlapping effective range
    if (teachingAssignmentRepository.existsByClsIdAndSubjectIdAndSemesterIdAndEffectiveFrom(
            request.classId(), request.subjectId(), request.semesterId(), request.effectiveFrom())) {
        throw new ConflictException("Đã có phân công cho môn này trong khoảng thời gian này");
    }

    TeachingAssignment ta = new TeachingAssignment();
    ta.setCls(cls);
    ta.setSubject(subject);
    ta.setTeacher(teacher);
    ta.setSemester(semester);
    ta.setEffectiveFrom(request.effectiveFrom());
    ta.setStatus(AssignmentStatus.ACTIVE);
    ta = teachingAssignmentRepository.save(ta);

    return toDto(ta);
}
```

### HomeroomAssignmentService.java
```java
public interface HomeroomAssignmentService {
    List<HomeroomAssignmentDto> listByClass(Long classId, Long academicYearId);
    HomeroomAssignmentDto getByClassAndYear(Long classId, Long academicYearId);
    HomeroomAssignmentDto create(CreateHomeroomAssignmentRequest request);
    HomeroomAssignmentDto update(Long id, CreateHomeroomAssignmentRequest request);
    void delete(Long id);
}
```

- [ ] **Step 3.1:** Create `TeachingAssignmentService.java` interface
- [ ] **Step 3.2:** Create `TeachingAssignmentServiceImpl.java`
- [ ] **Step 3.3:** Create `HomeroomAssignmentService.java` interface
- [ ] **Step 3.4:** Create `HomeroomAssignmentServiceImpl.java`
- [ ] **Step 3.5:** Compile check

---

## Task 4: Create Controllers

### TeachingAssignmentController.java
```java
@RestController
@RequestMapping("/api/teaching-assignments")
@RequiredArgsConstructor
@Tag(name = "Teaching Assignments", description = "Phân công giáo viên bộ môn")
@SecurityRequirement(name = "Bearer Authentication")
public class TeachingAssignmentController {

    private final TeachingAssignmentService teachingAssignmentService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "DS phân công theo lớp hoặc GV")
    public ResponseEntity<ApiResponse<List<TeachingAssignmentDto>>> list(
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) Long semesterId,
            @RequestParam(defaultValue = "ACTIVE") AssignmentStatus status) {
        if (classId != null) {
            return ResponseEntity.ok(ApiResponse.success(
                teachingAssignmentService.listByClass(classId, semesterId, status)));
        } else if (teacherId != null) {
            return ResponseEntity.ok(ApiResponse.success(
                teachingAssignmentService.listByTeacher(teacherId, semesterId, status)));
        }
        throw new BadRequestException("Phải cung cấp classId hoặc teacherId");
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Chi tiết phân công + TKB")
    public ResponseEntity<ApiResponse<TeachingAssignmentDetailDto>> getDetail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(teachingAssignmentService.getDetail(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Phân công GV bộ môn")
    public ResponseEntity<ApiResponse<TeachingAssignmentDto>> create(
            @Valid @RequestBody CreateTeachingAssignmentRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
            "Phân công thành công", teachingAssignmentService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Sửa phân công (đổi GV hoặc thời hạn)")
    public ResponseEntity<ApiResponse<TeachingAssignmentDto>> update(
            @PathVariable Long id, @Valid @RequestBody CreateTeachingAssignmentRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
            "Cập nhật thành công", teachingAssignmentService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Kết thúc phân công (soft delete)")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Long id) {
        teachingAssignmentService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.success("Đã kết thúc phân công", null));
    }

    @PostMapping("/{id}/reactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Khôi phục phân công")
    public ResponseEntity<ApiResponse<TeachingAssignmentDto>> reactivate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
            "Khôi phục thành công", teachingAssignmentService.reactivate(id)));
    }
}
```

### HomeroomAssignmentController.java
```java
@RestController
@RequestMapping("/api/homeroom-assignments")
@RequiredArgsConstructor
@Tag(name = "Homeroom Assignments", description = "Phân công GVCN")
@SecurityRequirement(name = "Bearer Authentication")
public class HomeroomAssignmentController {

    private final HomeroomAssignmentService homeroomAssignmentService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "DS GVCN")
    public ResponseEntity<ApiResponse<List<HomeroomAssignmentDto>>> list(
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long academicYearId) {
        return ResponseEntity.ok(ApiResponse.success(
            homeroomAssignmentService.listByClass(classId, academicYearId)));
    }

    @GetMapping("/current")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "GVCN hiện tại của lớp")
    public ResponseEntity<ApiResponse<HomeroomAssignmentDto>> getCurrent(
            @RequestParam Long classId, @RequestParam Long academicYearId) {
        return ResponseEntity.ok(ApiResponse.success(
            homeroomAssignmentService.getByClassAndYear(classId, academicYearId)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Gán GVCN")
    public ResponseEntity<ApiResponse<HomeroomAssignmentDto>> create(
            @Valid @RequestBody CreateHomeroomAssignmentRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
            "Gán GVCN thành công", homeroomAssignmentService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Đổi GVCN")
    public ResponseEntity<ApiResponse<HomeroomAssignmentDto>> update(
            @PathVariable Long id, @Valid @RequestBody CreateHomeroomAssignmentRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
            "Cập nhật thành công", homeroomAssignmentService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa phân công GVCN")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        homeroomAssignmentService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa thành công", null));
    }
}
```

- [ ] **Step 4.1:** Create `TeachingAssignmentController.java`
- [ ] **Step 4.2:** Create `HomeroomAssignmentController.java`
- [ ] **Step 4.3:** Compile check

---

## Task 5: Refactor Schedule entity

### Schedule.java — OLD fields to REMOVE:
```java
// ❌ Xóa
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "class_id", nullable = false)
private SchoolClass cls;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "subject_id", nullable = false)
private Subject subject;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "teacher_id", nullable = false)
private Teacher teacher;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "semester_id", nullable = false)
private Semester semester;
```

### Schedule.java — NEW field:
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "assignment_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_sch_assignment"))
private TeachingAssignment assignment;

// Giữ lại:
@Column(name = "day_of_week", nullable = false)
private Integer dayOfWeek;

@Column(nullable = false)
private Integer period;

@Column(length = 20)
private String room;

@Enumerated(EnumType.STRING)
@Column(nullable = false, length = 20)
private Shift shift = Shift.MORNING;
```

### Unique constraint mới:
```java
@Table(name = "schedules",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"assignment_id", "day_of_week", "period"}))
```

### ScheduleDto.java — NEW:
```java
public record ScheduleDto(
    Long id,
    Long assignmentId,
    Long classId, String className,
    Long subjectId, String subjectName,
    Long teacherId, String teacherName,
    Long semesterId, String semesterName,
    Integer dayOfWeek, String dayOfWeekName,
    Integer period, String room, Shift shift
) {}
```

### ScheduleRequest.java — NEW:
```java
public record ScheduleRequest(
    @NotNull Long assignmentId,
    @NotNull @Min(1) @Max(7) Integer dayOfWeek,
    @NotNull @Min(1) @Max(10) Integer period,
    String room,
    @NotNull Shift shift
) {}
```

### ScheduleRepository.java — THÊM query:
```java
List<Schedule> findByAssignmentId(Long assignmentId);

@Query("SELECT s FROM Schedule s WHERE s.assignment.cls.id = :classId " +
       "AND s.assignment.semester.id = :semesterId " +
       "ORDER BY s.dayOfWeek ASC, s.period ASC")
List<Schedule> findByClassIdAndSemesterId(@Param("classId") Long classId,
                                           @Param("semesterId") Long semesterId);

@Query("SELECT s FROM Schedule s WHERE s.assignment.teacher.id = :teacherId " +
       "AND s.assignment.semester.id = :semesterId " +
       "ORDER BY s.dayOfWeek ASC, s.period ASC")
List<Schedule> findByTeacherIdAndSemesterId(@Param("teacherId") Long teacherId,
                                             @Param("semesterId") Long semesterId);

// Check conflict
Optional<Schedule> findByAssignmentIdAndDayOfWeekAndPeriod(
    Long assignmentId, Integer dayOfWeek, Integer period);
```

### ScheduleServiceImpl.java — resolve from assignment:
```java
private ScheduleDto toDto(Schedule s) {
    TeachingAssignment ta = s.getAssignment();
    SchoolClass cls = ta.getCls();
    Subject subject = ta.getSubject();
    Teacher teacher = ta.getTeacher();
    Semester semester = ta.getSemester();
    return new ScheduleDto(
        s.getId(),
        ta.getId(),
        cls.getId(), cls.getName(),
        subject.getId(), subject.getName(),
        teacher.getId(), teacher.getUser().getName(),
        semester.getId(), semester.getName(),
        s.getDayOfWeek(), getDayName(s.getDayOfWeek()),
        s.getPeriod(), s.getRoom(), s.getShift()
    );
}
```

### getAvailablePeriods — query by class+semester:
```java
@Query("SELECT s FROM Schedule s WHERE s.assignment.cls.id = :classId " +
       "AND s.assignment.semester.id = :semesterId")
List<Schedule> findByClassSemester(@Param("classId") Long classId,
                                   @Param("semesterId") Long semesterId);
```

- [ ] **Step 5.1:** Sửa `Schedule.java` — xóa 4 FK, thêm `assignment_id`
- [ ] **Step 5.2:** Cập nhật `ScheduleDto.java`
- [ ] **Step 5.3:** Cập nhật `ScheduleRequest.java`
- [ ] **Step 5.4:** Cập nhật `ScheduleRepository.java`
- [ ] **Step 5.5:** Sửa `ScheduleServiceImpl.java` — resolve từ assignment
- [ ] **Step 5.6:** Sửa `ScheduleController.java` — endpoints giữ nguyên, chỉ đổi param
- [ ] **Step 5.7:** Compile check

---

## Task 6: Modify ClassController (remove assign/remove subject)

### ClassController — XÓA:
```java
// ❌ Xóa 2 endpoints:
// POST /api/classes/{id}/subjects
// DELETE /api/classes/subjects/{classSubjectId}
```

### ClassService — XÓA:
```java
// ❌ Xóa 2 methods:
// assignSubject(CreateClassSubjectRequest)
// removeSubject(Long classSubjectId)
```

### ClassDetailDto — thay thế ClassSubject list thành TeachingAssignment list:
```java
public record ClassDetailDto(
    Long id, String name, Integer gradeLevel,
    Long academicYearId, String academicYearName, String schoolName,
    List<StudentSummaryDto> students,
    List<TeachingAssignmentDto> assignments,  // ← thay vì subjects
    HomeroomAssignmentDto homeroomTeacher     // ← thêm GVCN
) {}
```

### ClassServiceImpl — sửa getClassDetail:
```java
@Override
public ClassDetailDto getClassDetail(Long classId) {
    SchoolClass cls = classRepository.findById(classId)
        .orElseThrow(() -> new ResourceNotFoundException("Class", "id", classId));
    // Students
    List<StudentSummaryDto> students = enrollmentRepository
        .findActiveStudentsByClassAndYear(classId, cls.getAcademicYear().getId())
        .stream().map(s -> new StudentSummaryDto(...)).toList();
    // Teaching assignments
    List<TeachingAssignmentDto> assignments = teachingAssignmentRepository
        .findByClsIdAndSemesterIdAndStatus(classId, null, AssignmentStatus.ACTIVE)
        .stream().map(this::toDto).toList();
    // Homeroom
    HomeroomAssignmentDto homeroom = homeroomAssignmentRepository
        .findActiveByClassAndYear(classId, cls.getAcademicYear().getId())
        .map(this::toDto).orElse(null);
    return new ClassDetailDto(cls.getId(), cls.getName(), cls.getGradeLevel(),
        cls.getAcademicYear().getId(), cls.getAcademicYear().getName(),
        cls.getSchoolName(), students, assignments, homeroom);
}
```

- [ ] **Step 6.1:** Sửa `ClassController.java` — xóa subject endpoints
- [ ] **Step 6.2:** Sửa `ClassService.java` interface
- [ ] **Step 6.3:** Sửa `ClassServiceImpl.java` — getClassDetail dùng TeachingAssignment
- [ ] **Step 6.4:** Sửa `ClassDetailDto.java`

---

## Task 7: Delete ClassSubject

- [ ] **Step 7.1:** Kiểm tra tất cả reference đến `ClassSubject` trong codebase
  ```bash
  grep -r "ClassSubject" backend/src/ --include="*.java"
  ```
- [ ] **Step 7.2:** Xóa `ClassSubject.java`
- [ ] **Step 7.3:** Xóa `ClassSubjectRepository.java`
- [ ] **Step 7.4:** Xóa `ClassSubjectDto.java`, `CreateClassSubjectRequest.java`
- [ ] **Step 7.5:** Compile check — không còn lỗi

---

## Task 8: Update tests

- [ ] **Step 8.1:** Thêm `TeachingAssignmentIntegrationTest.java` (test CRUD + conflict)
- [ ] **Step 8.2:** Thêm `HomeroomAssignmentIntegrationTest.java`
- [ ] **Step 8.3:** Sửa `ScheduleIntegrationTest` — dùng assignmentId thay vì classId+subjectId+teacherId
- [ ] **Step 8.4:** Sửa `ClassIntegrationTest` — dùng assignment subject list
- [ ] **Step 8.5:** Chạy test:
  ```bash
  mvn -f backend/pom.xml test -Dtest=TeachingAssignmentIntegrationTest,HomeroomAssignmentIntegrationTest
  mvn -f backend/pom.xml test
  ```

---

## Task 9: Admin-web

### TeachingAssignmentsPage.tsx
```tsx
// Features:
// 1. Select class → semester → load assignments
// 2. Table: Môn | GV | Hiệu lực từ | Hiệu lực đến | Trạng thái
// 3. Create: chọn class + subject + teacher + effectiveFrom
// 4. Deactivate: set status = INACTIVE
// 5. Xem TKB của assignment: expand row
```

### HomeroomAssignmentsPage.tsx
```tsx
// Features:
// 1. Select academic year → class → load homeroom
// 2. Card: Lớp | GVCN | Hiệu lực
// 3. Assign/Change GVCN
```

### SchedulesPage.tsx — sửa:
- Load assignments theo class + semester → dropdown chọn assignment
- Khi tạo schedule: chọn assignment, day, period, room, shift

- [ ] **Step 9.1:** Tạo `TeachingAssignmentsPage.tsx`
- [ ] **Step 9.2:** Tạo `HomeroomAssignmentsPage.tsx`
- [ ] **Step 9.3:** Sửa `SchedulesPage.tsx` — dùng assignmentId
- [ ] **Step 9.4:** Build:
  ```bash
  npm --prefix admin-web run build
  ```

---

## Task 10: Final verification

- [ ] **Step 10.1:** Backend all tests:
  ```bash
  mvn -f backend/pom.xml test
  ```
- [ ] **Step 10.2:** Admin-web build:
  ```bash
  npm --prefix admin-web run build
  ```
- [ ] **Step 10.3:** Manual smoke test:
  1. Login admin
  2. Tạo teaching assignment: POST `/api/teaching-assignments` → thành công
  3. Tạo schedule với assignmentId mới: POST `/api/schedules` → thành công
  4. GET `/api/classes/{id}` → trả về assignment list + homeroom teacher
  5. GET `/api/schedules/class?classId=1&semesterId=2` → schedule có đầy đủ thông tin từ assignment
  6. Deactivate assignment → schedule cũ vẫn còn, không tạo mới được

---

## Self-review checklist

- [ ] `TeachingAssignment` có unique (class_id, subject_id, semester_id, effective_from)
- [ ] `HomeroomAssignment` có unique (class_id, academic_year_id, effective_from)
- [ ] `Schedule` không còn lưu class_id/subject_id/teacher_id/semester_id
- [ ] `ClassSubject` đã xóa hoàn toàn
- [ ] `ClassDetailDto` trả về assignment list + homeroom thay vì subject list
- [ ] Không có backward-compatible code cho ClassSubject
- [ ] Tests pass
- [ ] Admin-web build pass
