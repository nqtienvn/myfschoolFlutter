# Phase H: Master Data Tables

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Tạo bảng danh mục độc lập cho các giá trị dùng nhiều lần: `School`, `GradeLevel`, `SchoolShift`, `Period`, `Room`. Tránh hardcode trong code.

**Architecture:**
```
School (multi-campus support)
GradeLevel (khối 1-12)
SchoolShift (Sáng/Chiều) ─── Period (Tiết 1-10)
Room (A101, B202...)
```

**Tech Stack:** Spring Boot 3.4.5, Spring Data JPA, Java 21, React 19. No new dependency.

## Global Constraints
- Tất cả là master data, ít thay đổi, cache 24h
- Các bảng này độc lập, không phụ thuộc Phase khác
- Có thể làm song song với Phase B-G
- `School` optional (nếu multi-school thì cần, không thì dùng default)

---

## File Map

### Create (20 files)

```
backend/src/main/java/vn/edu/fpt/myfschool/
├── controller/entity/School.java
├── controller/entity/GradeLevel.java
├── controller/entity/SchoolShift.java
├── controller/entity/Period.java
├── controller/entity/Room.java
├── repository/SchoolRepository.java
├── repository/GradeLevelRepository.java
├── repository/SchoolShiftRepository.java
├── repository/PeriodRepository.java
├── repository/RoomRepository.java
├── common/dto/SchoolDto.java
├── common/dto/CreateSchoolRequest.java
├── common/dto/GradeLevelDto.java
├── common/dto/SchoolShiftDto.java
├── common/dto/PeriodDto.java
├── common/dto/RoomDto.java
├── service/MasterDataService.java (gộp tất cả read-only master data)
├── controller/MasterDataController.java
├── controller/SchoolController.java (riêng vì có CRUD)
├── controller/RoomController.java (riêng vì có CRUD)
```

### Modify
```
backend/src/main/java/vn/edu/fpt/myfschool/
├── controller/entity/SchoolClass.java (thêm schoolId FK optional)
├── controller/entity/Schedule.java (thêm periodId FK optional -> thay period int)
```

### Admin web
```
admin-web/src/pages/
├── MasterDataPage.tsx (new — quản lý tất cả master data 1 trang)
├── SchoolPage.tsx (riêng nếu multi-school)
```

---

## Task 1: Entities

### School.java
```java
@Entity
@Table(name = "schools")
@Data
@EqualsAndHashCode(callSuper = true)
public class School extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 20, unique = true)
    private String code;

    @Column(length = 500)
    private String address;

    @Column(length = 20)
    private String phone;

    @Column(nullable = false, length = 200)
    private String schoolName = "FPT Schools"; // display name
}
```

### GradeLevel.java
```java
@Entity
@Table(name = "grade_levels",
       uniqueConstraints = @UniqueConstraint(columnNames = {"code", "order"}))
@Data
@EqualsAndHashCode(callSuper = true)
public class GradeLevel extends BaseEntity {

    @Column(nullable = false, length = 20)
    private String name; // "Khối 10", "Grade 10"

    @Column(nullable = false, length = 10)
    private String code; // "10"

    @Column(nullable = false)
    private Integer order; // 1-12

    @Column(length = 200)
    private String description;
}
```

### SchoolShift.java
```java
@Entity
@Table(name = "school_shifts")
@Data
@EqualsAndHashCode(callSuper = true)
public class SchoolShift extends BaseEntity {

    @Column(nullable = false, length = 50)
    private String name; // "Sáng", "Chiều"

    @Column(nullable = false, length = 20, unique = true)
    private String code; // "MORNING", "AFTERNOON"

    @Column(nullable = false)
    private Integer order; // 1, 2
}
```

### Period.java
```java
@Entity
@Table(name = "periods")
@Data
@EqualsAndHashCode(callSuper = true)
public class Period extends BaseEntity {

    @Column(nullable = false, length = 20)
    private String name; // "Tiết 1"

    @Column(nullable = false)
    private Integer order; // 1-10

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_period_shift"))
    private SchoolShift shift;

    @Column(nullable = false)
    private Boolean isActive = true;
}
```

### Room.java
```java
@Entity
@Table(name = "rooms")
@Data
@EqualsAndHashCode(callSuper = true)
public class Room extends BaseEntity {

    @Column(nullable = false, length = 20, unique = true)
    private String name; // "A101", "Lab202"

    @Column(nullable = false)
    private Integer capacity;

    @Column(length = 20)
    private String building; // "A", "B"

    @Column(length = 100)
    private String equipment; // "Máy chiếu, Máy lạnh"

    @Column(nullable = false)
    private Boolean isActive = true;
}
```

---

## Task 2: Repositories

### SchoolRepository.java
```java
public interface SchoolRepository extends JpaRepository<School, Long> {
    Optional<School> findByCode(String code);
    boolean existsByName(String name);
}
```

### GradeLevelRepository.java
```java
public interface GradeLevelRepository extends JpaRepository<GradeLevel, Long> {
    List<GradeLevel> findAllByOrderByOrderAsc();
    Optional<GradeLevel> findByCode(String code);
}
```

### SchoolShiftRepository.java
```java
public interface SchoolShiftRepository extends JpaRepository<SchoolShift, Long> {
    Optional<SchoolShift> findByCode(String code);
}
```

### PeriodRepository.java
```java
public interface PeriodRepository extends JpaRepository<Period, Long> {
    List<Period> findByShiftIdOrderByOrderAsc(Long shiftId);
    List<Period> findAllByOrderByOrderAsc();
}
```

### RoomRepository.java
```java
public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByIsActiveTrue();
    List<Room> findByBuilding(String building);
    Optional<Room> findByName(String name);
}
```

---

## Task 3: DTOs

### SchoolDto.java
```java
public record SchoolDto(Long id, String name, String code, String address, String phone) {}
```

### GradeLevelDto.java
```java
public record GradeLevelDto(Long id, String name, String code, Integer order, String description) {}
```

### SchoolShiftDto.java
```java
public record SchoolShiftDto(Long id, String name, String code, Integer order) {}
```

### PeriodDto.java
```java
public record PeriodDto(Long id, String name, Integer order, Long shiftId, String shiftName) {}
```

### RoomDto.java
```java
public record RoomDto(Long id, String name, Integer capacity, String building, String equipment, Boolean isActive) {}
```

### CreateSchoolRequest.java / CreateRoomRequest.java (tương tự)

---

## Task 4: Service

### MasterDataService.java (read-only, gộp chung)
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MasterDataService {

    private final GradeLevelRepository gradeLevelRepository;
    private final SchoolShiftRepository schoolShiftRepository;
    private final PeriodRepository periodRepository;

    public List<GradeLevelDto> getGradeLevels() {
        return gradeLevelRepository.findAllByOrderByOrderAsc().stream()
            .map(gl -> new GradeLevelDto(gl.getId(), gl.getName(), gl.getCode(), gl.getOrder(), gl.getDescription()))
            .toList();
    }

    public List<SchoolShiftDto> getShifts() {
        return schoolShiftRepository.findAll().stream()
            .map(ss -> new SchoolShiftDto(ss.getId(), ss.getName(), ss.getCode(), ss.getOrder()))
            .toList();
    }

    public List<PeriodDto> getPeriods(Long shiftId) {
        List<Period> periods = shiftId != null
            ? periodRepository.findByShiftIdOrderByOrderAsc(shiftId)
            : periodRepository.findAllByOrderByOrderAsc();
        return periods.stream()
            .map(p -> new PeriodDto(p.getId(), p.getName(), p.getOrder(),
                p.getShift().getId(), p.getShift().getName()))
            .toList();
    }
}
```

---

## Task 5: Controllers

### MasterDataController.java (read-only)
```java
@RestController
@RequestMapping("/api/master-data")
@RequiredArgsConstructor
@Tag(name = "Master Data", description = "Danh mục dùng chung")
@SecurityRequirement(name = "Bearer Authentication")
public class MasterDataController {

    private final MasterDataService masterDataService;

    @GetMapping("/grade-levels")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<GradeLevelDto>>> getGradeLevels() {
        return ResponseEntity.ok(ApiResponse.success(masterDataService.getGradeLevels()));
    }

    @GetMapping("/shifts")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<SchoolShiftDto>>> getShifts() {
        return ResponseEntity.ok(ApiResponse.success(masterDataService.getShifts()));
    }

    @GetMapping("/periods")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<PeriodDto>>> getPeriods(
            @RequestParam(required = false) Long shiftId) {
        return ResponseEntity.ok(ApiResponse.success(masterDataService.getPeriods(shiftId)));
    }
}
```

### SchoolController.java (CRUD riêng)
```java
@RestController
@RequestMapping("/api/schools")
@RequiredArgsConstructor
@Tag(name = "Schools", description = "Trường học")
@SecurityRequirement(name = "Bearer Authentication")
public class SchoolController {

    // GET, POST, PUT, DELETE /api/schools
    // CRUD tiêu chuẩn, ADMIN only
}
```

### RoomController.java (CRUD riêng)
```java
// GET, POST, PUT, DELETE /api/rooms
// CRUD tiêu chuẩn, ADMIN only
```

---

## Task 6: Apply Master Data vào entities cũ

### SchoolClass.java — thêm schoolId (optional)
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "school_id",
            foreignKey = @ForeignKey(name = "fk_classes_school"))
private School school;
```

### Schedule.java — thay period int bằng periodId FK (optional trong Phase H)
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "period_id",
            foreignKey = @ForeignKey(name = "fk_sch_period"))
private Period periodRef;

// Giữ lại period int để backward compatible
// ponytail: xóa period int sau khi tất cả code dùng periodId
@Column(nullable = false)
private Integer period;
```

---

## Task 7: Seed Data

### data.sql hoặc Flyway migration V2__seed_master_data.sql
```sql
-- GradeLevels
INSERT INTO grade_levels (name, code, `order`, description) VALUES
('Khối 1', '1', 1, 'Lớp 1'),
('Khối 2', '2', 2, 'Lớp 2'),
('Khối 3', '3', 3, 'Lớp 3'),
...
('Khối 12', '12', 12, 'Lớp 12');

-- SchoolShifts
INSERT INTO school_shifts (name, code, `order`) VALUES
('Sáng', 'MORNING', 1),
('Chiều', 'AFTERNOON', 2);

-- Periods
INSERT INTO periods (name, `order`, shift_id) VALUES
('Tiết 1', 1, 1),
('Tiết 2', 2, 1),
('Tiết 3', 3, 1),
('Tiết 4', 4, 1),
('Tiết 5', 5, 1),
('Tiết 6', 6, 2),
('Tiết 7', 7, 2),
('Tiết 8', 8, 2),
('Tiết 9', 9, 2),
('Tiết 10', 10, 2);

-- Default school
INSERT INTO schools (name, code, school_name) VALUES
('FPT Schools', 'FPT', 'FPT Schools');
```

---

## Task 8: Admin-web

### MasterDataPage.tsx
```tsx
// Features:
// 1. Tab: GradeLevels | Shifts | Periods | Rooms
// 2. GradeLevels: read-only table hiển thị
// 3. Shifts: read-only table
// 4. Periods: filter by shift, read-only
// 5. Rooms: CRUD table (name, capacity, building, equipment, isActive)
```

---

## Task 9: Tests

### MasterDataIntegrationTest.java
```java
// Test: seed data -> GET /api/master-data/grade-levels -> 12 items
// Test: GET /api/master-data/shifts -> 2 items
// Test: GET /api/master-data/periods?shiftId=1 -> 5 items (MORNING)
// Test: GET /api/master-data/periods?shiftId=2 -> 5 items (AFTERNOON)
```

---

## Self-review checklist
- [ ] 5 master data entities: School, GradeLevel, SchoolShift, Period, Room
- [ ] Master data đọc được bằng API (có thể cache 24h)
- [ ] Seed data: 12 grade levels, 2 shifts, 10 periods
- [ ] `School` optional FK trên `SchoolClass`
- [ ] `Period` optional FK trên `Schedule` (giữ backward compat)
- [ ] Tests pass
