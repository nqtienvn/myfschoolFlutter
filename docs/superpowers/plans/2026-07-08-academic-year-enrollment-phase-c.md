# Phase C: Fee Template & Bill Generation

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add `FeeCategory` (Học phí, Bảo hiểm, Đồng phục...), `FeeTemplate` (cấu hình số tiền, hạn đóng), và API tự động sinh `TuitionBill` hàng loạt.

**Architecture:**
```
FeeCategory (tên + mô tả)
    └── FeeTemplate (class + semester + amount + dueDate)
            └── TuitionBill (student + status) — tự sinh từ template
                    └── PaymentTransaction (payment log)
```

**Tech Stack:** Spring Boot 3.4.5, Spring Data JPA, Java 21, React 19, TypeScript, Vite. No new dependency.

## Global Constraints
- `FeeCategory` unique: (`name`)
- `FeeTemplate` unique: (`fee_category_id`, `class_id`, `semester_id`)
- `TuitionBill` unique: (`student_id`, `semester_id`, `fee_template_id`)
- Không sửa `PaymentTransaction` entity
- Phase C độc lập, không phụ thuộc Phase B

---

## File Map

### Create (13 files)
```
backend/src/main/java/vn/edu/fpt/myfschool/
├── controller/entity/FeeCategory.java
├── controller/entity/FeeTemplate.java
├── repository/FeeCategoryRepository.java
├── repository/FeeTemplateRepository.java
├── common/dto/FeeCategoryDto.java
├── common/dto/CreateFeeCategoryRequest.java
├── common/dto/FeeTemplateDto.java
├── common/dto/CreateFeeTemplateRequest.java
├── common/dto/GenerateBillResultDto.java
├── service/FeeCategoryService.java
├── service/FeeTemplateService.java
├── controller/FeeCategoryController.java
├── controller/FeeTemplateController.java
```

### Modify (5 files)
```
backend/src/main/java/vn/edu/fpt/myfschool/
├── controller/entity/TuitionBill.java
├── repository/TuitionBillRepository.java
├── common/dto/TuitionBillDto.java
├── service/impl/TuitionBillServiceImpl.java
├── controller/TuitionBillController.java
```

### Admin web (create/modify)
```
admin-web/src/pages/
├── FeeCategoriesPage.tsx (new)
├── FeeTemplatesPage.tsx (new)
├── TuitionPage.tsx (modify — show feeTemplate info)
```

---

## Task 1: FeeCategory entity + API

### FeeCategory.java
```java
@Entity
@Table(name = "fee_categories",
       uniqueConstraints = @UniqueConstraint(columnNames = "name"))
@Data
@EqualsAndHashCode(callSuper = true)
public class FeeCategory extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String name;  // "Học phí", "Bảo hiểm", "Đồng phục"

    @Column(nullable = false, length = 200)
    private String description;
}
```

### FeeCategoryRepository.java
```java
public interface FeeCategoryRepository extends JpaRepository<FeeCategory, Long> {
    boolean existsByName(String name);
    Optional<FeeCategory> findByName(String name);
}
```

### FeeCategoryDto.java
```java
public record FeeCategoryDto(Long id, String name, String description) {}
```

### CreateFeeCategoryRequest.java
```java
public record CreateFeeCategoryRequest(
    @NotBlank @Size(max = 50) String name,
    @NotBlank @Size(max = 200) String description
) {}
```

### FeeCategoryService.java
```java
@Service
@RequiredArgsConstructor
@Transactional
public class FeeCategoryService {

    private final FeeCategoryRepository feeCategoryRepository;

    @Transactional(readOnly = true)
    public List<FeeCategoryDto> list() {
        return feeCategoryRepository.findAll().stream()
            .map(c -> new FeeCategoryDto(c.getId(), c.getName(), c.getDescription()))
            .toList();
    }

    public FeeCategoryDto create(CreateFeeCategoryRequest request) {
        if (feeCategoryRepository.existsByName(request.name())) {
            throw new ConflictException("Danh mục phí đã tồn tại");
        }
        FeeCategory fc = new FeeCategory();
        fc.setName(request.name());
        fc.setDescription(request.description());
        fc = feeCategoryRepository.save(fc);
        return new FeeCategoryDto(fc.getId(), fc.getName(), fc.getDescription());
    }

    public void delete(Long id) {
        FeeCategory fc = feeCategoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("FeeCategory", "id", id));
        feeCategoryRepository.delete(fc);
    }
}
```

### FeeCategoryController.java
```java
@RestController
@RequestMapping("/api/fee-categories")
@RequiredArgsConstructor
@Tag(name = "Fee Categories", description = "Danh mục loại phí")
@SecurityRequirement(name = "Bearer Authentication")
public class FeeCategoryController {

    private final FeeCategoryService feeCategoryService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PARENT', 'STUDENT', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<FeeCategoryDto>>> list() {
        return ResponseEntity.ok(ApiResponse.success(feeCategoryService.list()));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FeeCategoryDto>> create(
            @Valid @RequestBody CreateFeeCategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Tạo danh mục thành công", feeCategoryService.create(request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        feeCategoryService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa thành công", null));
    }
}
```

- [ ] **Step 1.1:** Tạo `FeeCategory.java`
- [ ] **Step 1.2:** Tạo `FeeCategoryRepository.java`
- [ ] **Step 1.3:** Tạo DTOs
- [ ] **Step 1.4:** Tạo `FeeCategoryService.java`
- [ ] **Step 1.5:** Tạo `FeeCategoryController.java`
- [ ] **Step 1.6:** Compile check

---

## Task 2: FeeTemplate entity + API

### FeeTemplate.java
```java
@Entity
@Table(name = "fee_templates",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"fee_category_id", "class_id", "semester_id"}))
@Data
@EqualsAndHashCode(callSuper = true)
public class FeeTemplate extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fee_category_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_ft_category"))
    private FeeCategory feeCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_ft_class"))
    private SchoolClass cls;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_ft_semester"))
    private Semester semester;

    @Column(nullable = false, length = 200)
    private String name;  // "Học phí HK2 năm 2026-2027"

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate dueDate;
}
```

### FeeTemplateRepository.java
```java
public interface FeeTemplateRepository extends JpaRepository<FeeTemplate, Long> {
    List<FeeTemplate> findByClsIdAndSemesterId(Long classId, Long semesterId);
    List<FeeTemplate> findByClsId(Long classId);
    boolean existsByFeeCategoryIdAndClsIdAndSemesterId(Long categoryId, Long classId, Long semesterId);
}
```

### CreateFeeTemplateRequest.java
```java
public record CreateFeeTemplateRequest(
    @NotNull Long feeCategoryId,
    @NotNull Long classId,
    @NotNull Long semesterId,
    @NotBlank @Size(max = 200) String name,
    @NotNull @DecimalMin("0.01") BigDecimal amount,
    @NotNull LocalDate dueDate
) {}
```

### FeeTemplateDto.java
```java
public record FeeTemplateDto(
    Long id,
    Long feeCategoryId, String feeCategoryName,
    Long classId, String className,
    Long semesterId, String semesterName,
    String name, BigDecimal amount, LocalDate dueDate,
    Integer studentCount  // số HS sẽ được generate bill
) {}
```

### GenerateBillResultDto.java
```java
public record GenerateBillResultDto(
    Long feeTemplateId,
    int totalStudents,
    int created,
    int skipped  // đã có bill rồi
) {}
```

### FeeTemplateService.java
```java
@Service
@RequiredArgsConstructor
@Transactional
public class FeeTemplateService {

    private final FeeTemplateRepository feeTemplateRepository;
    private final FeeCategoryRepository feeCategoryRepository;
    private final SchoolClassRepository classRepository;
    private final SemesterRepository semesterRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final TuitionBillRepository tuitionBillRepository;

    @Transactional(readOnly = true)
    public List<FeeTemplateDto> listByClass(Long classId, Long semesterId) {
        return feeTemplateRepository.findByClsIdAndSemesterId(classId, semesterId)
            .stream().map(this::toDto).toList();
    }

    public FeeTemplateDto create(CreateFeeTemplateRequest request) {
        FeeCategory fc = feeCategoryRepository.findById(request.feeCategoryId())
            .orElseThrow(() -> new ResourceNotFoundException("FeeCategory", "id", request.feeCategoryId()));
        SchoolClass cls = classRepository.findById(request.classId())
            .orElseThrow(() -> new ResourceNotFoundException("Class", "id", request.classId()));
        Semester semester = semesterRepository.findById(request.semesterId())
            .orElseThrow(() -> new ResourceNotFoundException("Semester", "id", request.semesterId()));

        if (feeTemplateRepository.existsByFeeCategoryIdAndClsIdAndSemesterId(
                request.feeCategoryId(), request.classId(), request.semesterId())) {
            throw new ConflictException("Template đã tồn tại cho lớp và học kỳ này");
        }

        FeeTemplate ft = new FeeTemplate();
        ft.setFeeCategory(fc);
        ft.setCls(cls);
        ft.setSemester(semester);
        ft.setName(request.name());
        ft.setAmount(request.amount());
        ft.setDueDate(request.dueDate());
        ft = feeTemplateRepository.save(ft);

        // Count students
        List<Student> students = enrollmentRepository.findActiveStudentsByClassAndYear(
            cls.getId(), cls.getAcademicYear().getId());
        return new FeeTemplateDto(ft.getId(), fc.getId(), fc.getName(),
            cls.getId(), cls.getName(), semester.getId(), semester.getName(),
            ft.getName(), ft.getAmount(), ft.getDueDate(), students.size());
    }

    public GenerateBillResultDto generateBills(Long feeTemplateId) {
        FeeTemplate ft = feeTemplateRepository.findById(feeTemplateId)
            .orElseThrow(() -> new ResourceNotFoundException("FeeTemplate", "id", feeTemplateId));

        List<Student> students = enrollmentRepository.findActiveStudentsByClassAndYear(
            ft.getCls().getId(), ft.getCls().getAcademicYear().getId());

        int created = 0, skipped = 0;
        for (Student s : students) {
            if (tuitionBillRepository.existsByStudentIdAndSemesterIdAndFeeTemplateId(
                    s.getId(), ft.getSemester().getId(), ft.getId())) {
                skipped++;
                continue;
            }
            TuitionBill bill = new TuitionBill();
            bill.setStudent(s);
            bill.setCls(ft.getCls());
            bill.setSemester(ft.getSemester());
            bill.setFeeTemplate(ft);
            bill.setName(ft.getName());
            bill.setAmount(ft.getAmount());
            bill.setDueDate(ft.getDueDate());
            bill.setStatus(BillStatus.UNPAID);
            tuitionBillRepository.save(bill);
            created++;
        }

        return new GenerateBillResultDto(ft.getId(), students.size(), created, skipped);
    }

    private FeeTemplateDto toDto(FeeTemplate ft) {
        List<Student> students = enrollmentRepository.findActiveStudentsByClassAndYear(
            ft.getCls().getId(), ft.getCls().getAcademicYear().getId());
        return new FeeTemplateDto(ft.getId(),
            ft.getFeeCategory().getId(), ft.getFeeCategory().getName(),
            ft.getCls().getId(), ft.getCls().getName(),
            ft.getSemester().getId(), ft.getSemester().getName(),
            ft.getName(), ft.getAmount(), ft.getDueDate(), students.size());
    }
}
```

### FeeTemplateController.java
```java
@RestController
@RequestMapping("/api/fee-templates")
@RequiredArgsConstructor
@Tag(name = "Fee Templates", description = "Cấu hình khoản phí")
@SecurityRequirement(name = "Bearer Authentication")
public class FeeTemplateController {

    private final FeeTemplateService feeTemplateService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<FeeTemplateDto>>> list(
            @RequestParam Long classId, @RequestParam Long semesterId) {
        return ResponseEntity.ok(ApiResponse.success(feeTemplateService.listByClass(classId, semesterId)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FeeTemplateDto>> create(
            @Valid @RequestBody CreateFeeTemplateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Tạo template thành công", feeTemplateService.create(request)));
    }

    @PostMapping("/{id}/generate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Sinh học phí hàng loạt từ template")
    public ResponseEntity<ApiResponse<GenerateBillResultDto>> generate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
            "Sinh học phí thành công", feeTemplateService.generateBills(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        feeTemplateService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa thành công", null));
    }
}
```

- [ ] **Step 2.1:** Tạo `FeeTemplate.java`
- [ ] **Step 2.2:** Tạo `FeeTemplateRepository.java`
- [ ] **Step 2.3:** Tạo DTOs
- [ ] **Step 2.4:** Tạo `FeeTemplateService.java`
- [ ] **Step 2.5:** Tạo `FeeTemplateController.java`
- [ ] **Step 2.6:** Compile check

---

## Task 3: Modify TuitionBill — thêm feeTemplate FK

### TuitionBill.java — thêm:
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "fee_template_id",
            foreignKey = @ForeignKey(name = "fk_tb_fee_template"))
private FeeTemplate feeTemplate;
```

### Cập nhật unique constraint:
```java
@Table(name = "tuition_bills",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"student_id", "semester_id", "fee_template_id"}))
```

### TuitionBillRepository.java — thêm:
```java
boolean existsByStudentIdAndSemesterIdAndFeeTemplateId(Long studentId, Long semesterId, Long feeTemplateId);
List<TuitionBill> findByFeeTemplateId(Long feeTemplateId);
```

### TuitionBillDto.java — thêm:
```java
Long feeTemplateId, String feeTemplateName
```

- [ ] **Step 3.1:** Sửa `TuitionBill.java`
- [ ] **Step 3.2:** Sửa `TuitionBillRepository.java`
- [ ] **Step 3.3:** Sửa `TuitionBillDto.java`
- [ ] **Step 3.4:** Compile check

---

## Task 4: Update admin-web

### FeeCategoriesPage.tsx
```tsx
// CRUD danh mục phí: name, description
// GET/POST/DELETE /api/fee-categories
```

### FeeTemplatesPage.tsx
```tsx
// Select class + semester → list templates
// Create: chọn category, class, semester, nhập amount + dueDate
// Nút "Generate Bills" → POST /api/fee-templates/{id}/generate
// Hiển thị kết quả: total, created, skipped
```

### TuitionPage.tsx — sửa
```tsx
// Thêm cột: "Template" hiển thị feeTemplateName
// Filter bills by feeTemplateId
```

- [ ] **Step 4.1:** Tạo `FeeCategoriesPage.tsx`
- [ ] **Step 4.2:** Tạo `FeeTemplatesPage.tsx`
- [ ] **Step 4.3:** Sửa `TuitionPage.tsx`
- [ ] **Step 4.4:** Build:
  ```bash
  npm --prefix admin-web run build
  ```

---

## Task 5: Tests

### FeeTemplateIntegrationTest.java
```java
// Test: tạo category → tạo template → generate bills
// Test: generate lần 2 → skip (idempotent)
// Test: conflict khi tạo template trùng category+class+semester
```

- [ ] **Step 5.1:** Tạo test file
- [ ] **Step 5.2:** Chạy test:
  ```bash
  mvn -f backend/pom.xml test -Dtest=FeeTemplateIntegrationTest
  ```
- [ ] **Step 5.3:** All tests:
  ```bash
  mvn -f backend/pom.xml test
  ```

---

## Task 6: Final verification

- [ ] **Step 6.1:** Backend tests pass
- [ ] **Step 6.2:** Admin-web build pass
- [ ] **Step 6.3:** Manual smoke:
  1. Tạo danh mục: "Học phí"
  2. Tạo template: class=10A1, semester=HK2, amount=15tr
  3. Generate: 42 created, 0 skipped
  4. Generate lại: 0 created, 42 skipped (idempotent)
  5. GET `/api/tuition/bills/student?studentId=1` → bill có feeTemplateId

---

## Self-review checklist
- [ ] `FeeCategory` unique name
- [ ] `FeeTemplate` unique (category_id, class_id, semester_id)
- [ ] `TuitionBill` có FK đến `FeeTemplate`
- [ ] Generate bill idempotent (chạy lại không duplicate)
- [ ] Tests pass
- [ ] Admin-web build pass
