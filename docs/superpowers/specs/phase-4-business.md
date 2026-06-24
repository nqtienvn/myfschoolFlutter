# Phase 4 — Business (Tuition + Clubs + Attachments)

> Tables: `tuition_bills`, `payment_transactions`, `club_registrations`, `attachments`
> Tổng: **4 tables**, **~25 API endpoints**

---

## 4A. Tuition Bills

### 4A.1. Entity

```java
@Entity
@Table(name = "tuition_bills")
public class TuitionBill extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_tb_student"))
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_tb_class"))
    private Class cls;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_tb_semester"))
    private Semester semester;

    @Column(nullable = false, length = 200)
    private String name;  // VD: Học phí HK2, Phí cơ sở vật chất

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BillStatus status = BillStatus.UNPAID;

    private LocalDateTime paidAt;

    @OneToMany(mappedBy = "tuitionBill", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PaymentTransaction> paymentTransactions = new ArrayList<>();

    @OneToMany(mappedBy = "tuitionBill", fetch = FetchType.LAZY)
    private List<Notification> notifications = new ArrayList<>();
}
```

### 4A.2. DTOs

```java
// --- Create tuition bill ---
public record CreateTuitionBillRequest(
    @NotNull Long studentId,
    @NotNull Long classId,
    @NotNull Long semesterId,
    @NotBlank @Size(max = 200) String name,
    @NotNull @DecimalMin("0") BigDecimal amount,
    @NotNull LocalDate dueDate
) {}

// --- Batch create for class ---
public record BatchCreateTuitionBillRequest(
    @NotNull Long classId,
    @NotNull Long semesterId,
    @NotBlank @Size(max = 200) String name,
    @NotNull @DecimalMin("0") BigDecimal amount,
    @NotNull LocalDate dueDate,
    List<Long> studentIds   // null = all students in class
) {}

// --- Tuition bill ---
public record TuitionBillDto(
    Long id,
    Long studentId,
    String studentName,
    String studentCode,
    Long classId,
    String className,
    Long semesterId,
    String semesterName,
    String name,
    BigDecimal amount,
    LocalDate dueDate,
    BillStatus status,
    LocalDateTime paidAt,
    List<PaymentTransactionDto> transactions,
    LocalDateTime createdAt
) {}

// --- Tuition summary for a class ---
public record TuitionClassSummaryDto(
    Long classId,
    String className,
    Long semesterId,
    String semesterName,
    int totalStudents,
    int paidCount,
    int unpaidCount,
    int processingCount,
    BigDecimal totalAmount,
    BigDecimal paidAmount,
    BigDecimal unpaidAmount
) {}

// --- Tuition summary for a student ---
public record TuitionStudentSummaryDto(
    Long studentId,
    String studentName,
    String studentCode,
    List<TuitionBillDto> bills,
    BigDecimal totalUnpaid,
    BigDecimal totalPaid
) {}
```

### 4A.3. Repository

```java
@Repository
public interface TuitionBillRepository extends JpaRepository<TuitionBill, Long> {

    // PH xem học phí con
    List<TuitionBill> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    // PH xem bills theo trạng thái
    List<TuitionBill> findByStudentIdAndStatusOrderByDueDateAsc(
        Long studentId, BillStatus status);

    // GV xem bills theo lớp + học kỳ
    List<TuitionBill> findByClassIdAndSemesterIdOrderByCreatedAtDesc(
        Long classId, Long semesterId);

    // GV xem bills theo lớp + học kỳ + trạng thái
    List<TuitionBill> findByClassIdAndSemesterIdAndStatusOrderByCreatedAtDesc(
        Long classId, Long semesterId, BillStatus status);

    // Stats: count by status for class+semester
    @Query("SELECT tb.status, COUNT(tb), SUM(tb.amount) FROM TuitionBill tb " +
           "WHERE tb.cls.id = :classId AND tb.semester.id = :semesterId " +
           "GROUP BY tb.status")
    List<Object[]> countByStatusForClassSemester(@Param("classId") Long classId,
                                                  @Param("semesterId") Long semesterId);

    // Check if bill already exists for student+semester+name
    boolean existsByStudentIdAndSemesterIdAndName(Long studentId, Long semesterId, String name);

    // Find unpaid bills due soon (for notification)
    @Query("SELECT tb FROM TuitionBill tb WHERE tb.status = 'UNPAID' AND tb.dueDate <= :date")
    List<TuitionBill> findUnpaidDueSoon(@Param("date") LocalDate date);

    // Total unpaid for student
    @Query("SELECT COALESCE(SUM(tb.amount), 0) FROM TuitionBill tb " +
           "WHERE tb.student.id = :studentId AND tb.status = 'UNPAID'")
    BigDecimal totalUnpaidByStudent(@Param("studentId") Long studentId);
}
```

### 4A.4. Service

```java
@Service
@Transactional
public class TuitionBillService {

    private final TuitionBillRepository tuitionBillRepository;
    private final StudentRepository studentRepository;
    private final ClassRepository classRepository;
    private final SemesterRepository semesterRepository;
    private final ParentRepository parentRepository;
    private final NotificationService notificationService;

    // ============================
    // TEACHER FUNCTIONS
    // ============================

    // --- Create tuition bill for a student ---
    public TuitionBillDto createTuitionBill(CreateTuitionBillRequest request) {
        // 1. Validate student, class, semester exist
        // 2. Check unique (student, semester, name)
        // 3. Create TuitionBill with status = UNPAID
        // 4. Create notification for student's parents
        // 5. Return TuitionBillDto
    }

    // --- Batch create tuition bills for class ---
    public List<TuitionBillDto> batchCreateTuitionBills(BatchCreateTuitionBillRequest request) {
        // 1. If studentIds null → load all students in class
        // 2. For each student: create bill (skip if already exists)
        // 3. Notify all affected parents
        // 4. Return list of created bills
    }

    // --- Get class tuition summary ---
    public TuitionClassSummaryDto getClassSummary(Long classId, Long semesterId) {
        // 1. Load all bills for class+semester
        // 2. Compute counts + amounts by status
        // 3. Return TuitionClassSummaryDto
    }

    // --- Get all bills for class+semester (with filters) ---
    public List<TuitionBillDto> getClassBills(Long classId, Long semesterId,
                                               BillStatus status) {
        // 1. Load bills (optionally filtered by status)
        // 2. Map to TuitionBillDto
        // 3. Return list
    }

    // --- Update tuition bill ---
    public TuitionBillDto updateTuitionBill(Long billId, CreateTuitionBillRequest request) {
        // 1. Find bill
        // 2. Only allow update if status == UNPAID
        // 3. Update fields
        // 4. Return updated bill
    }

    // --- Delete tuition bill ---
    public void deleteTuitionBill(Long billId) {
        // 1. Find bill
        // 2. Only allow delete if status == UNPAID
        // 3. Delete
    }

    // ============================
    // PARENT FUNCTIONS
    // ============================

    // --- Get my children's tuition bills ---
    public TuitionStudentSummaryDto getStudentBills(Long studentId, Long parentUserId) {
        // 1. Verify parent is guardian of student
        // 2. Load all bills
        // 3. Compute total paid/unpaid
        // 4. Return TuitionStudentSummaryDto
    }

    // --- Initiate payment ---
    public TuitionBillDto initiatePayment(Long billId, Long parentUserId) {
        // 1. Verify parent is guardian
        // 2. Verify bill status == UNPAID
        // 3. Create PaymentTransaction with status = PENDING
        // 4. Update bill status = PROCESSING
        // 5. Return updated bill
    }

    // --- Get payment history ---
    public List<TuitionBillDto> getPaymentHistory(Long studentId, Long parentUserId) {
        // 1. Verify parent is guardian
        // 2. Load all PAID bills with transactions
        // 3. Return list
    }
}
```

**Business Rules:**
- 1 bill per student per semester per name (unique)
- Bill status: UNPAID → PROCESSING → PAID
- Only UNPAID bills can be updated/deleted
- Khi tạo bill → notification cho phụ huynh
- Batch create: skip duplicate (student+semester+name)
- Payment flow: parent initiate → create transaction PENDING → (mock) confirm → bill PAID

---

## 4B. Payment Transactions

### 4B.1. Entity

```java
@Entity
@Table(name = "payment_transactions")
public class PaymentTransaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_pt_bill"))
    private TuitionBill tuitionBill;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(length = 50)
    private String paymentMethod;  // VNPAY, QR, CASH

    @Column(length = 100)
    private String transactionRef;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.PENDING;

    private LocalDateTime paidAt;
}
```

### 4B.2. DTOs

```java
// --- Payment transaction ---
public record PaymentTransactionDto(
    Long id,
    Long billId,
    BigDecimal amount,
    String paymentMethod,
    String transactionRef,
    PaymentStatus status,
    LocalDateTime paidAt,
    LocalDateTime createdAt
) {}

// --- Confirm payment (mock) ---
public record ConfirmPaymentRequest(
    @NotNull Long transactionId,
    @NotBlank String paymentMethod,   // QR, VNPAY, CASH
    String transactionRef
) {}
```

### 4B.3. Repository

```java
@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    List<PaymentTransaction> findByTuitionBillIdOrderByCreatedAtDesc(Long billId);

    List<PaymentTransaction> findByTuitionBillIdAndStatus(
        Long billId, PaymentStatus status);

    // Total paid for a bill
    @Query("SELECT COALESCE(SUM(pt.amount), 0) FROM PaymentTransaction pt " +
           "WHERE pt.tuitionBill.id = :billId AND pt.status = 'SUCCESS'")
    BigDecimal totalPaidByBill(@Param("billId") Long billId);
}
```

### 4B.4. Service

```java
@Service
@Transactional
public class PaymentTransactionService {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final TuitionBillRepository tuitionBillRepository;
    private final NotificationService notificationService;

    // --- Confirm payment (mock) ---
    // Teacher/admin manually confirms payment (or webhook from payment gateway)
    public PaymentTransactionDto confirmPayment(ConfirmPaymentRequest request) {
        // 1. Find transaction
        // 2. Verify status == PENDING
        // 3. Update: status = SUCCESS, paymentMethod, transactionRef, paidAt = now
        // 4. Check if bill fully paid:
        //    totalPaid >= bill.amount → update bill.status = PAID, bill.paidAt = now
        // 5. Create notification for parent
        // 6. Return updated transaction
    }

    // --- Get transactions for a bill ---
    public List<PaymentTransactionDto> getTransactions(Long billId) {
        return paymentTransactionRepository.findByTuitionBillIdOrderByCreatedAtDesc(billId)
            .stream()
            .map(this::toDto)
            .toList();
    }

    // --- Simulate payment (for dev/demo) ---
    // Automatically confirms payment (no manual step)
    public PaymentTransactionDto simulatePayment(Long billId) {
        // 1. Create transaction with status = SUCCESS
        // 2. Update bill status = PAID
        // 3. Return transaction
    }
}
```

---

## 4C. Club Registrations

### 4C.1. Entity

```java
@Entity
@Table(name = "club_registrations",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"student_id", "club_name", "academic_year"}))
public class ClubRegistration extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_cr_student"))
    private Student student;

    @Column(nullable = false, length = 200)
    private String clubName;

    @Column(nullable = false, length = 9)
    private String academicYear;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClubStatus status = ClubStatus.REGISTERED;

    private LocalDateTime registeredAt;
}
```

### 4C.2. DTOs

```java
// --- Register for club ---
public record RegisterClubRequest(
    @NotBlank @Size(max = 200) String clubName,
    @NotBlank @Size(max = 9) String academicYear
) {}

// --- Club registration ---
public record ClubRegistrationDto(
    Long id,
    Long studentId,
    String studentName,
    String studentCode,
    String clubName,
    String academicYear,
    ClubStatus status,
    LocalDateTime registeredAt
) {}
```

### 4C.3. Repository

```java
@Repository
public interface ClubRegistrationRepository extends JpaRepository<ClubRegistration, Long> {

    // Student's registrations
    List<ClubRegistration> findByStudentIdAndAcademicYearOrderByRegisteredAtDesc(
        Long studentId, String academicYear);

    // Check unique
    boolean existsByStudentIdAndClubNameAndAcademicYear(
        Long studentId, String clubName, String academicYear);

    // All registrations for a club
    @Query("SELECT cr FROM ClubRegistration cr WHERE cr.clubName = :clubName " +
           "AND cr.academicYear = :year AND cr.status = 'REGISTERED'")
    List<ClubRegistration> findActiveByClub(@Param("clubName") String clubName,
                                             @Param("year") String year);

    // Distinct club names for a student
    @Query("SELECT DISTINCT cr.clubName FROM ClubRegistration cr " +
           "WHERE cr.student.id = :studentId AND cr.academicYear = :year " +
           "AND cr.status = 'REGISTERED'")
    List<String> findActiveClubNames(@Param("studentId") Long studentId,
                                      @Param("year") String year);
}
```

### 4C.4. Service

```java
@Service
@Transactional
public class ClubRegistrationService {

    private final ClubRegistrationRepository clubRegistrationRepository;
    private final StudentRepository studentRepository;

    // --- Register for club ---
    public ClubRegistrationDto register(RegisterClubRequest request, Long studentUserId) {
        // 1. Find student by userId
        // 2. Check unique (student, club, year)
        // 3. Create ClubRegistration with status = REGISTERED
        // 4. Return ClubRegistrationDto
    }

    // --- Cancel registration ---
    public void cancel(Long registrationId, Long studentUserId) {
        // 1. Find registration
        // 2. Verify student owns it
        // 3. Set status = CANCELLED
    }

    // --- Get my registrations ---
    public List<ClubRegistrationDto> getMyRegistrations(Long studentUserId, String academicYear) {
        // 1. Find student by userId
        // 2. Load registrations for year
        // 3. Return list
    }

    // --- Get club members (teacher view) ---
    public List<ClubRegistrationDto> getClubMembers(String clubName, String academicYear) {
        // 1. Load all active registrations for club
        // 2. Return list
    }
}
```

---

## 4D. Attachments

### 4D.1. Entity

```java
@Entity
@Table(name = "attachments")
public class Attachment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leave_request_id",
                foreignKey = @ForeignKey(name = "fk_att_lr"))
    private LeaveRequest leaveRequest;  // optional

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id",
                foreignKey = @ForeignKey(name = "fk_att_msg"))
    private Message message;  // optional

    @Column(nullable = false, length = 500)
    private String fileUrl;

    @Column(nullable = false, length = 255)
    private String fileName;

    @Column(nullable = false)
    private Integer fileSize;  // bytes

    @Column(nullable = false, length = 100)
    private String mimeType;
}
```

### 4D.2. DTOs

```java
// --- Attachment ---
public record AttachmentDto(
    Long id,
    String fileUrl,
    String fileName,
    Long fileSize,
    String mimeType
) {}

// --- Upload metadata (file upload skipped in phase 4, metadata only) ---
public record CreateAttachmentRequest(
    String fileUrl,
    String fileName,
    Long fileSize,
    String mimeType,
    Long leaveRequestId,   // optional
    Long messageId         // optional
) {}
```

### 4D.3. Service

```java
@Service
@Transactional
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;

    // --- Create attachment metadata ---
    // Phase 4: no actual file upload, just save metadata
    public AttachmentDto createAttachment(CreateAttachmentRequest request) {
        // 1. Validate: either leaveRequestId or messageId is set (not both null)
        // 2. Create Attachment
        // 3. Return AttachmentDto
    }

    // --- Get attachments for leave request ---
    public List<AttachmentDto> getAttachmentsByLeaveRequest(Long leaveRequestId) {
        return attachmentRepository.findByLeaveRequestId(leaveRequestId)
            .stream().map(this::toDto).toList();
    }

    // --- Get attachments for message ---
    public List<AttachmentDto> getAttachmentsByMessage(Long messageId) {
        return attachmentRepository.findByMessageId(messageId)
            .stream().map(this::toDto).toList();
    }

    // --- Delete attachment ---
    public void deleteAttachment(Long attachmentId, Long userId) {
        // 1. Find attachment
        // 2. Verify ownership (via leave request parent, or message sender)
        // 3. Delete
    }
}
```

### 4D.4. Repository

```java
@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    List<Attachment> findByLeaveRequestId(Long leaveRequestId);

    List<Attachment> findByMessageId(Long messageId);
}
```

---

## 4E. Controllers

```java
@RestController
@RequestMapping("/api/tuition")
@Tag(name = "Tuition", description = "Học phí & Thanh toán")
@SecurityRequirement(name = "Bearer Authentication")
public class TuitionBillController {

    private final TuitionBillService tuitionBillService;

    // POST /api/tuition/bills
    // TEACHER: create bill for a student
    @PostMapping("/bills")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<TuitionBillDto>> createBill(
        @Valid @RequestBody CreateTuitionBillRequest request) { ... }

    // POST /api/tuition/bills/batch
    // TEACHER: batch create for class
    @PostMapping("/bills/batch")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<List<TuitionBillDto>>> batchCreateBills(
        @Valid @RequestBody BatchCreateTuitionBillRequest request) { ... }

    // GET /api/tuition/bills/class?classId=1&semesterId=2&status=UNPAID
    // TEACHER: bills for a class
    @GetMapping("/bills/class")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<List<TuitionBillDto>>> getClassBills(
        @RequestParam Long classId,
        @RequestParam Long semesterId,
        @RequestParam(required = false) BillStatus status) { ... }

    // GET /api/tuition/bills/class/summary?classId=1&semesterId=2
    // TEACHER: class summary
    @GetMapping("/bills/class/summary")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<TuitionClassSummaryDto>> getClassSummary(
        @RequestParam Long classId,
        @RequestParam Long semesterId) { ... }

    // PUT /api/tuition/bills/{id}
    // TEACHER: update bill (only UNPAID)
    @PutMapping("/bills/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<TuitionBillDto>> updateBill(
        @PathVariable Long id,
        @Valid @RequestBody CreateTuitionBillRequest request) { ... }

    // DELETE /api/tuition/bills/{id}
    // TEACHER: delete bill (only UNPAID)
    @DeleteMapping("/bills/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<Void>> deleteBill(@PathVariable Long id) { ... }

    // GET /api/tuition/bills/student?studentId=1
    // PARENT: bills for my child
    @GetMapping("/bills/student")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<ApiResponse<TuitionStudentSummaryDto>> getStudentBills(
        @RequestParam Long studentId) { ... }

    // POST /api/tuition/bills/{id}/pay
    // PARENT: initiate payment
    @PostMapping("/bills/{id}/pay")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<ApiResponse<TuitionBillDto>> initiatePayment(
        @PathVariable Long id) { ... }

    // GET /api/tuition/bills/payment-history?studentId=1
    // PARENT: payment history
    @GetMapping("/bills/payment-history")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<ApiResponse<List<TuitionBillDto>>> getPaymentHistory(
        @RequestParam Long studentId) { ... }

    // POST /api/tuition/transactions/{id}/confirm
    // TEACHER: confirm payment (mock)
    @PostMapping("/transactions/{id}/confirm")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<PaymentTransactionDto>> confirmPayment(
        @PathVariable Long id,
        @Valid @RequestBody ConfirmPaymentRequest request) { ... }

    // POST /api/tuition/bills/{id}/simulate-pay
    // TEACHER: simulate payment (dev/demo)
    @PostMapping("/bills/{id}/simulate-pay")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<PaymentTransactionDto>> simulatePayment(
        @PathVariable Long id) { ... }
}

@RestController
@RequestMapping("/api/clubs")
@Tag(name = "Clubs", description = "Đăng ký câu lạc bộ")
@SecurityRequirement(name = "Bearer Authentication")
public class ClubRegistrationController {

    private final ClubRegistrationService clubRegistrationService;

    // POST /api/clubs/register
    // STUDENT: register for club
    @PostMapping("/register")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<ClubRegistrationDto>> register(
        @Valid @RequestBody RegisterClubRequest request) { ... }

    // DELETE /api/clubs/{id}
    // STUDENT: cancel registration
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<Void>> cancelRegistration(
        @PathVariable Long id) { ... }

    // GET /api/clubs/my?academicYear=2026-2027
    // STUDENT: my registrations
    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<List<ClubRegistrationDto>>> getMyRegistrations(
        @RequestParam(required = false) String academicYear) { ... }

    // GET /api/clubs/members?clubName=ABC&academicYear=2026-2027
    // TEACHER: club members
    @GetMapping("/members")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<List<ClubRegistrationDto>>> getClubMembers(
        @RequestParam String clubName,
        @RequestParam(required = false) String academicYear) { ... }
}

@RestController
@RequestMapping("/api/attachments")
@Tag(name = "Attachments", description = "File đính kèm")
@SecurityRequirement(name = "Bearer Authentication")
public class AttachmentController {

    private final AttachmentService attachmentService;

    // POST /api/attachments
    // Any role: create attachment metadata
    @PostMapping
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT', 'TEACHER')")
    public ResponseEntity<ApiResponse<AttachmentDto>> createAttachment(
        @Valid @RequestBody CreateAttachmentRequest request) { ... }

    // GET /api/attachments/leave-request/{leaveRequestId}
    @GetMapping("/leave-request/{leaveRequestId}")
    @PreAuthorize("hasAnyRole('PARENT', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<AttachmentDto>>> getByLeaveRequest(
        @PathVariable Long leaveRequestId) { ... }

    // GET /api/attachments/message/{messageId}
    @GetMapping("/message/{messageId}")
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<AttachmentDto>>> getByMessage(
        @PathVariable Long messageId) { ... }

    // DELETE /api/attachments/{id}
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT', 'TEACHER')")
    public ResponseEntity<ApiResponse<Void>> deleteAttachment(
        @PathVariable Long id) { ... }
}
```

---

## Phase 4 — Summary

### API Endpoints

| # | Method | Endpoint | Auth | Description |
|---|--------|----------|------|-------------|
| 1 | POST | `/api/tuition/bills` | TEACHER | Tạo khoản HP |
| 2 | POST | `/api/tuition/bills/batch` | TEACHER | Tạo hàng loạt |
| 3 | GET | `/api/tuition/bills/class` | TEACHER | HP theo lớp |
| 4 | GET | `/api/tuition/bills/class/summary` | TEACHER | Tổng kết HP lớp |
| 5 | PUT | `/api/tuition/bills/{id}` | TEACHER | Sửa khoản HP |
| 6 | DELETE | `/api/tuition/bills/{id}` | TEACHER | Xóa khoản HP |
| 7 | GET | `/api/tuition/bills/student` | PARENT | HP con tôi |
| 8 | POST | `/api/tuition/bills/{id}/pay` | PARENT | Bắt đầu thanh toán |
| 9 | GET | `/api/tuition/bills/payment-history` | PARENT | Lịch sử GD |
| 10 | POST | `/api/tuition/transactions/{id}/confirm` | TEACHER | Xác nhận GD |
| 11 | POST | `/api/tuition/bills/{id}/simulate-pay` | TEACHER | Mô phỏng thanh toán |
| 12 | POST | `/api/clubs/register` | STUDENT | Đăng ký CLB |
| 13 | DELETE | `/api/clubs/{id}` | STUDENT | Hủy đăng ký |
| 14 | GET | `/api/clubs/my` | STUDENT | CLB của tôi |
| 15 | GET | `/api/clubs/members` | TEACHER | DS thành viên CLB |
| 16 | POST | `/api/attachments` | Any | Tạo metadata đính kèm |
| 17 | GET | `/api/attachments/leave-request/{id}` | PARENT/TEACHER | Đính kèm đơn |
| 18 | GET | `/api/attachments/message/{id}` | Any | Đính kèm tin nhắn |
| 19 | DELETE | `/api/attachments/{id}` | Any | Xóa đính kèm |

### Key Flows

```
GV tạo bill HP → notification cho PH
PH initiate payment → bill = PROCESSING → GD PENDING
GV confirm payment → GD SUCCESS → bill = PAID → notification PH
(Dev: simulate-pay → auto confirm → bill PAID)

HS đăng ký CLB → check unique (HS, CLB, năm) → REGISTERED
HS hủy → status = CANCELLED
```

---

*Tiếp tục: [Phase 5 — Schedule & Reports](phase-5-schedule-reports.md)*
