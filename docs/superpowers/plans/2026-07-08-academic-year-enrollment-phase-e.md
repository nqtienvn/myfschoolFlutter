# Phase E: Grade Book Refactor

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace monolithic `Grade` entity with `GradeBook → GradeItem → StudentScore` hierarchy. Hỗ trợ nhiều cột điểm với hệ số, tính TBM tự động, mô phỏng điểm.

**Architecture:**
```
GradeBook (Lớp 10A1 - HK2 - Môn Toán)
    ├── GradeItem "Miệng" (hệ số 1)
    │     ├── StudentScore (HS A = 8.0)
    │     ├── StudentScore (HS B = 7.5)
    │     └── ...
    ├── GradeItem "15 phút" (hệ số 2)
    ├── GradeItem "1 tiết" (hệ số 3)
    └── GradeItem "Học kỳ" (hệ số 4)
```

**Tech Stack:** Spring Boot 3.4.5, Spring Data JPA, Java 21, React 19, TypeScript, Vite. No new dependency.

## Global Constraints
- `GradeBook` unique: (`class_id`, `subject_id`, `semester_id`)
- `GradeItem` unique: (`grade_book_id`, `name`)
- `StudentScore` unique: (`grade_item_id`, `student_id`)
- Giữ lại `Grade` entity cũ cho legacy queries, đánh dấu `@Deprecated`
- Phase E phụ thuộc Phase B (TeachingAssignment) để resolve teacher

---

## File Map

### Create (12 files)
```
backend/src/main/java/vn/edu/fpt/myfschool/
├── controller/entity/GradeBook.java
├── controller/entity/GradeItem.java
├── controller/entity/StudentScore.java
├── repository/GradeBookRepository.java
├── repository/GradeItemRepository.java
├── repository/StudentScoreRepository.java
├── common/dto/GradeBookDto.java
├── common/dto/GradeItemDto.java
├── common/dto/StudentScoreDto.java
├── common/dto/UpdateStudentScoreRequest.java
├── service/GradeBookService.java
├── controller/GradeBookController.java
```

### Modify (3 files)
```
backend/src/main/java/vn/edu/fpt/myfschool/
├── controller/entity/Grade.java (thêm @Deprecated)
├── controller/GradeController.java (thêm endpoint mới)
└── service/GradeService.java (thêm method mới)
```

---

## Task 1: Entities

### GradeBook.java
```java
@Entity
@Table(name = "grade_books",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"class_id", "subject_id", "semester_id"}))
@Data
@EqualsAndHashCode(callSuper = true)
public class GradeBook extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_gb_class"))
    private SchoolClass cls;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_gb_subject"))
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_gb_semester"))
    private Semester semester;

    @Column(nullable = false)
    private Boolean isFinalized = false;

    @OneToMany(mappedBy = "gradeBook", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<GradeItem> items = new ArrayList<>();
}
```

### GradeItem.java
```java
@Entity
@Table(name = "grade_items",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"grade_book_id", "name"}))
@Data
@EqualsAndHashCode(callSuper = true)
public class GradeItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grade_book_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_gi_gradebook"))
    private GradeBook gradeBook;

    @Column(nullable = false, length = 100)
    private String name; // "Miệng", "15 phút", "1 tiết", "Học kỳ"

    @Column(nullable = false)
    private Integer weight; // 1, 2, 3, 4

    @Column(nullable = false)
    private Integer maxScore = 10;

    @Column(nullable = false)
    private Integer order = 0; // thứ tự hiển thị
}
```

### StudentScore.java
```java
@Entity
@Table(name = "student_scores",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"grade_item_id", "student_id"}))
@Data
@EqualsAndHashCode(callSuper = true)
public class StudentScore extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grade_item_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_ss_gradeitem"))
    private GradeItem gradeItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_ss_student"))
    private Student student;

    @Column(precision = 5, scale = 2)
    private BigDecimal score;

    @Column(nullable = false)
    private Boolean isGraded = false;

    private String note;

    @Column(nullable = false)
    private Boolean isCommentBased = false;

    private String comment;
}
```

---

## Task 2: Repositories

### GradeBookRepository.java
```java
public interface GradeBookRepository extends JpaRepository<GradeBook, Long> {
    List<GradeBook> findBySemesterId(Long semesterId);
    List<GradeBook> findByClsIdAndSemesterId(Long classId, Long semesterId);
    Optional<GradeBook> findByClsIdAndSubjectIdAndSemesterId(Long classId, Long subjectId, Long semesterId);
    boolean existsByClsIdAndSubjectIdAndSemesterId(Long classId, Long subjectId, Long semesterId);
}
```

### GradeItemRepository.java
```java
public interface GradeItemRepository extends JpaRepository<GradeItem, Long> {
    List<GradeItem> findByGradeBookIdOrderByOrderAsc(Long gradeBookId);
}
```

### StudentScoreRepository.java
```java
public interface StudentScoreRepository extends JpaRepository<StudentScore, Long> {
    List<StudentScore> findByGradeItemId(Long gradeItemId);
    Optional<StudentScore> findByGradeItemIdAndStudentId(Long gradeItemId, Long studentId);
    List<StudentScore> findByStudentIdAndGradeItemGradeBookSemesterId(Long studentId, Long semesterId);
}
```

---

## Task 3: DTOs

### GradeBookDto.java
```java
public record GradeBookDto(
    Long id,
    Long classId, String className,
    Long subjectId, String subjectName,
    Long semesterId, String semesterName,
    Boolean isFinalized,
    List<GradeItemDto> items
) {}
```

### GradeItemDto.java
```java
public record GradeItemDto(
    Long id,
    String name,
    Integer weight,
    Integer maxScore,
    Integer order
) {}
```

### StudentScoreDto.java
```java
public record StudentScoreDto(
    Long id,
    Long studentId, String studentName, String studentCode,
    Long gradeItemId,
    BigDecimal score,
    Boolean isGraded,
    String note,
    Boolean isCommentBased,
    String comment,
    BigDecimal average  // TBM tự tính
) {}
```

### UpdateStudentScoreRequest.java
```java
public record UpdateScoreEntry(
    Long studentId, BigDecimal score, String note,
    Boolean isGraded, Boolean isCommentBased, String comment
) {}

public record UpdateStudentScoreRequest(
    @NotNull Long gradeItemId,
    @NotNull List<UpdateScoreEntry> entries
) {}
```

---

## Task 4: Service

### GradeBookService.java
```java
@Service
@RequiredArgsConstructor
@Transactional
public class GradeBookService {

    private final GradeBookRepository gradeBookRepository;
    private final GradeItemRepository gradeItemRepository;
    private final StudentScoreRepository studentScoreRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final SubjectRepository subjectRepository;
    private final SemesterRepository semesterRepository;
    private final ClassRepository classRepository;

    @Transactional(readOnly = true)
    public GradeBookDto getByClassSubjectSemester(Long classId, Long subjectId, Long semesterId) {
        GradeBook gb = gradeBookRepository.findByClsIdAndSubjectIdAndSemesterId(classId, subjectId, semesterId)
            .orElseThrow(() -> new ResourceNotFoundException("GradeBook", "classId+subjectId+semesterId"));
        return toDto(gb);
    }

    public GradeBookDto getOrCreate(Long classId, Long subjectId, Long semesterId) {
        return gradeBookRepository.findByClsIdAndSubjectIdAndSemesterId(classId, subjectId, semesterId)
            .map(this::toDto)
            .orElseGet(() -> {
                SchoolClass cls = classRepository.findById(classId).orElseThrow(...);
                Subject subject = subjectRepository.findById(subjectId).orElseThrow(...);
                Semester semester = semesterRepository.findById(semesterId).orElseThrow(...);
                GradeBook gb = new GradeBook();
                gb.setCls(cls);
                gb.setSubject(subject);
                gb.setSemester(semester);
                gb = gradeBookRepository.save(gb);
                return toDto(gb);
            });
    }

    public GradeItemDto addItem(Long gradeBookId, String name, Integer weight, Integer order) {
        GradeBook gb = gradeBookRepository.findById(gradeBookId)
            .orElseThrow(() -> new ResourceNotFoundException("GradeBook", "id", gradeBookId));
        GradeItem item = new GradeItem();
        item.setGradeBook(gb);
        item.setName(name);
        item.setWeight(weight);
        item.setOrder(order != null ? order : 0);
        item = gradeItemRepository.save(item);
        return new GradeItemDto(item.getId(), item.getName(), item.getWeight(), item.getMaxScore(), item.getOrder());
    }

    public List<StudentScoreDto> updateScores(UpdateStudentScoreRequest request) {
        GradeItem item = gradeItemRepository.findById(request.gradeItemId())
            .orElseThrow(() -> new ResourceNotFoundException("GradeItem", "id", request.gradeItemId()));

        List<StudentScoreDto> results = new ArrayList<>();
        for (UpdateScoreEntry entry : request.entries()) {
            StudentScore score = studentScoreRepository
                .findByGradeItemIdAndStudentId(item.getId(), entry.studentId())
                .orElseGet(() -> {
                    StudentScore s = new StudentScore();
                    s.setGradeItem(item);
                    s.setStudent(studentRepository.findById(entry.studentId()).orElseThrow(...));
                    return s;
                });
            score.setScore(entry.score());
            score.setIsGraded(entry.isGraded() != null ? entry.isGraded() : (entry.score() != null));
            score.setNote(entry.note());
            score.setIsCommentBased(entry.isCommentBased() != null ? entry.isCommentBased() : false);
            score.setComment(entry.comment());
            score = studentScoreRepository.save(score);
            results.add(toDto(score));
        }
        return results;
    }

    public BigDecimal calculateAverage(Long studentId, Long gradeBookId) {
        List<GradeItem> items = gradeItemRepository.findByGradeBookIdOrderByOrderAsc(gradeBookId);
        BigDecimal weightedSum = BigDecimal.ZERO;
        int totalWeight = 0;
        for (GradeItem item : items) {
            StudentScore score = studentScoreRepository.findByGradeItemIdAndStudentId(item.getId(), studentId)
                .orElse(null);
            if (score != null && score.getScore() != null && score.getIsGraded()) {
                weightedSum = weightedSum.add(score.getScore().multiply(BigDecimal.valueOf(item.getWeight())));
                totalWeight += item.getWeight();
            }
        }
        if (totalWeight == 0) return null;
        return weightedSum.divide(BigDecimal.valueOf(totalWeight), 2, RoundingMode.HALF_UP);
    }

    public void finalize(Long gradeBookId) {
        GradeBook gb = gradeBookRepository.findById(gradeBookId)
            .orElseThrow(() -> new ResourceNotFoundException("GradeBook", "id", gradeBookId));
        gb.setIsFinalized(true);
        gradeBookRepository.save(gb);
    }

    private GradeBookDto toDto(GradeBook gb) {
        List<GradeItemDto> items = gradeItemRepository.findByGradeBookIdOrderByOrderAsc(gb.getId()).stream()
            .map(i -> new GradeItemDto(i.getId(), i.getName(), i.getWeight(), i.getMaxScore(), i.getOrder()))
            .toList();
        return new GradeBookDto(gb.getId(),
            gb.getCls().getId(), gb.getCls().getName(),
            gb.getSubject().getId(), gb.getSubject().getName(),
            gb.getSemester().getId(), gb.getSemester().getName(),
            gb.getIsFinalized(), items);
    }
}
```

---

## Task 5: Controller

### GradeBookController.java
```java
@RestController
@RequestMapping("/api/grade-books")
@RequiredArgsConstructor
@Tag(name = "Grade Books", description = "Bảng điểm theo lớp-môn-học kỳ")
@SecurityRequirement(name = "Bearer Authentication")
public class GradeBookController {

    private final GradeBookService gradeBookService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Lấy hoặc tạo GradeBook")
    public ResponseEntity<ApiResponse<GradeBookDto>> getOrCreate(
            @RequestParam Long classId, @RequestParam Long subjectId, @RequestParam Long semesterId) {
        return ResponseEntity.ok(ApiResponse.success(
            gradeBookService.getOrCreate(classId, subjectId, semesterId)));
    }

    @PostMapping("/{id}/items")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Thêm cột điểm (GradeItem)")
    public ResponseEntity<ApiResponse<GradeItemDto>> addItem(
            @PathVariable Long id, @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        Integer weight = (Integer) body.get("weight");
        Integer order = body.get("order") != null ? (Integer) body.get("order") : null;
        return ResponseEntity.ok(ApiResponse.success(
            "Thêm cột điểm thành công", gradeBookService.addItem(id, name, weight, order)));
    }

    @PutMapping("/scores")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Cập nhật điểm hàng loạt")
    public ResponseEntity<ApiResponse<List<StudentScoreDto>>> updateScores(
            @Valid @RequestBody UpdateStudentScoreRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
            "Cập nhật điểm thành công", gradeBookService.updateScores(request)));
    }

    @GetMapping("/{id}/students")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Xem điểm tất cả HS trong GradeBook")
    public ResponseEntity<ApiResponse<List<StudentScoreDto>>> getStudentScores(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
            gradeBookService.getStudentScores(id)));
    }

    @PostMapping("/{id}/finalize")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lock gradebook (không cho sửa)")
    public ResponseEntity<ApiResponse<Void>> finalize(@PathVariable Long id) {
        gradeBookService.finalize(id);
        return ResponseEntity.ok(ApiResponse.success("Đã khóa bảng điểm", null));
    }
}
```

---

## Task 6: Admin-web

### GradeBookPage.tsx
```tsx
// Features:
// 1. Select Class + Subject + Semester → load/create GradeBook
// 2. DataTable: Tên HS | Miệng | 15p | 1 tiết | HK | TBM
// 3. Inline edit cells (click to edit score)
// 4. Tab "Nhập tay" + "Upload Excel"
// 5. Nút "Lưu" → PUT /api/grade-books/scores
// 6. Nút "Khóa" → POST /api/grade-books/{id}/finalize
```

---

## Task 7: Tests

### GradeBookIntegrationTest.java
```java
// Test: getOrCreate → tạo GradeBook mới + 4 GradeItem mặc định
// Test: updateScores → update 3 HS
// Test: calculateAverage → verify weighted formula
// Test: finalize → scoresImmutable
// Test: duplicate score → upsert không duplicate
```

---
