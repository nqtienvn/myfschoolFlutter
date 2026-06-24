# MyFschool Backend — Full Spec (Java Spring Boot)

> Phiên bản: 1.0 — Ngày: 2026-06-24
> Kiến trúc: Monolith — Spring Boot 3.x + MySQL + JWT + WebSocket

---

## 1. Tổng quan

Backend REST API phục vụ ứng dụng Sổ liên lạc điện tử **MyFschool** cho 3 role:
- **PARENT** (Phụ huynh)
- **STUDENT** (Học sinh)
- **TEACHER** (Giáo viên / GVCN)

1 account = 1 role duy nhất. JWT chứa `userId`, `role`.

---

## 2. Tech Stack

| Layer | Technology |
|-------|-----------|
| Framework | Spring Boot 3.4.x |
| Java | 21 LTS |
| Database | MySQL 8.x (utf8mb4_unicode_ci, InnoDB) |
| ORM | Spring Data JPA + Hibernate |
| Auth | JWT (jjwt 0.12.x) |
| Password | BCrypt |
| WebSocket | Spring WebSocket (raw, không STOMP) |
| Validation | Jakarta Bean Validation 2.0 |
| Mapping | MapStruct 1.6.x |
| API Docs | SpringDoc OpenAPI 3.0 (Swagger UI) |
| Build | Maven |

---

## 3. Project Structure

```
backend/
├── pom.xml
└── src/
    └── main/
        ├── java/vn/edu/fpt/myfschool/
        │   ├── MyfSchoolApplication.java
        │   │
        │   ├── config/
        │   │   ├── SecurityConfig.java
        │   │   ├── JwtConfig.java
        │   │   ├── WebConfig.java
        │   │   ├── WebSocketConfig.java
        │   │   └── OpenApiConfig.java
        │   │
        │   ├── security/
        │   │   ├── JwtTokenProvider.java
        │   │   ├── JwtAuthenticationFilter.java
        │   │   ├── UserDetailsServiceImpl.java
        │   │   └── CustomUserDetails.java
        │   │
        │   ├── common/
        │   │   ├── enums/
        │   │   │   ├── UserRole.java
        │   │   │   ├── UserStatus.java
        │   │   │   ├── AttendanceStatus.java
        │   │   │   ├── LeaveShift.java
        │   │   │   ├── LeaveStatus.java
        │   │   │   ├── Theme.java
        │   │   │   ├── Language.java
        │   │   │   ├── Shift.java
        │   │   │   ├── BillStatus.java
        │   │   │   ├── PaymentStatus.java
        │   │   │   ├── TargetRole.java
        │   │   │   ├── Relationship.java
        │   │   │   └── ClubStatus.java
        │   │   │
        │   │   ├── dto/
        │   │   │   ├── ApiResponse.java
        │   │   │   ├── PagedResponse.java
        │   │   │   └── ErrorResponse.java
        │   │   │
        │   │   ├── exception/
        │   │   │   ├── GlobalExceptionHandler.java
        │   │   │   ├── ResourceNotFoundException.java
        │   │   │   ├── BadRequestException.java
        │   │   │   ├── UnauthorizedException.java
        │   │   │   └── ForbiddenException.java
        │   │   │
        │   │   └── util/
        │   │       ├── SecurityUtil.java
        │   │       └── GradeCalculator.java
        │   │
        │   ├── entity/
        │   │   ├── BaseEntity.java          (id, createdAt, updatedAt)
        │   │   ├── User.java
        │   │   ├── UserSetting.java
        │   │   ├── Parent.java
        │   │   ├── Student.java
        │   │   ├── Teacher.java
        │   │   ├── StudentGuardian.java
        │   │   ├── StudentClass.java
        │   │   ├── Class.java
        │   │   ├── Subject.java
        │   │   ├── Semester.java
        │   │   ├── ClassSubject.java
        │   │   ├── Schedule.java
        │   │   ├── Grade.java
        │   │   ├── SemesterResult.java
        │   │   ├── Attendance.java
        │   │   ├── LeaveRequest.java
        │   │   ├── TuitionBill.java
        │   │   ├── PaymentTransaction.java
        │   │   ├── Announcement.java
        │   │   ├── AnnouncementClass.java
        │   │   ├── AnnouncementRead.java
        │   │   ├── Conversation.java
        │   │   ├── ConversationParticipant.java
        │   │   ├── Message.java
        │   │   ├── Notification.java
        │   │   ├── ClubRegistration.java
        │   │   └── Attachment.java
        │   │
        │   ├── repository/
        │   │   ├── UserRepository.java
        │   │   ├── UserSettingRepository.java
        │   │   ├── ParentRepository.java
        │   │   ├── StudentRepository.java
        │   │   ├── TeacherRepository.java
        │   │   ├── StudentGuardianRepository.java
        │   │   ├── StudentClassRepository.java
        │   │   ├── ClassRepository.java
        │   │   ├── SubjectRepository.java
        │   │   ├── SemesterRepository.java
        │   │   ├── ClassSubjectRepository.java
        │   │   ├── ScheduleRepository.java
        │   │   ├── GradeRepository.java
        │   │   ├── SemesterResultRepository.java
        │   │   ├── AttendanceRepository.java
        │   │   ├── LeaveRequestRepository.java
        │   │   ├── TuitionBillRepository.java
        │   │   ├── PaymentTransactionRepository.java
        │   │   ├── AnnouncementRepository.java
        │   │   ├── AnnouncementClassRepository.java
        │   │   ├── AnnouncementReadRepository.java
        │   │   ├── ConversationRepository.java
        │   │   ├── ConversationParticipantRepository.java
        │   │   ├── MessageRepository.java
        │   │   ├── NotificationRepository.java
        │   │   ├── ClubRegistrationRepository.java
        │   │   └── AttachmentRepository.java
        │   │
        │   ├── mapper/
        │   │   ├── UserMapper.java
        │   │   ├── ParentMapper.java
        │   │   ├── StudentMapper.java
        │   │   ├── TeacherMapper.java
        │   │   ├── ClassMapper.java
        │   │   ├── SubjectMapper.java
        │   │   ├── SemesterMapper.java
        │   │   ├── GradeMapper.java
        │   │   ├── SemesterResultMapper.java
        │   │   ├── AttendanceMapper.java
        │   │   ├── LeaveRequestMapper.java
        │   │   ├── TuitionBillMapper.java
        │   │   ├── AnnouncementMapper.java
        │   │   ├── MessageMapper.java
        │   │   ├── NotificationMapper.java
        │   │   └── ScheduleMapper.java
        │   │
        │   ├── service/
        │   │   ├── AuthService.java
        │   │   ├── UserService.java
        │   │   ├── ParentService.java
        │   │   ├── StudentService.java
        │   │   ├── TeacherService.java
        │   │   ├── ClassService.java
        │   │   ├── SubjectService.java
        │   │   ├── SemesterService.java
        │   │   ├── GradeService.java
        │   │   ├── SemesterResultService.java
        │   │   ├── AttendanceService.java
        │   │   ├── LeaveRequestService.java
        │   │   ├── TuitionBillService.java
        │   │   ├── PaymentTransactionService.java
        │   │   ├── AnnouncementService.java
        │   │   ├── ConversationService.java
        │   │   ├── MessageService.java
        │   │   ├── NotificationService.java
        │   │   ├── ClubRegistrationService.java
        │   │   ├── ScheduleService.java
        │   │   └── AttachmentService.java
        │   │
        │   ├── controller/
        │   │   ├── AuthController.java
        │   │   ├── UserController.java
        │   │   ├── ParentController.java
        │   │   ├── StudentController.java
        │   │   ├── TeacherController.java
        │   │   ├── ClassController.java
        │   │   ├── SubjectController.java
        │   │   ├── SemesterController.java
        │   │   ├── GradeController.java
        │   │   ├── SemesterResultController.java
        │   │   ├── AttendanceController.java
        │   │   ├── LeaveRequestController.java
        │   │   ├── TuitionBillController.java
        │   │   ├── PaymentTransactionController.java
        │   │   ├── AnnouncementController.java
        │   │   ├── ConversationController.java
        │   │   ├── MessageController.java
        │   │   ├── NotificationController.java
        │   │   ├── ClubRegistrationController.java
        │   │   ├── ScheduleController.java
        │   │   └── DashboardController.java
        │   │
        │   └── websocket/
        │       ├── ChatWebSocketHandler.java
        │       ├── WebSocketSessionManager.java
        │       └── WebSocketAuthInterceptor.java
        │
        └── resources/
            ├── application.yml
            ├── application-dev.yml
            ├── application-prod.yml
            ├── data.sql                    (sample data)
            ├── schema.sql                  (DDL auto-generate hoặc manual)
            └── db/
                └── migration/
                    └── V1__init.sql        (Flyway migration)
```

---

## 4. Phases

| Phase | Scope | Tables | Parallel Tasks |
|-------|-------|--------|----------------|
| **P1** | Foundation | 11 tables | P1A: Auth+User, P1B: Reference CRUD |
| **P2** | Academic | 4 tables | P2A: Grades, P2B: Attendance+Leave |
| **P3** | Communication | 7 tables | P3A: Announcements, P3B: Messaging+WS |
| **P4** | Business | 3 tables | P4A: Tuition, P4B: Clubs+Attachments |
| **P5** | Schedule+Reports | 1 table | Schedule + Dashboard stats |

Mỗi phase có thể implement song song bằng subagent khi đã có spec chi tiết.

---

## 5. Cross-Cutting Concerns

### 5.1. API Response Format

```json
{
  "success": true,
  "message": "Thành công",
  "data": { ... },
  "timestamp": "2026-06-24T10:30:00"
}
```

Error:
```json
{
  "success": false,
  "message": "Lỗi validation",
  "errors": [
    { "field": "phone", "message": "Số điện thoại không hợp lệ" }
  ],
  "timestamp": "2026-06-24T10:30:00"
}
```

### 5.2. Pagination

Request: `GET /api/students?page=0&size=20&sort=created_at,desc`

Response:
```json
{
  "content": [...],
  "page": 0,
  "size": 20,
  "totalElements": 150,
  "totalPages": 8
}
```

### 5.3. Authentication

- Header: `Authorization: Bearer <jwt_token>`
- JWT payload:
```json
{
  "sub": "1",
  "role": "PARENT",
  "name": "Nguyễn Văn An",
  "iat": 1719216600,
  "exp": 1719303000
}
```
- Token expiry: 24h (configurable)
- Refresh token: chưa implement phase 1

### 5.4. RBAC (Role-Based Access Control)

Mỗi endpoint gán role được phép:

```java
@PreAuthorize("hasRole('TEACHER')")
// hoặc
@PreAuthorize("hasAnyRole('PARENT', 'STUDENT')")
```

Role hierarchy:
```
TEACHER → xem điểm + điểm danh + duyệt đơn + CRUD TKB
PARENT  → xem điểm con + tạo đơn nghỉ + xem HP
STUDENT → xem điểm mình + xem TKB mình + đăng ký CLB
```

### 5.5. Global Exception Handling

| HTTP Code | Exception | Mô tả |
|-----------|-----------|-------|
| 400 | BadRequestException | Validation fail, bad input |
| 401 | UnauthorizedException | JWT invalid/expired |
| 403 | ForbiddenException | Role không đủ quyền |
| 404 | ResourceNotFoundException | Entity không tồn tại |
| 409 | ConflictException | Duplicate key, business rule |
| 500 | InternalServerException | Lỗi hệ thống |

### 5.6. Enums (Java)

```java
public enum UserRole {
    PARENT, STUDENT, TEACHER
}

public enum UserStatus {
    ACTIVE, INACTIVE, LOCKED
}

public enum AttendanceStatus {
    PRESENT, LATE, ABSENT_WITH_LEAVE, ABSENT_WITHOUT_LEAVE
}

public enum LeaveShift {
    FULL_DAY, MORNING, AFTERNOON
}

public enum LeaveStatus {
    PENDING, APPROVED, REJECTED
}

public enum Theme {
    LIGHT, DARK
}

public enum Language {
    VI, EN
}

public enum Shift {
    MORNING, AFTERNOON
}

public enum BillStatus {
    UNPAID, PROCESSING, PAID
}

public enum PaymentStatus {
    PENDING, SUCCESS, FAILED
}

public enum TargetRole {
    PARENT, STUDENT, ALL
}

public enum Relationship {
    FATHER, MOTHER, GUARDIAN
}

public enum ClubStatus {
    REGISTERED, CANCELLED
}
```

---

## 6. Database Config

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/myfschool?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=utf8mb4
    username: root
    password: ${DB_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5

  jpa:
    hibernate:
      ddl-auto: validate    # Dùng Flyway để manage schema
    show-sql: false
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.MySQLDialect

  flyway:
    enabled: true
    locations: classpath:db/migration
```

---

*Spec tổng quan — xem chi tiết từng phase trong các file riêng.*
