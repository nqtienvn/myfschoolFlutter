# Phase G: Bulk APIs — Initialize, Promote, Import

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Cung cấp các API bulk cho đầu năm học: khởi tạo năm học (copy từ năm cũ), promote học sinh lên lớp trên, import enrollment từ Excel, import schedule từ Excel.

**Architecture:**
```
POST /api/academic-years/{id}/initialize
  -> Copy classes từ previous year
  -> Copy teaching Assignments (template only, không copy teacher)
  -> Copy homeroom Assignments (không copy, để trống)
  -> Copy fee Templates

POST /api/enrollments/promote?fromAcademicYearId=1&toAcademicYearId=2
  -> Tìm class mapping (10A1 -> 11A1 ...)
  -> Close old enrollment (status = PROMOTED)
  -> Create new enrollment for new year

POST /api/enrollments/import
  -> Upload Excel -> validate -> batch create

POST /api/schedules/import  
  -> Upload Excel -> validate -> batch insert
```

**Tech Stack:** Spring Boot 3.4.5, Spring Data JPA, Java 21, Apache POI (Excel) hoặc OpenCSV.

## New Dependency
```xml
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.5</version>
</dependency>
```

---

## File Map

### Create (12 files)
```
backend/src/main/java/vn/edu/fpt/myfschool/
├── common/dto/InitializeAcademicYearRequest.java
├── common/dto/InitializeAcademicYearResponse.java
├── common/dto/PromoteResponse.java
├── common/dto/ImportResultDto.java
├── service/AcademicYearInitializationService.java
├── service/EnrollmentPromotionService.java
├── service/EnrollmentImportService.java
├── service/ScheduleImportService.java
├── controller/AcademicYearInitializationController.java
├── controller/EnrollmentBulkController.java
├── controller/ImportController.java
├── common/util/ExcelReader.java (utility)
```

### Modify (1 file)
```
backend/src/main/java/vn/edu/fpt/myfschool/
├── common/enums/EnrollmentStatus.java (thêm PROMOTED)
```

### Admin web
```
admin-web/src/pages/
├── AcademicYearInitPage.tsx (new)
├── EnrollmentImportPage.tsx (new)
├── ScheduleImportPage.tsx (new)
```

---

## Task 1: Initialize Academic Year

### InitializeAcademicYearRequest.java
```java
public record InitializeAcademicYearRequest(
    @NotNull Long fromAcademicYearId  // copy từ năm này
) {}
```

### InitializeAcademicYearResponse.java
```java
public record InitializeAcademicYearResponse(
    Long newAcademicYearId,
    int classesCreated,
    int teachingAssignmentsCreated,
    int feeTemplatesCopied,
    List<String> warnings
) {}
```

### AcademicYearInitializationService.java
```java
@Service
@RequiredArgsConstructor
@Transactional
public class AcademicYearInitializationService {

    private final AcademicYearRepository academicYearRepository;
    private final ClassRepository classRepository;
    private final TeachingAssignmentRepository teachingAssignmentRepository;
    private final FeeTemplateRepository feeTemplateRepository;
    private final HomeroomAssignmentRepository homeroomAssignmentRepository;

    public InitializeAcademicYearResponse initialize(Long newAcademicYearId, Long fromAcademicYearId) {
        AcademicYear newYear = academicYearRepository.findById(newAcademicYearId)
            .orElseThrow(() -> new ResourceNotFoundException("AcademicYear", "id", newAcademicYearId));
        AcademicYear fromYear = academicYearRepository.findById(fromAcademicYearId)
            .orElseThrow(() -> new ResourceNotFoundException("AcademicYear", "id", fromAcademicYearId));

        List<String> warnings = new ArrayList<>();

        // 1. Copy classes
        List<SchoolClass> fromClasses = classRepository.findByAcademicYearId(fromYear.getId());
        int classesCreated = 0;
        Map<String, SchoolClass> classMap = new HashMap<>(); // old name -> new class
        for (SchoolClass old : fromClasses) {
            // Check if class already exists in new year
            if (classRepository.existsByNameAndAcademicYearId(old.getName(), newYear.getId())) {
                warnings.add("Lớp " + old.getName() + " đã tồn tại, bỏ qua");
                continue;
            }
            SchoolClass newCls = new SchoolClass();
            newCls.setName(old.getName());
            newCls.setGradeLevel(old.getGradeLevel());
            newCls.setAcademicYear(newYear);
            newCls.setSchoolName(old.getSchoolName());
            classRepository.save(newCls);
            classMap.put(old.getName(), newCls);
            classesCreated++;
        }

        // 2. Copy teaching assignments (template only - không copy teacher cụ thể)
        int assignmentsCreated = 0;
        List<TeachingAssignment> fromAssignments = teachingAssignmentRepository
            .findByAcademicYearId(fromYear.getId());
        for (TeachingAssignment old : fromAssignments) {
            SchoolClass newCls = classMap.get(old.getCls().getName());
            if (newCls == null) continue;
            
            TeachingAssignment ta = new TeachingAssignment();
            ta.setCls(newCls);
            ta.setSubject(old.getSubject());
            ta.setTeacher(null); // để trống, admin gán sau
            ta.setSemester(old.getSemester()); // HK giống tên, khác năm -> cần mapping
            ta.setEffectiveFrom(newYear.getStartDate());
            ta.setStatus(AssignmentStatus.INACTIVE); // chưa active
            teachingAssignmentRepository.save(ta);
            assignmentsCreated++;
        }

        // 3. Copy fee templates
        int templatesCopied = 0;
        List<FeeTemplate> fromTemplates = feeTemplateRepository
            .findByAcademicYearId(fromYear.getId());
        for (FeeTemplate old : fromTemplates) {
            SchoolClass newCls = classMap.get(old.getCls().getName());
            if (newCls == null) continue;
            
            FeeTemplate ft = new FeeTemplate();
            ft.setFeeCategory(old.getFeeCategory());
            ft.setCls(newCls);
            ft.setSemester(old.getSemester());
            ft.setName(old.getName());
            ft.setAmount(old.getAmount());
            ft.setDueDate(old.getDueDate());
            feeTemplateRepository.save(ft);
            templatesCopied++;
        }

        return new InitializeAcademicYearResponse(
            newYear.getId(), classesCreated, assignmentsCreated, templatesCopied, warnings);
    }
}
```

### AcademicYearInitializationController.java
```java
@RestController
@RequestMapping("/api/academic-years")
@RequiredArgsConstructor
@Tag(name = "Academic Year Init", description = "Khởi tạo năm học")
@SecurityRequirement(name = "Bearer Authentication")
public class AcademicYearInitializationController {

    private final AcademicYearInitializationService initializationService;

    @PostMapping("/{id}/initialize")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Khởi tạo năm học từ năm trước")
    public ResponseEntity<ApiResponse<InitializeAcademicYearResponse>> initialize(
            @PathVariable Long id, @Valid @RequestBody InitializeAcademicYearRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
            "Khởi tạo năm học thành công", initializationService.initialize(id, request.fromAcademicYearId())));
    }
}
```

---

## Task 2: Promote Students

### EnrollmentStatus.java — thêm:
```java
public enum EnrollmentStatus {
    ACTIVE,
    LEFT,
    TRANSFERRED,
    PROMOTED  // thêm
}
```

### PromotionResponse.java
```java
public record PromotionResponse(
    int promoted,
    int skipped,
    List<String> warnings
) {}
```

### EnrollmentPromotionService.java
```java
@Service
@RequiredArgsConstructor
@Transactional
public class EnrollmentPromotionService {

    private final EnrollmentRepository enrollmentRepository;
    private final ClassRepository classRepository;
    private final AcademicYearRepository academicYearRepository;

    public PromotionResponse promoteAll(Long fromAcademicYearId, Long toAcademicYearId) {
        AcademicYear fromYear = academicYearRepository.findById(fromAcademicYearId).orElseThrow(...);
        AcademicYear toYear = academicYearRepository.findById(toAcademicYearId).orElseThrow(...);

        // Class mapping: gradeLevel + 1, same name prefix pattern
        // 10A1 -> 11A1, 10A2 -> 11A2...
        Map<String, String> classMapping = buildClassMapping(fromYear, toYear);

        List<Enrollment> activeEnrollments = enrollmentRepository
            .findByAcademicYearIdAndStatus(fromYear.getId(), EnrollmentStatus.ACTIVE);

        int promoted = 0, skipped = 0;
        List<String> warnings = new ArrayList<>();

        for (Enrollment e : activeEnrollments) {
            // Find target class
            String oldClassName = e.getCls().getName();
            String newClassName = classMapping.get(oldClassName);
            if (newClassName == null) {
                warnings.add(e.getStudent().getStudentCode() + ": không tìm thấy lớp " + oldClassName + " ở năm mới");
                skipped++;
                continue;
            }

            SchoolClass newClass = classRepository
                .findByNameAndAcademicYearId(newClassName, toYear.getId())
                .orElse(null);
            if (newClass == null) {
                warnings.add("Lớp " + newClassName + " chưa được tạo");
                skipped++;
                continue;
            }

            // Check duplicate enrollment for new year
            if (enrollmentRepository.findByStudentIdAndAcademicYearIdAndStatus(
                    e.getStudent().getId(), toYear.getId(), EnrollmentStatus.ACTIVE).isPresent()) {
                skipped++;
                continue;
            }

            // Close old enrollment
            e.setLeaveDate(fromYear.getEndDate());
            e.setStatus(EnrollmentStatus.PROMOTED);
            enrollmentRepository.save(e);

            // Create new enrollment
            Enrollment newEnrollment = new Enrollment();
            newEnrollment.setStudent(e.getStudent());
            newEnrollment.setCls(newClass);
            newEnrollment.setAcademicYear(toYear);
            newEnrollment.setJoinDate(toYear.getStartDate());
            newEnrollment.setStatus(EnrollmentStatus.ACTIVE);
            enrollmentRepository.save(newEnrollment);
            
            promoted++;
        }

        return new PromotionResponse(promoted, skipped, warnings);
    }

    private Map<String, String> buildClassMapping(AcademicYear fromYear, AcademicYear toYear) {
        List<SchoolClass> fromClasses = classRepository.findByAcademicYearId(fromYear.getId());
        List<SchoolClass> toClasses = classRepository.findByAcademicYearId(toYear.getId());
        
        Map<String, String> mapping = new HashMap<>();
        for (SchoolClass from : fromClasses) {
            // Find target by gradeLevel + 1
            SchoolClass target = toClasses.stream()
                .filter(tc -> tc.getGradeLevel() == from.getGradeLevel() + 1)
                .filter(tc -> tc.getName().contains(from.getName().replaceAll("\\d+", ""))) // match prefix
                .findFirst().orElse(null);
            if (target != null) {
                mapping.put(from.getName(), target.getName());
            }
        }
        return mapping;
    }
}
```

### EnrollmentBulkController.java
```java
@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
@Tag(name = "Enrollment Bulk", description = "Promote & Import học sinh")
@SecurityRequirement(name = "Bearer Authentication")
public class EnrollmentBulkController {

    private final EnrollmentPromotionService promotionService;

    @PostMapping("/promote")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Promote học sinh lên lớp trên")
    public ResponseEntity<ApiResponse<PromotionResponse>> promote(
            @RequestParam Long fromAcademicYearId, @RequestParam Long toAcademicYearId) {
        return ResponseEntity.ok(ApiResponse.success(
            "Promote thành công", promotionService.promoteAll(fromAcademicYearId, toAcademicYearId)));
    }
}
```

---

## Task 3: Import Enrollment từ Excel

### ExcelReader.java
```java
@Component
public class ExcelReader {
    public List<Map<String, String>> read(InputStream is) {
        List<Map<String, String>> rows = new ArrayList<>();
        Workbook workbook = new XSSFWorkbook(is);
        Sheet sheet = workbook.getSheetAt(0);
        Row headerRow = sheet.getRow(0);
        List<String> headers = new ArrayList<>();
        for (Cell cell : headerRow) headers.add(cell.getStringCellValue());
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            Map<String, String> rowMap = new HashMap<>();
            for (int j = 0; j < headers.size(); j++) {
                Cell cell = row.getCell(j);
                rowMap.put(headers.get(j), cell != null ? cell.toString() : "");
            }
            rows.add(rowMap);
        }
        workbook.close();
        return rows;
    }
}
```

### ImportResultDto.java
```java
public record ImportResultDto(
    int total,
    int success,
    int failed,
    List<String> errors
) {}
```

### EnrollmentImportService.java (trong EnrollmentBulkController hoặc riêng)
```java
@Service
@RequiredArgsConstructor
@Transactional
public class EnrollmentImportService {

    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final ClassRepository classRepository;
    private final AcademicYearRepository academicYearRepository;
    private final ExcelReader excelReader;

    public ImportResultDto importFromExcel(MultipartFile file, Long academicYearId) {
        AcademicYear year = academicYearRepository.findById(academicYearId).orElseThrow(...);
        List<Map<String, String>> rows = excelReader.read(file.getInputStream());
        
        int success = 0, failed = 0;
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> row = rows.get(i);
            int rowNum = i + 2; // header row + 1-indexed
            try {
                String studentCode = row.get("studentCode");
                String className = row.get("className");
                
                Student student = studentRepository.findByStudentCode(studentCode)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy học sinh " + studentCode));
                SchoolClass cls = classRepository.findByNameAndAcademicYearId(className, year.getId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp " + className));

                if (enrollmentRepository.findByStudentIdAndAcademicYearIdAndStatus(
                        student.getId(), year.getId(), EnrollmentStatus.ACTIVE).isPresent()) {
                    throw new RuntimeException("Học sinh đã có enrollment active");
                }

                Enrollment enrollment = new Enrollment();
                enrollment.setStudent(student);
                enrollment.setCls(cls);
                enrollment.setAcademicYear(year);
                enrollment.setJoinDate(row.containsKey("joinDate") 
                    ? LocalDate.parse(row.get("joinDate")) : year.getStartDate());
                enrollment.setStatus(EnrollmentStatus.ACTIVE);
                enrollmentRepository.save(enrollment);
                success++;
            } catch (Exception e) {
                failed++;
                errors.add("Dòng " + rowNum + ": " + e.getMessage());
            }
        }

        return new ImportResultDto(rows.size(), success, failed, errors);
    }
}
```

---

## Task 4: Import Schedule từ Excel

### ScheduleImportService.java
```java
@Service
@RequiredArgsConstructor
@Transactional
public class ScheduleImportService {

    private final ScheduleRepository scheduleRepository;
    private final TeachingAssignmentRepository teachingAssignmentRepository;
    private final SemesterRepository semesterRepository;
    private final ExcelReader excelReader;

    public ImportResultDto importFromExcel(MultipartFile file, Long semesterId) {
        Semester semester = semesterRepository.findById(semesterId).orElseThrow(...);
        List<Map<String, String>> rows = excelReader.read(file.getInputStream());
        
        int success = 0, failed = 0;
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> row = rows.get(i);
            int rowNum = i + 2;
            try {
                Long assignmentId = Long.parseLong(row.get("assignmentId"));
                Integer dayOfWeek = Integer.parseInt(row.get("dayOfWeek"));
                Integer period = Integer.parseInt(row.get("period"));
                String room = row.getOrDefault("room", "");
                Shift shift = "AFTERNOON".equalsIgnoreCase(row.get("shift")) 
                    ? Shift.AFTERNOON : Shift.MORNING;

                TeachingAssignment ta = teachingAssignmentRepository.findById(assignmentId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy assignment " + assignmentId));

                // Check conflict
                if (scheduleRepository.findByAssignmentIdAndDayOfWeekAndPeriod(
                        assignmentId, dayOfWeek, period).isPresent()) {
                    throw new RuntimeException("Xung đột lịch: assignment " + assignmentId + " đã có tiết " + period + " thứ " + dayOfWeek);
                }

                Schedule schedule = new Schedule();
                schedule.setAssignment(ta);
                schedule.setDayOfWeek(dayOfWeek);
                schedule.setPeriod(period);
                schedule.setRoom(room);
                schedule.setShift(shift);
                scheduleRepository.save(schedule);
                success++;
            } catch (Exception e) {
                failed++;
                errors.add("Dòng " + rowNum + ": " + e.getMessage());
            }
        }

        return new ImportResultDto(rows.size(), success, failed, errors);
    }
}
```

### ImportController.java
```java
@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
@Tag(name = "Import", description = "Import Excel hàng loạt")
@SecurityRequirement(name = "Bearer Authentication")
public class ImportController {

    private final EnrollmentImportService enrollmentImportService;
    private final ScheduleImportService scheduleImportService;

    @PostMapping("/enrollments")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ImportResultDto>> importEnrollments(
            @RequestParam("file") MultipartFile file, @RequestParam Long academicYearId) {
        return ResponseEntity.ok(ApiResponse.success(
            "Import enrollment thành công", enrollmentImportService.importFromExcel(file, academicYearId)));
    }

    @PostMapping("/schedules")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ImportResultDto>> importSchedules(
            @RequestParam("file") MultipartFile file, @RequestParam Long semesterId) {
        return ResponseEntity.ok(ApiResponse.success(
            "Import TKB thành công", scheduleImportService.importFromExcel(file, semesterId)));
    }
}
```

---

## Task 5: Admin-web

### AcademicYearInitPage.tsx
```tsx
// Features:
// 1. Select new academic year
// 2. Select from academic year (để copy)
// 3. Nút "Khởi tạo" -> POST /api/academic-years/{id}/initialize
// 4. Hiển thị kết quả: classes, assignments, feeTemplates count + warnings
```

### EnrollmentImportPage.tsx
```tsx
// Features:
// 1. Upload Excel file (studentCode, className, joinDate)
// 2. Preview rows
// 3. Nút "Import" -> POST /api/import/enrollments
// 4. Hiển thị result: success, failed, errors list
```

### ScheduleImportPage.tsx
```tsx
// Features:
// 1. Upload Excel file (assignmentId, dayOfWeek, period, room, shift)
// 2. Preview rows
// 3. Nút "Import" -> POST /api/import/schedules
// 4. Hiển thị result + errors
```

---

## Task 6: Tests

### AcademicYearInitializationIntegrationTest.java
```java
// Test: tạo 2 academic years -> init với classes + assignments -> verify copy
// Test: init lần 2 -> skip existing classes
```

### EnrollmentPromotionIntegrationTest.java
```java
// Test: promote 3 HS từ 10A1 -> 11A1
// Test: old enrollment status = PROMOTED
// Test: new enrollment status = ACTIVE
```

### ImportIntegrationTest.java
```java
// Test: upload Excel -> import enrollments -> verify DB
// Test: upload lỗi (student not found) -> errors list
```

---

## Self-review checklist
- [ ] Init: copy classes, teaching assignments (teacher=null), fee templates
- [ ] Init: skip if class already exists in new year
- [ ] Promote: close old enrollment (PROMOTED), create new
- [ ] Promote: class mapping by gradeLevel + 1
- [ ] Import enrollment: validate student + class existence
- [ ] Import schedule: check conflict
- [ ] Idempotent: chạy lại không duplicate
- [ ] Excel: Apache POI dependency added
- [ ] Tests pass
