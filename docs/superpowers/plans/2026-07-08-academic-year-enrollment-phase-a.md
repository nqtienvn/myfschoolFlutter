# Academic Year + Enrollment Phase A Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace string-based academic years with `AcademicYear` FK and replace `StudentClass` with `Enrollment`, using `academicYearId` in backend/admin-web APIs.

**Architecture:** Add `AcademicYear` as source of truth. `SchoolClass`, `Semester`, and `Enrollment` reference it by FK. Keep Phase A narrow: no teaching-assignment, schedule, fee, attendance, or grade-book refactor yet.

**Tech Stack:** Spring Boot 3.4.5, Spring Data JPA, Java 21, React 19, TypeScript, Vite. No new dependency.

## Global Constraints

- Work only on Phase A scope from `docs/superpowers/specs/2026-07-08-academic-year-enrollment-phase-a-design.md`.
- Use enum `DRAFT | ACTIVE | CLOSED` for `AcademicYear.status`.
- API changes to `academicYearId` now; no backward-compatible `academicYear` string API.
- Do not implement `TeachingAssignment`, `HomeroomAssignment`, `Schedule.assignment_id`, `FeeTemplate`, attendance split, grade-book split, or bulk APIs in this plan.
- Do not commit or push unless user explicitly asks.

---

## File Map

### Create

- `backend/src/main/java/vn/edu/fpt/myfschool/common/enums/AcademicYearStatus.java` — year lifecycle enum.
- `backend/src/main/java/vn/edu/fpt/myfschool/common/enums/EnrollmentStatus.java` — enrollment lifecycle enum.
- `backend/src/main/java/vn/edu/fpt/myfschool/controller/entity/AcademicYear.java` — academic year entity.
- `backend/src/main/java/vn/edu/fpt/myfschool/controller/entity/Enrollment.java` — replaces `StudentClass`.
- `backend/src/main/java/vn/edu/fpt/myfschool/repository/AcademicYearRepository.java`.
- `backend/src/main/java/vn/edu/fpt/myfschool/repository/EnrollmentRepository.java`.
- `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/AcademicYearDto.java`.
- `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/CreateAcademicYearRequest.java`.
- `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/UpdateAcademicYearStatusRequest.java`.
- `backend/src/main/java/vn/edu/fpt/myfschool/service/AcademicYearService.java`.
- `backend/src/main/java/vn/edu/fpt/myfschool/controller/AcademicYearController.java`.
- `backend/src/test/java/vn/edu/fpt/myfschool/AcademicYearIntegrationTest.java`.

### Modify

- `backend/src/main/java/vn/edu/fpt/myfschool/controller/entity/SchoolClass.java`.
- `backend/src/main/java/vn/edu/fpt/myfschool/controller/entity/Semester.java`.
- `backend/src/main/java/vn/edu/fpt/myfschool/controller/entity/Student.java`.
- `backend/src/main/java/vn/edu/fpt/myfschool/repository/ClassRepository.java`.
- `backend/src/main/java/vn/edu/fpt/myfschool/repository/SemesterRepository.java`.
- `backend/src/main/java/vn/edu/fpt/myfschool/repository/StudentRepository.java`.
- `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/ClassDto.java`.
- `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/ClassDetailDto.java`.
- `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/CreateClassRequest.java`.
- `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/SemesterDto.java`.
- `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/CreateSemesterRequest.java`.
- `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/StudentSemesterGradesDto.java`.
- `backend/src/main/java/vn/edu/fpt/myfschool/controller/ClassController.java`.
- `backend/src/main/java/vn/edu/fpt/myfschool/controller/SemesterController.java`.
- `backend/src/main/java/vn/edu/fpt/myfschool/service/ClassService.java`.
- `backend/src/main/java/vn/edu/fpt/myfschool/service/SemesterService.java`.
- `backend/src/main/java/vn/edu/fpt/myfschool/service/impl/ClassServiceImpl.java`.
- `backend/src/main/java/vn/edu/fpt/myfschool/service/impl/SemesterServiceImpl.java`.
- `backend/src/main/java/vn/edu/fpt/myfschool/service/impl/AttendanceServiceImpl.java`.
- `backend/src/main/java/vn/edu/fpt/myfschool/service/impl/GradeServiceImpl.java`.
- `backend/src/main/java/vn/edu/fpt/myfschool/repository/GradeRepository.java`.
- `backend/src/test/java/vn/edu/fpt/myfschool/BaseIntegrationTest.java`.
- Admin web pages: `ClassesPage.tsx`, `SemestersPage.tsx`, `AssignmentsPage.tsx`, `SchedulesPage.tsx`, `TuitionPage.tsx`.

### Delete/Stop using

- `backend/src/main/java/vn/edu/fpt/myfschool/controller/entity/StudentClass.java`.
- `backend/src/main/java/vn/edu/fpt/myfschool/repository/StudentClassRepository.java`.

---

## Task 1: Add AcademicYear backend API

**Files:**
- Create: enum/entity/repository/DTO/service/controller/test listed above.

**Interfaces:**
- Produces: `AcademicYearDto(Long id, String name, LocalDate startDate, LocalDate endDate, AcademicYearStatus status)`.
- Produces: `AcademicYearRepository extends JpaRepository<AcademicYear, Long>` with `findByStatus`, `findByName`, `existsByName`.
- Produces endpoints: `GET/POST/PUT /api/academic-years`, `PUT /api/academic-years/{id}/status`.

- [ ] **Step 1: Write failing integration test**

Create `backend/src/test/java/vn/edu/fpt/myfschool/AcademicYearIntegrationTest.java`:

```java
package vn.edu.fpt.myfschool;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AcademicYearIntegrationTest extends BaseIntegrationTest {

    @Test
    void createAcademicYear_adminRole_returnsCreatedYear() throws Exception {
        String token = loginAsAdmin();

        mockMvc.perform(post("/api/academic-years")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"2027-2028\",\"startDate\":\"2027-08-01\",\"endDate\":\"2028-05-31\",\"status\":\"DRAFT\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.name").value("2027-2028"))
            .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    void activateAcademicYear_deactivatesPreviousActiveYear() throws Exception {
        String token = loginAsAdmin();

        String body = "{\"name\":\"2028-2029\",\"startDate\":\"2028-08-01\",\"endDate\":\"2029-05-31\",\"status\":\"DRAFT\"}";
        String response = mockMvc.perform(post("/api/academic-years")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(response).path("data").path("id").asLong();

        mockMvc.perform(put("/api/academic-years/" + id + "/status")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"ACTIVE\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        mockMvc.perform(get("/api/academic-years")
                .header("Authorization", authHeader(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.status == 'ACTIVE')].id").isArray())
            .andExpect(jsonPath("$.data[?(@.status == 'ACTIVE')].id.length()").value(1));
    }
}
```

- [ ] **Step 2: Run red test**

```bash
mvn -f backend/pom.xml test -Dtest=AcademicYearIntegrationTest
```

Expected: compile fail because academic year API/classes do not exist.

- [ ] **Step 3: Add enums**

`backend/src/main/java/vn/edu/fpt/myfschool/common/enums/AcademicYearStatus.java`:

```java
package vn.edu.fpt.myfschool.common.enums;

public enum AcademicYearStatus {
    DRAFT,
    ACTIVE,
    CLOSED
}
```

`backend/src/main/java/vn/edu/fpt/myfschool/common/enums/EnrollmentStatus.java`:

```java
package vn.edu.fpt.myfschool.common.enums;

public enum EnrollmentStatus {
    ACTIVE,
    LEFT,
    TRANSFERRED
}
```

- [ ] **Step 4: Add AcademicYear entity/repository/DTOs**

`AcademicYear.java`:

```java
package vn.edu.fpt.myfschool.controller.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import vn.edu.fpt.myfschool.common.enums.AcademicYearStatus;

import java.time.LocalDate;

@Entity
@Table(name = "academic_years", uniqueConstraints = @UniqueConstraint(columnNames = "name"))
@Data
@EqualsAndHashCode(callSuper = true)
public class AcademicYear extends BaseEntity {

    @Column(nullable = false, unique = true, length = 20)
    private String name;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AcademicYearStatus status = AcademicYearStatus.DRAFT;
}
```

`AcademicYearRepository.java`:

```java
package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.myfschool.common.enums.AcademicYearStatus;
import vn.edu.fpt.myfschool.controller.entity.AcademicYear;

import java.util.List;
import java.util.Optional;

@Repository
public interface AcademicYearRepository extends JpaRepository<AcademicYear, Long> {
    boolean existsByName(String name);
    Optional<AcademicYear> findByName(String name);
    List<AcademicYear> findByStatus(AcademicYearStatus status);
}
```

DTOs:

```java
package vn.edu.fpt.myfschool.common.dto;

import vn.edu.fpt.myfschool.common.enums.AcademicYearStatus;
import java.time.LocalDate;

public record AcademicYearDto(Long id, String name, LocalDate startDate, LocalDate endDate, AcademicYearStatus status) {}
```

```java
package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import vn.edu.fpt.myfschool.common.enums.AcademicYearStatus;

import java.time.LocalDate;

public record CreateAcademicYearRequest(
    @NotBlank @Size(max = 20) String name,
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate,
    AcademicYearStatus status
) {}
```

```java
package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.NotNull;
import vn.edu.fpt.myfschool.common.enums.AcademicYearStatus;

public record UpdateAcademicYearStatusRequest(@NotNull AcademicYearStatus status) {}
```

- [ ] **Step 5: Add AcademicYearService**

`backend/src/main/java/vn/edu/fpt/myfschool/service/AcademicYearService.java`:

```java
package vn.edu.fpt.myfschool.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.AcademicYearDto;
import vn.edu.fpt.myfschool.common.dto.CreateAcademicYearRequest;
import vn.edu.fpt.myfschool.common.enums.AcademicYearStatus;
import vn.edu.fpt.myfschool.common.exception.ConflictException;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.controller.entity.AcademicYear;
import vn.edu.fpt.myfschool.repository.AcademicYearRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AcademicYearService {

    private final AcademicYearRepository academicYearRepository;

    @Transactional(readOnly = true)
    public List<AcademicYearDto> listAcademicYears() {
        return academicYearRepository.findAll().stream().map(this::toDto).toList();
    }

    public AcademicYearDto createAcademicYear(CreateAcademicYearRequest request) {
        if (academicYearRepository.existsByName(request.name())) {
            throw new ConflictException("Năm học đã tồn tại");
        }
        AcademicYear year = new AcademicYear();
        year.setName(request.name());
        year.setStartDate(request.startDate());
        year.setEndDate(request.endDate());
        year.setStatus(request.status() != null ? request.status() : AcademicYearStatus.DRAFT);
        if (year.getStatus() == AcademicYearStatus.ACTIVE) deactivateActiveYears();
        return toDto(academicYearRepository.save(year));
    }

    public AcademicYearDto updateAcademicYear(Long id, CreateAcademicYearRequest request) {
        AcademicYear year = findEntity(id);
        academicYearRepository.findByName(request.name())
            .filter(existing -> !existing.getId().equals(id))
            .ifPresent(existing -> { throw new ConflictException("Năm học đã tồn tại"); });
        year.setName(request.name());
        year.setStartDate(request.startDate());
        year.setEndDate(request.endDate());
        year.setStatus(request.status() != null ? request.status() : year.getStatus());
        if (year.getStatus() == AcademicYearStatus.ACTIVE) deactivateOtherActiveYears(id);
        return toDto(academicYearRepository.save(year));
    }

    public AcademicYearDto updateStatus(Long id, AcademicYearStatus status) {
        AcademicYear year = findEntity(id);
        if (status == AcademicYearStatus.ACTIVE) deactivateOtherActiveYears(id);
        year.setStatus(status);
        return toDto(academicYearRepository.save(year));
    }

    public AcademicYear findEntity(Long id) {
        return academicYearRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("AcademicYear", "id", id));
    }

    private void deactivateActiveYears() {
        academicYearRepository.findByStatus(AcademicYearStatus.ACTIVE)
            .forEach(year -> year.setStatus(AcademicYearStatus.CLOSED));
    }

    private void deactivateOtherActiveYears(Long activeId) {
        academicYearRepository.findByStatus(AcademicYearStatus.ACTIVE).stream()
            .filter(year -> !year.getId().equals(activeId))
            .forEach(year -> year.setStatus(AcademicYearStatus.CLOSED));
    }

    private AcademicYearDto toDto(AcademicYear year) {
        return new AcademicYearDto(year.getId(), year.getName(), year.getStartDate(), year.getEndDate(), year.getStatus());
    }
}
```

- [ ] **Step 6: Add AcademicYearController**

`backend/src/main/java/vn/edu/fpt/myfschool/controller/AcademicYearController.java`:

```java
package vn.edu.fpt.myfschool.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.service.AcademicYearService;

import java.util.List;

@RestController
@RequestMapping("/api/academic-years")
@RequiredArgsConstructor
@Tag(name = "Academic Years", description = "Quản lý năm học")
@SecurityRequirement(name = "Bearer Authentication")
public class AcademicYearController {

    private final AcademicYearService academicYearService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PARENT', 'STUDENT', 'TEACHER')")
    @Operation(summary = "Danh sách năm học")
    public ResponseEntity<ApiResponse<List<AcademicYearDto>>> listAcademicYears() {
        return ResponseEntity.ok(ApiResponse.success(academicYearService.listAcademicYears()));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tạo năm học")
    public ResponseEntity<ApiResponse<AcademicYearDto>> createAcademicYear(
            @Valid @RequestBody CreateAcademicYearRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Tạo năm học thành công", academicYearService.createAcademicYear(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Sửa năm học")
    public ResponseEntity<ApiResponse<AcademicYearDto>> updateAcademicYear(
            @PathVariable Long id,
            @Valid @RequestBody CreateAcademicYearRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật năm học thành công", academicYearService.updateAcademicYear(id, request)));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Đổi trạng thái năm học")
    public ResponseEntity<ApiResponse<AcademicYearDto>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAcademicYearStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái thành công", academicYearService.updateStatus(id, request.status())));
    }
}
```

- [ ] **Step 7: Run green test for Task 1**

```bash
mvn -f backend/pom.xml test -Dtest=AcademicYearIntegrationTest
```

Expected: pass after `BaseIntegrationTest` is updated in Task 4; if it fails only because `BaseIntegrationTest` still lacks `objectMapper`, add `@Autowired protected ObjectMapper objectMapper;` there in Task 4.

---

## Task 2: Convert SchoolClass and Semester to academicYearId

**Files:** entities, DTOs, repositories, class/semester services/controllers.

**Interfaces:**
- `CreateClassRequest(Long academicYearId)`.
- `ClassDto(..., Long academicYearId, String academicYearName, ...)`.
- `CreateSemesterRequest(Long academicYearId, Integer order)`.
- `SemesterDto(..., Long academicYearId, String academicYearName, Integer order, ...)`.

- [ ] **Step 1: Update backend DTOs**

`CreateClassRequest.java`:

```java
public record CreateClassRequest(
    @NotBlank @Size(max = 20) String name,
    @NotNull @Min(1) @Max(12) Integer gradeLevel,
    @NotNull Long academicYearId,
    @Size(max = 200) String schoolName
) {}
```

`ClassDto.java`:

```java
public record ClassDto(
    Long id,
    String name,
    Integer gradeLevel,
    Long academicYearId,
    String academicYearName,
    String schoolName,
    Integer studentCount
) {}
```

`ClassDetailDto.java` mirrors `ClassDto` year fields:

```java
public record ClassDetailDto(
    Long id,
    String name,
    Integer gradeLevel,
    Long academicYearId,
    String academicYearName,
    String schoolName,
    List<StudentSummaryDto> students,
    List<ClassSubjectDto> subjects
) {}
```

`CreateSemesterRequest.java`:

```java
public record CreateSemesterRequest(
    @NotBlank @Size(max = 50) String name,
    @NotNull Long academicYearId,
    @NotNull Integer order,
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate,
    Boolean isCurrent
) {}
```

`SemesterDto.java`:

```java
public record SemesterDto(
    Long id,
    String name,
    Long academicYearId,
    String academicYearName,
    Integer order,
    LocalDate startDate,
    LocalDate endDate,
    Boolean isCurrent
) {}
```

- [ ] **Step 2: Update entities**

`SchoolClass.java`: replace `String academicYear` with `AcademicYear academicYear` and update unique constraint:

```java
@Table(name = "classes",
       uniqueConstraints = @UniqueConstraint(columnNames = {"name", "academic_year_id"}))
```

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "academic_year_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_classes_academic_year"))
private AcademicYear academicYear;
```

`Semester.java`: replace unique constraint and field:

```java
@Table(name = "semesters",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"name", "academic_year_id"}),
           @UniqueConstraint(columnNames = {"academic_year_id", "semester_order"})
       })
```

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "academic_year_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_semesters_academic_year"))
private AcademicYear academicYear;

@Column(name = "semester_order", nullable = false)
private Integer order;
```

- [ ] **Step 3: Update repositories**

`ClassRepository.java`:

```java
List<SchoolClass> findByAcademicYearId(Long academicYearId);
Optional<SchoolClass> findByNameAndAcademicYearId(String name, Long academicYearId);
boolean existsByNameAndAcademicYearId(String name, Long academicYearId);

@Query("SELECT c FROM SchoolClass c WHERE c.academicYear.id = :academicYearId AND " +
       "(LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')))")
List<SchoolClass> searchByYearAndKeyword(@Param("academicYearId") Long academicYearId,
                                          @Param("keyword") String keyword);
```

`SemesterRepository.java`:

```java
List<Semester> findByAcademicYearIdOrderByOrderAsc(Long academicYearId);
Optional<Semester> findByNameAndAcademicYearId(String name, Long academicYearId);
List<Semester> findByAcademicYearId(Long academicYearId);
```

- [ ] **Step 4: Update service interfaces/controllers signatures**

`ClassService.java`:

```java
Page<ClassDto> listClasses(Long academicYearId, String keyword, int page, int size);
```

`ClassController.java` list param:

```java
@RequestParam(required = false) Long academicYearId
```

`SemesterService.java`:

```java
List<SemesterDto> listSemesters(Long academicYearId);
```

`SemesterController.java` list param:

```java
@RequestParam(required = false) Long academicYearId
```

- [ ] **Step 5: Update ClassServiceImpl**

Inject `AcademicYearService` or `AcademicYearRepository`. Use existing service to centralize not-found behavior:

```java
private final AcademicYearService academicYearService;
```

In `listClasses`, use active/default year only if no param:

```java
Long yearId = academicYearId;
if (yearId == null) {
    yearId = academicYearRepository.findByStatus(AcademicYearStatus.ACTIVE).stream()
        .findFirst()
        .orElseThrow(() -> new ResourceNotFoundException("AcademicYear", "status", AcademicYearStatus.ACTIVE))
        .getId();
}
```

Then call `findByAcademicYearId` / `searchByYearAndKeyword`.

In create/update:

```java
AcademicYear year = academicYearService.findEntity(request.academicYearId());
if (classRepository.existsByNameAndAcademicYearId(request.name(), year.getId())) {
    throw new ConflictException("Lớp đã tồn tại trong năm học này");
}
cls.setAcademicYear(year);
```

In DTO construction, return:

```java
new ClassDto(cls.getId(), cls.getName(), cls.getGradeLevel(),
    cls.getAcademicYear().getId(), cls.getAcademicYear().getName(),
    cls.getSchoolName(), cls.getStudents().size())
```

- [ ] **Step 6: Update SemesterServiceImpl**

Use `academicYearService.findEntity(request.academicYearId())`, set `semester.setAcademicYear(year)`, set `semester.setOrder(request.order())`, list by `academicYearId` sorted by `order`.

When setting current semester, clear current only within same academic year:

```java
semesterRepository.findByAcademicYearId(semester.getAcademicYear().getId())
    .forEach(s -> s.setIsCurrent(false));
```

- [ ] **Step 7: Run targeted compile/test**

```bash
mvn -f backend/pom.xml test -Dtest=AcademicYearIntegrationTest,AdminRoleIntegrationTest
```

Expected: compile errors remain where `currentClass`/`StudentClass` are still used; Task 3 handles them.

---

## Task 3: Replace StudentClass with Enrollment

**Files:** `Student.java`, `StudentRepository.java`, `GradeRepository.java`, `AttendanceServiceImpl.java`, `GradeServiceImpl.java`, class service student listing/deletion checks.

**Interfaces:**
- Produces `EnrollmentRepository` with active enrollment queries.
- `Student.currentClass` can stay during Phase A as a denormalized shortcut if needed, but class membership queries must use `Enrollment`.

- [ ] **Step 1: Add Enrollment entity/repository**

`Enrollment.java`:

```java
package vn.edu.fpt.myfschool.controller.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import vn.edu.fpt.myfschool.common.enums.EnrollmentStatus;

import java.time.LocalDate;

@Entity
@Table(name = "enrollments",
       uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "class_id", "academic_year_id"}))
@Data
@EqualsAndHashCode(callSuper = true)
public class Enrollment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_enrollments_student"))
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_enrollments_class"))
    private SchoolClass cls;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_enrollments_academic_year"))
    private AcademicYear academicYear;

    @Column(nullable = false)
    private LocalDate joinDate;

    private LocalDate leaveDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EnrollmentStatus status = EnrollmentStatus.ACTIVE;
}
```

`EnrollmentRepository.java`:

```java
package vn.edu.fpt.myfschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.myfschool.common.enums.EnrollmentStatus;
import vn.edu.fpt.myfschool.controller.entity.Enrollment;
import vn.edu.fpt.myfschool.controller.entity.Student;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    List<Enrollment> findByStudentId(Long studentId);
    Optional<Enrollment> findByStudentIdAndAcademicYearIdAndStatus(Long studentId, Long academicYearId, EnrollmentStatus status);
    List<Enrollment> findByClsIdAndAcademicYearIdAndStatus(Long classId, Long academicYearId, EnrollmentStatus status);

    @Query("SELECT e.student FROM Enrollment e WHERE e.cls.id = :classId AND e.academicYear.id = :academicYearId AND e.status = 'ACTIVE'")
    List<Student> findActiveStudentsByClassAndYear(@Param("classId") Long classId, @Param("academicYearId") Long academicYearId);
}
```

- [ ] **Step 2: Update Student entity**

Replace:

```java
@OneToMany(mappedBy = "student", fetch = FetchType.LAZY)
private List<StudentClass> studentClasses = new ArrayList<>();
```

With:

```java
@OneToMany(mappedBy = "student", fetch = FetchType.LAZY)
private List<Enrollment> enrollments = new ArrayList<>();
```

Keep `currentClass` in Phase A to reduce blast radius. Add `// ponytail:` comment:

```java
// ponytail: denormalized shortcut kept for old dashboard paths; remove after all class membership queries use Enrollment.
```

- [ ] **Step 3: Replace StudentRepository current-class list queries where class/year matters**

Keep `findByCurrentClassId` only where not yet migrated. Add new repository method if needed:

```java
@Query("SELECT s FROM Student s JOIN Enrollment e ON e.student = s " +
       "WHERE e.cls.id = :classId AND e.academicYear.id = :academicYearId AND e.status = 'ACTIVE'")
List<Student> findActiveByClassAndAcademicYear(@Param("classId") Long classId, @Param("academicYearId") Long academicYearId);
```

Use either this method or `EnrollmentRepository.findActiveStudentsByClassAndYear`, not both in same service. Prefer `EnrollmentRepository` for clarity.

- [ ] **Step 4: Update ClassServiceImpl student reads**

In `getClassDetail`, `getStudentsInClass`, and `deleteClass`, resolve class first, get `classId` + `cls.getAcademicYear().getId()`, then use enrollment repository active students.

Example:

```java
List<Student> students = enrollmentRepository.findActiveStudentsByClassAndYear(classId, cls.getAcademicYear().getId());
```

- [ ] **Step 5: Update GradeRepository class query**

Replace `s.currentClass.id = :classId` query with Enrollment join:

```java
@Query("SELECT g FROM Grade g JOIN Enrollment e ON e.student = g.student " +
       "WHERE g.subject.id = :subjectId AND g.semester.id = :semesterId " +
       "AND e.cls.id = :classId AND e.academicYear.id = g.semester.academicYear.id AND e.status = 'ACTIVE'")
List<Grade> findBySubjectSemesterClass(@Param("subjectId") Long subjectId,
                                        @Param("semesterId") Long semesterId,
                                        @Param("classId") Long classId);
```

- [ ] **Step 6: Update AttendanceServiceImpl and GradeServiceImpl**

When a service needs students for a class/semester, resolve `Semester`, get `semester.getAcademicYear().getId()`, then use `enrollmentRepository.findActiveStudentsByClassAndYear(classId, academicYearId)`.

- [ ] **Step 7: Remove old StudentClass files**

Delete:

- `backend/src/main/java/vn/edu/fpt/myfschool/controller/entity/StudentClass.java`
- `backend/src/main/java/vn/edu/fpt/myfschool/repository/StudentClassRepository.java`

Only delete after compile references are gone.

---

## Task 4: Update test seed and backend tests

**Files:** `BaseIntegrationTest.java`, existing integration tests that create classes/semesters.

- [ ] **Step 1: Update BaseIntegrationTest fields/imports**

Add fields:

```java
protected AcademicYear testAcademicYear;
```

Inject repositories/services as needed:

```java
@Autowired protected AcademicYearRepository academicYearRepository;
@Autowired protected EnrollmentRepository enrollmentRepository;
@Autowired protected ObjectMapper objectMapper;
```

- [ ] **Step 2: Create AcademicYear before class/semester seed**

At start of `setUpTestData()`:

```java
testAcademicYear = new AcademicYear();
testAcademicYear.setName("2026-2027");
testAcademicYear.setStartDate(LocalDate.of(2026, 8, 1));
testAcademicYear.setEndDate(LocalDate.of(2027, 5, 31));
testAcademicYear.setStatus(AcademicYearStatus.ACTIVE);
testAcademicYear = academicYearRepository.save(testAcademicYear);
```

- [ ] **Step 3: Replace class/semester seed strings with FK**

For each `SchoolClass`:

```java
testClass.setAcademicYear(testAcademicYear);
```

For `Semester`:

```java
testSemester.setAcademicYear(testAcademicYear);
testSemester.setOrder(1);
```

- [ ] **Step 4: Create Enrollment for seeded students**

After each student save:

```java
Enrollment enrollment = new Enrollment();
enrollment.setStudent(s);
enrollment.setCls(testClass);
enrollment.setAcademicYear(testAcademicYear);
enrollment.setJoinDate(LocalDate.of(2026, 8, 1));
enrollment.setStatus(EnrollmentStatus.ACTIVE);
enrollmentRepository.save(enrollment);
```

Keep `s.setCurrentClass(testClass)` for Phase A compatibility.

- [ ] **Step 5: Update JSON bodies in tests**

Replace `academicYear: "2026-2027"` with `academicYearId: testAcademicYear.getId()` in Java string bodies. Use string concatenation:

```java
"{\"name\":\"HK II\",\"academicYearId\":" + testAcademicYear.getId() + ",\"order\":2,\"startDate\":\"2027-01-20\",\"endDate\":\"2027-05-31\",\"isCurrent\":false}"
```

- [ ] **Step 6: Run backend tests**

```bash
mvn -f backend/pom.xml test
```

Expected: backend compiles; failing assertions should be only DTO shape changes. Update assertions from `academicYear` to `academicYearId`/`academicYearName`.

---

## Task 5: Update admin-web for academicYearId

**Files:**
- `admin-web/src/pages/ClassesPage.tsx`
- `admin-web/src/pages/SemestersPage.tsx`
- `admin-web/src/pages/AssignmentsPage.tsx`
- `admin-web/src/pages/SchedulesPage.tsx`
- `admin-web/src/pages/TuitionPage.tsx`

**Interfaces:**

```ts
interface AcademicYearItem {
  id: number;
  name: string;
  status: string;
}
```

- [ ] **Step 1: Add minimal academic year load helper per page**

Do not create shared component in Phase A. Add local state where needed:

```ts
const [academicYears, setAcademicYears] = useState<AcademicYearItem[]>([]);
const [academicYearId, setAcademicYearId] = useState('');
```

Load:

```ts
async function fetchAcademicYears() {
  const data = await apiFetch('/academic-years') as AcademicYearItem[];
  setAcademicYears(data);
  const active = data.find(y => y.status === 'ACTIVE') || data[0];
  if (active) setAcademicYearId(String(active.id));
}
```

- [ ] **Step 2: Update ClassesPage**

Change `ClassItem`:

```ts
interface ClassItem {
  id: number;
  name: string;
  gradeLevel: number;
  academicYearId: number;
  academicYearName: string;
}
```

Fetch classes:

```ts
const data = await apiFetch(`/classes?academicYearId=${academicYearId}&page=0&size=100`);
```

Create class payload:

```ts
academicYearId: +academicYearId
```

Display:

```tsx
<td>{c.academicYearName}</td>
```

CSV import should select year once from dropdown; remove per-row academic year parsing in Phase A.

- [ ] **Step 3: Update SemestersPage**

Replace text `academicYear` input with dropdown from `/academic-years`. Payload:

```ts
academicYearId: +academicYearId,
order: semesterOrder
```

Display `s.academicYearName` and `s.order`.

- [ ] **Step 4: Update AssignmentsPage/SchedulesPage/TuitionPage types**

Change interfaces:

```ts
interface ClassItem { id: number; name: string; academicYearId: number; academicYearName: string; }
interface SemesterItem { id: number; name: string; academicYearId: number; academicYearName: string; isCurrent: boolean; order: number; }
```

Fetch classes/semesters with selected `academicYearId`.

- [ ] **Step 5: Build admin web**

```bash
npm --prefix admin-web run build
```

Expected: TypeScript and Vite build pass.

---

## Task 6: Final verification

- [ ] **Step 1: Backend targeted tests**

```bash
mvn -f backend/pom.xml test -Dtest=AcademicYearIntegrationTest,AdminRoleIntegrationTest,ClassIntegrationTest,SemesterIntegrationTest
```

If `ClassIntegrationTest` or `SemesterIntegrationTest` files do not exist, run:

```bash
mvn -f backend/pom.xml test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 2: Admin web build**

```bash
npm --prefix admin-web run build
```

Expected: build success.

- [ ] **Step 3: Manual smoke**

1. Login admin.
2. Open academic years API page or use devtools: `GET /api/academic-years` returns active `2026-2027`.
3. Create class with selected year: payload uses `academicYearId`.
4. List classes: request uses `academicYearId`; table shows `academicYearName`.
5. Create semester: payload uses `academicYearId` and `order`.
6. Class detail shows students from `Enrollment`.

## Self-review checklist

- Spec coverage: AcademicYear, Class/Semester FK, Enrollment, admin-web ID switch covered.
- Out-of-scope items are explicitly excluded.
- No new dependencies.
- No commit/push steps.
