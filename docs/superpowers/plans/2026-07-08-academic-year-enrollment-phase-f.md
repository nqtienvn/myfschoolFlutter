# Phase F: SemesterResult Auto-Generation

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Tự động sinh `SemesterResult` từ điểm (GradeBook/Grade), hạnh kiểm, chuyên cần. Loại bỏ nhập tay.

**Architecture:**
```
POST /api/semester-results/calculate
  body: { classId, semesterId }
  -> Loop Enrollment active
     -> Tính GPA từ GradeBook (hoặc Grade legacy)
     -> Tính xếp hạng trong lớp
     -> Tính học lực (Giỏi/Khá/TB/Yếu)
     -> Tính hạnh kiểm từ AttendanceSession stats
     -> Upsert SemesterResult
  -> Return { processed, updated }
```

**Tech Stack:** Spring Boot 3.4.5, Spring Data JPA, Java 21.

## Global Constraints
- Phụ thuộc Phase E (GradeBook) để tính GPA chính xác
- Phụ thuộc Phase D (AttendanceSession) để tính hạnh kiểm
- `SemesterResult` unique: (`student_id`, `semester_id`) — giữ nguyên
- Legacy Grade entity vẫn dùng được cho fallback

---

## File Map

### Create (4 files)
```
backend/src/main/java/vn/edu/fpt/myfschool/
├── common/dto/CalculateSemesterResultRequest.java
├── common/dto/CalculateSemesterResultResponse.java
├── service/SemesterResultCalculationService.java
├── controller/SemesterResultCalculationController.java
```

### Modify (2 files)
```
backend/src/main/java/vn/edu/fpt/myfschool/
├── service/SemesterResultService.java
├── service/impl/SemesterResultServiceImpl.java
```

---

## Task 1: DTOs

### CalculateSemesterResultRequest.java
```java
public record CalculateSemesterResultRequest(
    @NotNull Long classId,
    @NotNull Long semesterId
) {}
```

### CalculateSemesterResultResponse.java
```java
public record CalculateSemesterResultResponse(
    int processed,   // số HS được xử lý
    int updated,     // số HS cập nhật kết quả
    int skipped,     // HS không có enrollment active
    List<String> warnings
) {}
```

---

## Task 2: Service

### SemesterResultCalculationService.java
```java
@Service
@RequiredArgsConstructor
@Transactional
public class SemesterResultCalculationService {

    private final EnrollmentRepository enrollmentRepository;
    private final GradeRepository gradeRepository;
    private final SemesterResultRepository semesterResultRepository;
    private final AttendanceSessionRepository attendanceSessionRepository;
    private final AttendanceDetailRepository attendanceDetailRepository;
    private final GradeBookRepository gradeBookRepository; // Phase E
    private final GradeItemRepository gradeItemRepository;
    private final StudentScoreRepository studentScoreRepository;

    public CalculateSemesterResultResponse calculate(Long classId, Long semesterId) {
        Semester semester = semesterRepository.findById(semesterId)
            .orElseThrow(() -> new ResourceNotFoundException("Semester", "id", semesterId));

        List<Enrollment> enrollments = enrollmentRepository.findByClsIdAndAcademicYearIdAndStatus(
            classId, semester.getAcademicYear().getId(), EnrollmentStatus.ACTIVE);

        int processed = 0, updated = 0, skipped = 0;
        List<String> warnings = new ArrayList<>();

        for (Enrollment enrollment : enrollments) {
            Student student = enrollment.getStudent();
            
            // 1. Tính GPA
            BigDecimal gpa = calculateGPA(student, semester);
            if (gpa == null) {
                warnings.add(student.getStudentCode() + ": không có điểm");
                skipped++;
                continue;
            }

            // 2. Xếp hạng (sau khi có tất cả GPA)
            processed++;
        }

        // 2. Xếp hạng - sort by GPA desc
        List<StudentGPA> allGPAs = new ArrayList<>();
        for (Enrollment enrollment : enrollments) {
            Student student = enrollment.getStudent();
            BigDecimal gpa = calculateGPA(student, semester);
            if (gpa != null) {
                allGPAs.add(new StudentGPA(student.getId(), gpa));
            }
        }
        allGPAs.sort(Comparator.comparing(StudentGPA::gpa).reversed());

        // Gán rank
        for (int i = 0; i < allGPAs.size(); i++) {
            StudentGPA sg = allGPAs.get(i);
            int rank = i + 1;
            
            SemesterResult result = semesterResultRepository
                .findByStudentIdAndSemesterId(sg.studentId(), semesterId)
                .orElseGet(SemesterResult::new);
            
            result.setStudent(enrollment.getStudent());
            result.setSemester(semester);
            result.setCls(enrollment.getCls());
            result.setGpa(sg.gpa());
            result.setRank(rank);
            result.setHonor(calculateHonor(sg.gpa()));
            result.setAcademicAbility(calculateAcademicAbility(sg.gpa()));
            result.setConduct(calculateConduct(enrollment.getStudent(), semester));
            
            semesterResultRepository.save(result);
            updated++;
        }

        return new CalculateSemesterResultResponse(processed, updated, skipped, warnings);
    }

    private BigDecimal calculateGPA(Student student, Semester semester) {
        // Try GradeBook first (Phase E)
        if (gradeBookRepository != null) {
            List<GradeBook> books = gradeBookRepository.findBySemesterId(semester.getId());
            if (!books.isEmpty()) {
                return calculateGPAFromGradeBook(student, books);
            }
        }
        // Fallback: Grade entity legacy
        List<Grade> grades = gradeRepository.findByStudentIdAndSemesterId(student.getId(), semester.getId());
        if (grades.isEmpty()) return null;
        
        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        for (Grade g : grades) {
            if (g.getAverage() != null) {
                sum = sum.add(g.getAverage());
                count++;
            }
        }
        return count > 0 ? sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP) : null;
    }

    private BigDecimal calculateGPAFromGradeBook(Student student, List<GradeBook> gradeBooks) {
        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        for (GradeBook book : gradeBooks) {
            List<GradeItem> items = gradeItemRepository.findByGradeBookId(book.getId());
            BigDecimal weightedSum = BigDecimal.ZERO;
            int totalWeight = 0;
            for (GradeItem item : items) {
                StudentScore score = studentScoreRepository
                    .findByGradeItemIdAndStudentId(item.getId(), student.getId()).orElse(null);
                if (score != null && score.getScore() != null && score.getIsGraded()) {
                    weightedSum = weightedSum.add(score.getScore().multiply(BigDecimal.valueOf(item.getWeight())));
                    totalWeight += item.getWeight();
                }
            }
            if (totalWeight > 0) {
                sum = sum.add(weightedSum.divide(BigDecimal.valueOf(totalWeight), 2, RoundingMode.HALF_UP));
                count++;
            }
        }
        return count > 0 ? sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP) : null;
    }

    private String calculateHonor(BigDecimal gpa) {
        if (gpa.compareTo(BigDecimal.valueOf(8.0)) >= 0) return "Giỏi";
        if (gpa.compareTo(BigDecimal.valueOf(6.5)) >= 0) return "Khá";
        if (gpa.compareTo(BigDecimal.valueOf(5.0)) >= 0) return "Trung bình";
        return "Yếu";
    }

    private String calculateAcademicAbility(BigDecimal gpa) {
        return calculateHonor(gpa); // cùng logic
    }

    private String calculateConduct(Student student, Semester semester) {
        // Query attendance stats từ AttendanceSession
        List<AttendanceSession> sessions = attendanceSessionRepository
            .findByClsIdAndSemester(student.getCurrentClass().getId(), 
                semester.getStartDate(), semester.getEndDate());
        
        if (sessions.isEmpty()) return "Tốt"; // default
        
        int totalSessions = 0;
        int absentWithoutLeave = 0;
        
        for (AttendanceSession s : sessions) {
            totalSessions += s.getTotal();
            absentWithoutLeave += s.getAbsent(); // only ABSENT_WITHOUT_LEAVE ideally
        }
        
        if (totalSessions == 0) return "Tốt";
        
        double absentRate = (double) absentWithoutLeave / totalSessions * 100;
        if (absentRate < 5) return "Tốt";
        if (absentRate < 15) return "Khá";
        if (absentRate < 30) return "Trung bình";
        return "Yếu";
    }

    private record StudentGPA(Long studentId, BigDecimal gpa) {}
}
```

---

## Task 3: Controller

### SemesterResultCalculationController.java
```java
@RestController
@RequestMapping("/api/semester-results")
@RequiredArgsConstructor
@Tag(name = "Semester Results", description = "Tổng kết học kỳ")
@SecurityRequirement(name = "Bearer Authentication")
public class SemesterResultCalculationController {

    private final SemesterResultCalculationService calculationService;

    @PostMapping("/calculate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tự động tính tổng kết học kỳ")
    public ResponseEntity<ApiResponse<CalculateSemesterResultResponse>> calculate(
            @Valid @RequestBody CalculateSemesterResultRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
            "Tính tổng kết thành công", calculationService.calculate(request.classId(), request.semesterId())));
    }
}
```

---

## Task 4: Tests

### SemesterResultCalculationIntegrationTest.java
```java
// Test: setup 3 HS with grades -> call calculate
// Test: verify GPA, rank, honor correct
// Test: verify conduct from attendance
// Test: rerun -> update existing results (idempotent)
```

---

## Self-review checklist
- [ ] GPA tính từ GradeBook (Phase E) hoặc Grade legacy
- [ ] Rank sort by GPA desc, tie-break deterministic
- [ ] Honor/AcademicAbility từ GPA threshold
- [ ] Conduct từ attendance stats (absentWithoutLeave rate)
- [ ] Idempotent: chạy lại update, không duplicate
- [ ] Tests pass