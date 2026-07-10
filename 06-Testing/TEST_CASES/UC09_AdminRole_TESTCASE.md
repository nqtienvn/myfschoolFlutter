# UC09 Admin Role Test Cases

---

## Document Control

| Field | Value |
| :--- | :--- |
| **Document ID** | `MFS-UC09-TC-001` |
| **Use Case ID** | `UC09` |
| **Use Case Name** | Admin Role & Admin Web Configuration |
| **Module** | `ADMIN` |
| **Version** | `1.0` |
| **Status** | `Ready` |
| **Author** | `Claude` |
| **Reviewer** | `Tech Lead` |
| **Created Date** | `2026-07-07` |
| **Last Updated** | `2026-07-07` |
| **Data Classification** | `Internal, PII-linked` |

---

## Changelog

| Date | Author | Change |
| :--- | :--- | :--- |
| 2026-07-07 | Claude | Created UC09 test case document for Admin Role and Admin Web. |

---

## 1. Use Case Summary

### 1.1 Business Goal

Admin có thể đăng nhập vào web app React để quản trị cấu hình toàn hệ thống MyFschool. Admin không có bảng riêng; tài khoản admin là user có `role = ADMIN`. Teacher không còn quyền ghi cấu hình hệ thống, nhưng vẫn giữ quyền đọc dữ liệu cần cho UI teacher và các nghiệp vụ giáo viên.

### 1.2 Actors

| Actor | Role in UC |
| :--- | :--- |
| Admin | Đăng nhập admin web, xem/lọc tài khoản, khóa/mở tài khoản, quản trị cấu hình học vụ. |
| Teacher | Tiếp tục dùng nghiệp vụ giáo viên, đọc dữ liệu cần cho UI teacher, bị chặn khỏi API cấu hình admin-only. |
| Parent / Student | Không được truy cập admin web hoặc `/api/admin/**`. |
| System | Xác thực JWT, kiểm tra `ROLE_ADMIN`, trả lỗi 401/403/400/404/409 đúng chuẩn. |

### 1.3 Scope

| In Scope | Out of Scope |
| :--- | :--- |
| Thêm role `ADMIN` trong backend auth/authorization. | Tạo bảng `admin` riêng. |
| Admin web React đăng nhập bằng JWT hiện có. | Tạo user thủ công từ admin web v1. |
| Admin xem/lọc tài khoản và khóa/mở tài khoản. | Import account CSV/Excel cho student/parent/teacher. |
| Admin ghi cấu hình lớp, môn, học kỳ, phân công, lịch, học phí, thông báo. | Audit log thao tác admin. |
| Teacher bị chặn khỏi API ghi cấu hình hệ thống. | Phân quyền con kiểu `ADMIN_ACCOUNTS`, `ADMIN_ACADEMIC`. |
| Teacher vẫn dùng được API read/nghiệp vụ teacher. | Admin làm thay nghiệp vụ nhập điểm/điểm danh/duyệt đơn. |

---

## 2. Requirement Traceability

| Requirement ID | Source Document | Requirement Summary | Test Case IDs |
| :--- | :--- | :--- | :--- |
| `UC09-BR-001` | `docs/superpowers/specs/2026-07-07-admin-role-design.md` | Admin là role cao nhất, không có bảng admin riêng. | `TC-ADMIN-API-001`, `TC-ADMIN-INT-001` |
| `UC09-BR-002` | `docs/superpowers/specs/2026-07-07-admin-role-design.md` | Admin xem/lọc tài khoản và khóa/mở tài khoản. | `TC-ADMIN-API-002`, `TC-ADMIN-API-003`, `TC-ADMIN-UI-002` |
| `UC09-BR-003` | `docs/superpowers/specs/2026-07-07-admin-role-design.md` | Admin ghi cấu hình học vụ, teacher không được ghi cấu hình. | `TC-ADMIN-API-004`, `TC-ADMIN-SEC-002`, `TC-ADMIN-E2E-001` |
| `UC09-BR-004` | `docs/superpowers/specs/2026-07-07-admin-role-design.md` | Teacher vẫn đọc được dữ liệu và dùng nghiệp vụ teacher. | `TC-ADMIN-API-005`, `TC-ADMIN-E2E-002` |
| `UC09-SEC-001` | `00-policytest/TESTING_GUIDELINES.md` | API admin chỉ cho phép `ROLE_ADMIN`. | `TC-ADMIN-SEC-001` |
| `UC09-SEC-002` | `00-policytest/TESTING_GUIDELINES.md` | Non-admin không được truy cập admin web/API. | `TC-ADMIN-SEC-001`, `TC-ADMIN-UI-001` |
| `UC09-DB-001` | `docs/database.md` | User role/status lưu trong bảng `users`; không tạo bảng admin. | `TC-ADMIN-INT-001`, `TC-ADMIN-API-003` |

---

## 3. Preconditions & Assumptions

### 3.1 Preconditions

- [ ] Backend đang chạy local tại `http://localhost:8080` hoặc profile test.
- [ ] Admin web đang chạy local tại `http://localhost:5173` nếu test UI.
- [ ] Database đã seed user admin, teacher, parent, student bằng dữ liệu synthetic.
- [ ] Admin user có `role = ADMIN` và `status = ACTIVE`.
- [ ] Không sử dụng dữ liệu thật của học sinh/phụ huynh/giáo viên.

### 3.2 Assumptions

| ID | Assumption | Impact if Wrong |
| :--- | :--- | :--- |
| `ASM-001` | `CustomUserDetails.getAuthorities()` sinh authority dạng `ROLE_` + role name. | Nếu authority format khác, `hasRole('ADMIN')` không hoạt động. |
| `ASM-002` | User status dùng enum `ACTIVE`, `INACTIVE`, `LOCKED`. | Nếu enum thay đổi, API khóa/mở user cần cập nhật validation và test data. |
| `ASM-003` | Admin web dùng JWT từ `/api/auth/login` giống mobile app. | Nếu auth flow tách riêng, UI login và API client phải đổi. |

---

## 4. Test Data

| Data ID | Type | Value / Setup | Used By | Note |
| :--- | :--- | :--- | :--- | :--- |
| `TD-ADMIN-001` | User | phone `0909000009`, password `test1234`, role `ADMIN`, status `ACTIVE` | `TC-ADMIN-*` | Synthetic admin account. |
| `TD-ADMIN-002` | User | phone `0909000098`, email `teacher.test@myfschool.local`, password `test1234`, role `TEACHER`, status `ACTIVE` | `TC-ADMIN-SEC-*`, `TC-ADMIN-API-005` | Synthetic teacher account. |
| `TD-ADMIN-003` | User | phone `0909000097`, email `parent.test@myfschool.local`, password `test1234`, role `PARENT`, status `ACTIVE` | `TC-ADMIN-SEC-001`, `TC-ADMIN-UI-001` | Synthetic parent account. |
| `TD-ADMIN-004` | User | phone `0909000096`, email `student.test@myfschool.local`, password `test1234`, role `STUDENT`, status `ACTIVE` | `TC-ADMIN-SEC-001`, `TC-ADMIN-UI-001` | Synthetic student account. |
| `TD-ADMIN-005` | DB Seed | User ID `2001`, role `STUDENT`, status `ACTIVE` | `TC-ADMIN-API-003`, `TC-ADMIN-UI-002` | Target account for lock/unlock. |
| `TD-ADMIN-006` | API Payload | `{ "status": "LOCKED" }` | `TC-ADMIN-API-003` | Valid lock payload. |
| `TD-ADMIN-007` | API Payload | `{ "status": "ACTIVE" }` | `TC-ADMIN-API-003` | Valid unlock payload. |
| `TD-ADMIN-008` | API Payload | `{ "academicYearId": 1, "gradeLevel": 10, "namingPrefix": "A", "count": 2 }` | `TC-ADMIN-API-004` | Valid `GenerateClassesRequest` payload. |
| `TD-ADMIN-009` | API Payload | `{ "status": "DELETED" }` | `TC-ADMIN-API-006` | Invalid user status payload. |

---

## 5. Test Environment

| Item | Value |
| :--- | :--- |
| Frontend | React Admin Web (`admin-web`) |
| Mobile | Flutter (SDK ^3.11.5), used only for regression check if teacher UI touched |
| Backend | Spring Boot 3.4.5 (Java 21) |
| Database | MySQL / H2 |
| API Base URL | `http://localhost:8080/api` |
| Test Tools | JUnit 5, Spring MockMvc, Postman/curl, browser manual smoke |
| Build Commands | `cd backend && mvn test`, `cd admin-web && npm run build` |

---

## 6. Test Case Summary

| TC ID | Type | Priority | Scenario | Status |
| :--- | :--- | :---: | :--- | :--- |
| `TC-ADMIN-API-001` | API | Critical | Admin login returns JWT with role `ADMIN` | Not Run |
| `TC-ADMIN-API-002` | API | High | Admin lists and filters users | Not Run |
| `TC-ADMIN-API-003` | API | Critical | Admin locks and unlocks user status | Not Run |
| `TC-ADMIN-API-004` | API | Critical | Admin can write academic configuration | Not Run |
| `TC-ADMIN-API-005` | API | High | Teacher read/business APIs still work | Not Run |
| `TC-ADMIN-API-006` | API | Medium | Invalid user status returns 400 and does not update DB | Not Run |
| `TC-ADMIN-SEC-001` | Security | Critical | Non-admin cannot access `/api/admin/**` | Not Run |
| `TC-ADMIN-SEC-002` | Security | Critical | Teacher cannot write system configuration | Not Run |
| `TC-ADMIN-INT-001` | Integration | High | Admin role stored in `users`, no admin table required | Not Run |
| `TC-ADMIN-UI-001` | UI | Critical | Admin web blocks non-admin login | Not Run |
| `TC-ADMIN-UI-002` | UI | High | Admin web lists, filters, locks, unlocks users | Not Run |
| `TC-ADMIN-E2E-001` | E2E | Critical | Admin manages academic config end-to-end | Not Run |
| `TC-ADMIN-E2E-002` | E2E | High | Teacher regression after permission split | Not Run |

Status hợp lệ:

```text
Not Run / Pass / Fail / Blocked / Skipped
```

---

## 7. Detailed Test Cases

### TC-ADMIN-API-001 - Admin login returns JWT with role ADMIN

| Field | Value |
| :--- | :--- |
| **Type** | API |
| **Priority** | Critical |
| **Endpoint** | `POST /api/auth/login` |
| **Auth Required** | No |
| **Required Role** | None |
| **Requirement Ref** | `UC09-BR-001` |
| **Test Data** | `TD-ADMIN-001` |
| **Status** | Not Run |

#### Request

```http
POST /api/auth/login HTTP/1.1
Content-Type: application/json
```

```json
{
  "phone": "0909000099",
  "password": "test1234"
}
```

#### Steps

1. Seed admin user with `role = ADMIN` and `status = ACTIVE`.
2. Send login request.
3. Check HTTP status.
4. Check response body has token and role `ADMIN`.
5. Decode JWT payload in test and verify role claim equals `ADMIN`.

#### Expected Result

- HTTP Status: `200 OK`.
- Response contains a non-empty JWT token.
- Response user/session role is `ADMIN`.
- JWT role claim is `ADMIN`.

#### Actual Result

- Not Run.

#### Evidence

```bash
cd backend && mvn test -Dtest=AdminRoleIntegrationTest#login_adminUser_returnsAdminRole
```

---

### TC-ADMIN-API-002 - Admin lists and filters users

| Field | Value |
| :--- | :--- |
| **Type** | API |
| **Priority** | High |
| **Endpoint** | `GET /api/admin/users?role=STUDENT&status=ACTIVE&keyword=test` |
| **Auth Required** | Yes |
| **Required Role** | `ADMIN` |
| **Requirement Ref** | `UC09-BR-002` |
| **Test Data** | `TD-ADMIN-001`, `TD-ADMIN-005` |
| **Status** | Not Run |

#### Request

```http
GET /api/admin/users?role=STUDENT&status=ACTIVE&keyword=test HTTP/1.1
Authorization: Bearer <admin-token>
```

#### Steps

1. Login as admin and get JWT.
2. Seed at least one active student user matching keyword `test`.
3. Call `GET /api/admin/users?role=STUDENT&status=ACTIVE&keyword=test`.
4. Check HTTP status and response schema.
5. Verify returned users match filters.
6. Verify password hashes are not returned.

#### Expected Result

- HTTP Status: `200 OK`.
- Response contains users with `role = STUDENT`, `status = ACTIVE`.
- Response does not contain password/passwordHash/token fields.

#### Actual Result

- Not Run.

#### Evidence

```bash
cd backend && mvn test -Dtest=AdminRoleIntegrationTest#listUsers_adminRole_returnsFilteredUsersWithoutPassword
```

---

### TC-ADMIN-API-003 - Admin locks and unlocks user status

| Field | Value |
| :--- | :--- |
| **Type** | API |
| **Priority** | Critical |
| **Endpoint** | `PUT /api/admin/users/{id}/status` |
| **Auth Required** | Yes |
| **Required Role** | `ADMIN` |
| **Requirement Ref** | `UC09-BR-002`, `UC09-DB-001` |
| **Test Data** | `TD-ADMIN-001`, `TD-ADMIN-005`, `TD-ADMIN-006`, `TD-ADMIN-007` |
| **Status** | Not Run |

#### Request

```http
PUT /api/admin/users/2001/status HTTP/1.1
Authorization: Bearer <admin-token>
Content-Type: application/json
```

```json
{
  "status": "LOCKED"
}
```

#### Steps

1. Login as admin and get JWT.
2. Seed target user ID `2001` with `status = ACTIVE`.
3. Send lock request with `{ "status": "LOCKED" }`.
4. Verify response status and body.
5. Verify DB user status is `LOCKED`.
6. Send unlock request with `{ "status": "ACTIVE" }`.
7. Verify DB user status is `ACTIVE`.

#### Expected Result

- Lock request returns `200 OK` and user status `LOCKED`.
- Unlock request returns `200 OK` and user status `ACTIVE`.
- Only `users.status` changes; role/profile mapping remains unchanged.

#### Actual Result

- Not Run.

#### Evidence

```bash
cd backend && mvn test -Dtest=AdminRoleIntegrationTest#updateUserStatus_adminRole_locksAndUnlocksUser
```

---

### TC-ADMIN-API-004 - Admin can write academic configuration

| Field | Value |
| :--- | :--- |
| **Type** | API |
| **Priority** | Critical |
| **Endpoint** | `POST /api/classes/generate` |
| **Auth Required** | Yes |
| **Required Role** | `ADMIN` |
| **Requirement Ref** | `UC09-BR-003` |
| **Test Data** | `TD-ADMIN-001`, `TD-ADMIN-008` |
| **Status** | Not Run |

#### Request

```http
POST /api/classes/generate HTTP/1.1
Authorization: Bearer <admin-token>
Content-Type: application/json
```

```json
{
  "academicYearId": 1,
  "gradeLevel": 10,
  "namingPrefix": "A",
  "count": 2
}
```

#### Steps

1. Login as admin and get JWT.
2. Send bulk class generation request using valid payload.
3. Check response status and body.
4. Verify generated classes exist in DB.
5. Delete generated test classes during cleanup.

#### Expected Result

- HTTP Status: `201 Created` or `200 OK` according to current controller contract.
- Response contains the generated class list.
- DB contains classes `10A1` and `10A2` for the selected academic year.

#### Actual Result

- Not Run.

#### Evidence

```bash
cd backend && mvn test -Dtest=AdminRoleIntegrationTest#createClass_adminRole_succeeds
```

---

### TC-ADMIN-API-005 - Teacher read/business APIs still work

| Field | Value |
| :--- | :--- |
| **Type** | API |
| **Priority** | High |
| **Endpoint** | `GET /api/classes`, `GET /api/subjects`, teacher business endpoints |
| **Auth Required** | Yes |
| **Required Role** | `TEACHER` |
| **Requirement Ref** | `UC09-BR-004` |
| **Test Data** | `TD-ADMIN-002` |
| **Status** | Not Run |

#### Request

```http
GET /api/classes HTTP/1.1
Authorization: Bearer <teacher-token>
```

#### Steps

1. Login as teacher and get JWT.
2. Call read endpoints needed by teacher UI: classes, subjects, semesters, teacher schedule.
3. Call one teacher business endpoint such as grade/attendance/leave request action with valid seeded data.
4. Verify responses are successful.

#### Expected Result

- Teacher read endpoints return `200 OK`.
- Teacher business endpoint returns `200 OK` or expected success status.
- Permission split does not break teacher UI/backend workflow.

#### Actual Result

- Not Run.

#### Evidence

```bash
cd backend && mvn test -Dtest=AdminRoleIntegrationTest#teacherReadAndBusinessApis_stillSucceedAfterPermissionSplit
```

---

### TC-ADMIN-API-006 - Invalid user status returns 400 and does not update DB

| Field | Value |
| :--- | :--- |
| **Type** | API |
| **Priority** | Medium |
| **Endpoint** | `PUT /api/admin/users/{id}/status` |
| **Requirement Ref** | `UC09-BR-002` |
| **Test Data** | `TD-ADMIN-001`, `TD-ADMIN-005`, `TD-ADMIN-009` |
| **Status** | Not Run |

#### Invalid Input

```json
{
  "status": "DELETED"
}
```

#### Steps

1. Login as admin and get JWT.
2. Seed target user ID `2001` with `status = ACTIVE`.
3. Send invalid status payload.
4. Check response status and error body.
5. Check DB user status remains `ACTIVE`.

#### Expected Result

- HTTP Status: `400 Bad Request`.
- Response message says status is invalid.
- DB status remains `ACTIVE`.
- No stack trace or internal enum parser details leak in response.

#### Actual Result

- Not Run.

---

### TC-ADMIN-SEC-001 - Non-admin cannot access /api/admin/**

| Field | Value |
| :--- | :--- |
| **Type** | Security |
| **Priority** | Critical |
| **Endpoint/UI** | `GET /api/admin/users` |
| **Security Rule** | Only `ROLE_ADMIN` can access `/api/admin/**`. |
| **Requirement Ref** | `UC09-SEC-001`, `UC09-SEC-002` |
| **Status** | Not Run |

#### Attack / Negative Scenario

Teacher, parent, student, or unauthenticated user tries to access admin user list.

#### Steps

1. Call `GET /api/admin/users` without token.
2. Login as teacher and call `GET /api/admin/users`.
3. Login as parent and call `GET /api/admin/users`.
4. Login as student and call `GET /api/admin/users`.
5. Check status codes and response bodies.

#### Expected Result

- No token returns `401 Unauthorized`.
- Teacher/parent/student tokens return `403 Forbidden`.
- Response does not include user list or PII.

#### Actual Result

- Not Run.

---

### TC-ADMIN-SEC-002 - Teacher cannot write system configuration

| Field | Value |
| :--- | :--- |
| **Type** | Security |
| **Priority** | Critical |
| **Endpoint/UI** | `POST /api/classes/generate`, `POST /api/subjects`, `POST /api/semesters`, `POST /api/schedules` |
| **Security Rule** | Teacher cannot write system configuration after permission split. |
| **Requirement Ref** | `UC09-BR-003` |
| **Status** | Not Run |

#### Attack / Negative Scenario

Teacher uses valid JWT and manually calls admin-only write endpoint.

#### Steps

1. Login as teacher and get JWT.
2. Send `POST /api/classes/generate` with valid payload.
3. Send one more write request to another config endpoint, such as `POST /api/subjects`.
4. Verify response status.
5. Verify DB did not create rows.

#### Expected Result

- Both write requests return `403 Forbidden`.
- No config rows are created or updated.
- Teacher read endpoints remain allowed in separate test `TC-ADMIN-API-005`.

#### Actual Result

- Not Run.

---

### TC-ADMIN-INT-001 - Admin role stored in users, no admin table required

| Field | Value |
| :--- | :--- |
| **Type** | Integration |
| **Priority** | High |
| **Layer** | Database / Repository / Security |
| **Feature Under Test** | `UserRole.ADMIN`, `UserRepository`, `CustomUserDetails.getAuthorities()` |
| **Requirement Ref** | `UC09-BR-001`, `UC09-DB-001` |
| **Test Data** | `TD-ADMIN-001` |
| **Automation File** | `backend/src/test/java/vn/edu/fpt/myfschool/AdminRoleIntegrationTest.java` |
| **Status** | Not Run |

#### Preconditions

- Migration/schema has no `admin` table requirement.
- User seed includes one admin user.

#### Steps

1. Load admin user from `users` by email.
2. Assert `role = ADMIN`.
3. Convert user to `CustomUserDetails`.
4. Assert authorities include `ROLE_ADMIN`.
5. Verify no repository/entity/table named `Admin` is needed for login or authorization.

#### Expected Result

- Admin is represented by `users.role = ADMIN`.
- Security authority is `ROLE_ADMIN`.
- No admin table is required.

#### Actual Result

- Not Run.

#### Evidence

```bash
cd backend && mvn test -Dtest=AdminRoleIntegrationTest#adminRole_usesUsersTableAndRoleAuthority
```

---

### TC-ADMIN-UI-001 - Admin web blocks non-admin login

| Field | Value |
| :--- | :--- |
| **Type** | UI |
| **Priority** | Critical |
| **Screen/Page** | `admin-web LoginPage` |
| **Route** | `/login` |
| **Requirement Ref** | `UC09-SEC-002` |
| **Status** | Not Run |

#### Steps

1. Open admin web login page.
2. Login with teacher account.
3. Observe result.
4. Repeat with parent and student accounts.
5. Verify token is not kept for non-admin session.

#### Expected Result

- Non-admin login is rejected by admin web after role check.
- UI shows “Bạn không có quyền truy cập chức năng này.”
- User stays on login page.
- Admin dashboard is not rendered.

#### Actual Result

- Not Run.

---

### TC-ADMIN-UI-002 - Admin web lists, filters, locks, unlocks users

| Field | Value |
| :--- | :--- |
| **Type** | UI |
| **Priority** | High |
| **Screen/Page** | `admin-web UsersPage` |
| **Route** | `/users` |
| **Requirement Ref** | `UC09-BR-002` |
| **Status** | Not Run |

#### Steps

1. Login as admin.
2. Open Users page.
3. Filter by role `STUDENT`.
4. Filter by status `ACTIVE`.
5. Search keyword `test`.
6. Click lock action on test user.
7. Confirm status changes to `LOCKED`.
8. Click unlock action on same user.
9. Confirm status changes back to `ACTIVE`.

#### Expected Result

- Users list loads without password/token fields.
- Filters update displayed rows.
- Lock/unlock actions call API once per click.
- Loading state disables repeated clicks.
- Success/error message is visible.

#### Actual Result

- Not Run.

---

### TC-ADMIN-E2E-001 - Admin manages academic config end-to-end

| Field | Value |
| :--- | :--- |
| **Type** | E2E |
| **Priority** | Critical |
| **Business Flow** | Admin login → generate classes → verify list → cleanup |
| **Actors** | Admin, System |
| **Requirement Ref** | `UC09-BR-003` |
| **Status** | Not Run |

#### Steps

1. Login admin web with admin account.
2. Open Classes page.
3. Generate two classes for grade level `10`, prefix `A`, academic year `2026-2027`.
4. Verify classes `10A1` and `10A2` appear in the list.
5. Delete generated classes during cleanup.
6. Repeat smoke path for one other config module, such as Subject or Semester.

#### Expected Result

- Admin can generate/delete classes and create/edit/delete other config data.
- UI reflects backend response after each action.
- DB state matches final cleanup state.
- No teacher token is used in this flow.

#### Actual Result

- Not Run.

---

### TC-ADMIN-E2E-002 - Teacher regression after permission split

| Field | Value |
| :--- | :--- |
| **Type** | E2E |
| **Priority** | High |
| **Business Flow** | Teacher login → read UI data → perform teacher business action → blocked from config write |
| **Actors** | Teacher, System |
| **Requirement Ref** | `UC09-BR-004` |
| **Status** | Not Run |

#### Steps

1. Login as teacher in existing teacher UI/API flow.
2. Open teacher home and screens that read classes/subjects/semesters/schedule.
3. Perform one teacher business action with test data, such as grade update or attendance submit.
4. Try to call one config write endpoint manually with teacher JWT.
5. Verify read/business success and config write forbidden.

#### Expected Result

- Teacher UI read data still loads.
- Teacher business action succeeds.
- Teacher config write returns `403 Forbidden`.
- No regression in teacher navigation caused by admin split.

#### Actual Result

- Not Run.

---

## 8. State Transition Matrix

### 8.1 User status

| Entity | From State | Action | To State | Allowed? | Test Case |
| :--- | :--- | :--- | :--- | :---: | :--- |
| `User` | `ACTIVE` | Admin lock user | `LOCKED` | Yes | `TC-ADMIN-API-003` |
| `User` | `LOCKED` | Admin unlock user | `ACTIVE` | Yes | `TC-ADMIN-API-003` |
| `User` | `INACTIVE` | Admin activate user | `ACTIVE` | Yes | `TC-ADMIN-API-003` |
| `User` | `ACTIVE` | Teacher lock user | `ACTIVE` | No change | `TC-ADMIN-SEC-001` |
| `User` | `ACTIVE` | Invalid status `DELETED` | `ACTIVE` | No change | `TC-ADMIN-API-006` |

---

## 9. Database Verification

| Check ID | SQL / Verification | Expected Result | Test Case |
| :--- | :--- | :--- | :--- |
| `DB-ADMIN-001` | `SELECT role, status FROM users WHERE email = 'admin.test@myfschool.local';` | `role = 'ADMIN'`, `status = 'ACTIVE'` | `TC-ADMIN-API-001`, `TC-ADMIN-INT-001` |
| `DB-ADMIN-002` | `SELECT status FROM users WHERE id = 2001;` | `LOCKED` after lock, `ACTIVE` after unlock | `TC-ADMIN-API-003` |
| `DB-ADMIN-003` | `SELECT COUNT(*) FROM users WHERE id = 2001 AND status = 'ACTIVE';` | Count remains `1` after invalid status payload | `TC-ADMIN-API-006` |
| `DB-ADMIN-004` | `SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'admin';` | `0` | `TC-ADMIN-INT-001` |
| `DB-ADMIN-005` | `SELECT COUNT(*) FROM classes WHERE name IN ('10A1', '10A2') AND academic_year_id = 1;` | `2` after generation, `0` after cleanup | `TC-ADMIN-API-004`, `TC-ADMIN-E2E-001` |

```sql
SELECT role, status
FROM users
WHERE email = 'admin.test@myfschool.local';

SELECT status
FROM users
WHERE id = 2001;
```

---

## 10. Build & Test Execution Log

| Date | Runner | Command | Result | Note |
| :--- | :--- | :--- | :--- | :--- |
| 2026-07-08 | Claude | `cd backend && mvn test` | Pass | 73 tests, 0 failures |
| 2026-07-08 | Claude | `cd admin-web && npm run build` | Pass | Vite build succeeds |
| 2026-07-08 | Claude | Manual admin web smoke | Not Run | Run after backend + admin web implementation |

---

## 11. Defects Found

| Defect ID | Test Case | Severity | Description | Status | Owner |
| :--- | :--- | :--- | :--- | :--- | :--- |
| N/A | N/A | N/A | No defects logged yet. | N/A | N/A |

---

## 12. Exit Criteria

UC09 chỉ được nghiệm thu khi:

- [ ] Tất cả test case Priority Critical/High đã Pass.
- [ ] Không còn bug Critical/High đang Open.
- [ ] Build backend pass.
- [ ] Build admin web pass.
- [ ] Không có password/JWT/token bị leak trong response, log, screenshot, evidence.
- [ ] Database state đúng sau test và data test được cleanup.
- [ ] Reviewer đã kiểm tra và approve.

---

## 13. Final Review

| Role | Name | Decision | Date | Note |
| :--- | :--- | :--- | :--- | :--- |
| Developer | Claude | Pending | 2026-07-07 | Draft created before implementation. |
| Tester/QA | QA | Pending | 2026-07-07 | Review after implementation plan. |
| Tech Lead | Tech Lead | Pending | 2026-07-07 | Review before coding. |
