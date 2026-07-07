# Admin Role & Permission Split Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add `ADMIN` role to backend, create admin web API for user management + academic config, split permissions so TEACHER loses write access to system config, and scaffold a React admin web app.

**Architecture:** Backend-first: add `ADMIN` to `UserRole` enum, create `AdminUserController` under `/api/admin/**`, update existing controllers' `@PreAuthorize` to replace `TEACHER` write with `ADMIN`. Admin web is a minimal React SPA under `admin-web/` using `fetch` + `localStorage` for JWT, no UI library. Test doc `UC09_AdminRole_TESTCASE.md` already exists as draft.

**Tech Stack:** Spring Boot 3.4.5, Java 21, H2 (test), MySQL (dev), React (admin-web), Vite

---

## Global Constraints

- Backend: Spring Boot 3.4.5, Java 21, H2 for tests, MySQL for dev
- Admin web: React + Vite, no UI library, `fetch` for API, `localStorage` for JWT
- Only Edit, commit and push code on branch `master`
- Test case format follows `00-policytest/TESTCASE_WRITING_GUIDE.md`
- Vietnamese UI text, English code identifiers

---

## File Structure

```text
# Modified
backend/src/main/java/vn/edu/fpt/myfschool/common/enums/UserRole.java
backend/src/main/java/vn/edu/fpt/myfschool/service/impl/AuthServiceImpl.java
backend/src/main/java/vn/edu/fpt/myfschool/repository/UserRepository.java
backend/src/main/java/vn/edu/fpt/myfschool/controller/ClassController.java
backend/src/main/java/vn/edu/fpt/myfschool/controller/SubjectController.java
backend/src/main/java/vn/edu/fpt/myfschool/controller/SemesterController.java
backend/src/main/java/vn/edu/fpt/myfschool/controller/ScheduleController.java
backend/src/main/java/vn/edu/fpt/myfschool/controller/TuitionBillController.java
backend/src/main/java/vn/edu/fpt/myfschool/controller/AnnouncementController.java
backend/src/test/java/vn/edu/fpt/myfschool/BaseIntegrationTest.java
06-Testing/TEST_CASES/UC09_AdminRole_TESTCASE.md
06-Testing/TEST_CASES/TESTCASE_INDEX.md

# Created
backend/src/main/java/vn/edu/fpt/myfschool/common/dto/AdminUserDto.java
backend/src/main/java/vn/edu/fpt/myfschool/common/dto/UpdateUserStatusRequest.java
backend/src/main/java/vn/edu/fpt/myfschool/service/AdminUserService.java
backend/src/main/java/vn/edu/fpt/myfschool/service/impl/AdminUserServiceImpl.java
backend/src/main/java/vn/edu/fpt/myfschool/controller/AdminUserController.java
backend/src/test/java/vn/edu/fpt/myfschool/AdminRoleIntegrationTest.java
admin-web/ (entire React app)
```

---

## Phase 1: Backend Auth — Add ADMIN to UserRole

### Task 1: Add ADMIN to UserRole enum

**Files:**
- Modify: `backend/src/main/java/vn/edu/fpt/myfschool/common/enums/UserRole.java:3`

- [ ] **Step 1: Add ADMIN to enum**

```java
public enum UserRole {
    PARENT, STUDENT, TEACHER, ADMIN
}
```

- [ ] **Step 2: Verify compile**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/vn/edu/fpt/myfschool/common/enums/UserRole.java
git commit -m "feat(backend): add ADMIN to UserRole enum"
```

### Task 2: Handle ADMIN in AuthServiceImpl.register()

**Files:**
- Modify: `backend/src/main/java/vn/edu/fpt/myfschool/service/impl/AuthServiceImpl.java:71-104`

**Why:** Current `register()` switch handles PARENT, STUDENT, TEACHER. ADMIN falls through silently — no profile created, but no error either. That's fine for v1 since admin has no profile. Just add explicit `ADMIN -> {}` branch for clarity.

- [ ] **Step 1: Add ADMIN case to register switch**

In `AuthServiceImpl.java`, inside the `switch (request.role())` block (line 71), after the `TEACHER` case, add:

```java
            case ADMIN -> {
                // ponytail: admin has no profile table, user record is sufficient
            }
```

- [ ] **Step 2: Verify compile**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/vn/edu/fpt/myfschool/service/impl/AuthServiceImpl.java
git commit -m "feat(backend): handle ADMIN role in register switch"
```

### Task 3: Seed admin user in BaseIntegrationTest

**Files:**
- Modify: `backend/src/test/java/vn/edu/fpt/myfschool/BaseIntegrationTest.java:57-160`

**Why:** Tests need an admin user to verify role-based access. Seed it in `setUpTestData()`.

- [ ] **Step 1: Add admin user seed in BaseIntegrationTest**

After the parent user/parent block (after line 131, before the student loop at line 133), add:

```java
        // Admin user
        User adminUser = new User();
        adminUser.setPhone("0909000009");
        adminUser.setPassword(passwordEncoder.encode("test1234"));
        adminUser.setName("Admin Test");
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setStatus(UserStatus.ACTIVE);
        adminUser = userRepository.save(adminUser);
```

After the field declarations (after line 56 `protected Parent testParent;`), add:

```java
    protected User testAdminUser;
```

Update the admin user seed to assign to the field:

```java
        testAdminUser = userRepository.save(adminUser);
```

- [ ] **Step 2: Add loginAsAdmin() helper**

After `loginAsStudent2()` (line 216), add:

```java
    protected String loginAsAdmin() throws Exception {
        return login("0909000009", "test1234");
    }
```

- [ ] **Step 3: Verify existing tests still pass**

Run: `cd backend && mvn test -q`
Expected: All existing tests pass (admin user is additive, no conflicts)

- [ ] **Step 4: Commit**

```bash
git add backend/src/test/java/vn/edu/fpt/myfschool/BaseIntegrationTest.java
git commit -m "feat(test): seed admin user in BaseIntegrationTest"
```

---

## Phase 2: Backend — Admin User Management API

### Task 4: Create AdminUserDto and UpdateUserStatusRequest

**Files:**
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/AdminUserDto.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/common/dto/UpdateUserStatusRequest.java`

- [ ] **Step 1: Create AdminUserDto**

```java
package vn.edu.fpt.myfschool.common.dto;

import vn.edu.fpt.myfschool.common.enums.UserRole;
import vn.edu.fpt.myfschool.common.enums.UserStatus;
import java.time.LocalDateTime;

public record AdminUserDto(
    Long id,
    String phone,
    String name,
    String email,
    UserRole role,
    UserStatus status,
    LocalDateTime createdAt
) {}
```

- [ ] **Step 2: Create UpdateUserStatusRequest**

```java
package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.NotNull;
import vn.edu.fpt.myfschool.common.enums.UserStatus;

public record UpdateUserStatusRequest(
    @NotNull UserStatus status
) {}
```

- [ ] **Step 3: Verify compile**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/vn/edu/fpt/myfschool/common/dto/AdminUserDto.java \
       backend/src/main/java/vn/edu/fpt/myfschool/common/dto/UpdateUserStatusRequest.java
git commit -m "feat(backend): add AdminUserDto and UpdateUserStatusRequest"
```

### Task 5: Add search queries to UserRepository

**Files:**
- Modify: `backend/src/main/java/vn/edu/fpt/myfschool/repository/UserRepository.java`

**Why:** Admin user list needs filtering by role, status, and keyword. Existing `searchByRoleAndKeyword` doesn't cover status filter or combined filters. Add one method that handles all three optional filters.

- [ ] **Step 1: Add searchAdminUsers query**

In `UserRepository.java`, before the closing `}`, add:

```java
    @Query("SELECT u FROM User u WHERE " +
           "(:role IS NULL OR u.role = :role) AND " +
           "(:status IS NULL OR u.status = :status) AND " +
           "(:keyword IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "u.phone LIKE CONCAT('%', :keyword, '%'))")
    List<User> searchAdminUsers(@Param("role") UserRole role,
                                 @Param("status") UserStatus status,
                                 @Param("keyword") String keyword);
```

- [ ] **Step 2: Verify compile**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/vn/edu/fpt/myfschool/repository/UserRepository.java
git commit -m "feat(backend): add searchAdminUsers query with role/status/keyword filters"
```

### Task 6: Create AdminUserService and implementation

**Files:**
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/service/AdminUserService.java`
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/service/impl/AdminUserServiceImpl.java`

- [ ] **Step 1: Create AdminUserService interface**

```java
package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.common.dto.AdminUserDto;
import vn.edu.fpt.myfschool.common.enums.UserRole;
import vn.edu.fpt.myfschool.common.enums.UserStatus;
import java.util.List;

public interface AdminUserService {
    List<AdminUserDto> listUsers(UserRole role, UserStatus status, String keyword);
    AdminUserDto updateUserStatus(Long userId, UserStatus status);
}
```

- [ ] **Step 2: Create AdminUserServiceImpl**

```java
package vn.edu.fpt.myfschool.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.AdminUserDto;
import vn.edu.fpt.myfschool.common.enums.UserRole;
import vn.edu.fpt.myfschool.common.enums.UserStatus;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.entity.User;
import vn.edu.fpt.myfschool.repository.UserRepository;
import vn.edu.fpt.myfschool.service.AdminUserService;
import java.util.List;
import java.util.stream.Collectors;

@Service("adminUserService")
@RequiredArgsConstructor
@Transactional
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    @Override
    public List<AdminUserDto> listUsers(UserRole role, UserStatus status, String keyword) {
        return userRepository.searchAdminUsers(role, status, keyword)
            .stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    @Override
    public AdminUserDto updateUserStatus(Long userId, UserStatus status) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        user.setStatus(status);
        user = userRepository.save(user);
        return toDto(user);
    }

    private AdminUserDto toDto(User user) {
        return new AdminUserDto(
            user.getId(), user.getPhone(), user.getName(), user.getEmail(),
            user.getRole(), user.getStatus(), user.getCreatedAt());
    }
}
```

- [ ] **Step 3: Verify compile**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/vn/edu/fpt/myfschool/service/AdminUserService.java \
       backend/src/main/java/vn/edu/fpt/myfschool/service/impl/AdminUserServiceImpl.java
git commit -m "feat(backend): add AdminUserService for user list and status update"
```

### Task 7: Create AdminUserController

**Files:**
- Create: `backend/src/main/java/vn/edu/fpt/myfschool/controller/AdminUserController.java`

- [ ] **Step 1: Create AdminUserController**

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
import vn.edu.fpt.myfschool.common.dto.AdminUserDto;
import vn.edu.fpt.myfschool.common.dto.ApiResponse;
import vn.edu.fpt.myfschool.common.dto.UpdateUserStatusRequest;
import vn.edu.fpt.myfschool.common.enums.UserRole;
import vn.edu.fpt.myfschool.common.enums.UserStatus;
import vn.edu.fpt.myfschool.service.AdminUserService;
import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin Users", description = "Quản lý tài khoản (Admin only)")
@SecurityRequirement(name = "Bearer Authentication")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Danh sách tài khoản")
    public ResponseEntity<ApiResponse<List<AdminUserDto>>> listUsers(
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(ApiResponse.success(
            adminUserService.listUsers(role, status, keyword)));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Khóa/mở tài khoản")
    public ResponseEntity<ApiResponse<AdminUserDto>> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
            "Cập nhật trạng thái thành công",
            adminUserService.updateUserStatus(id, request.status())));
    }
}
```

- [ ] **Step 2: Verify compile**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/vn/edu/fpt/myfschool/controller/AdminUserController.java
git commit -m "feat(backend): add AdminUserController for user management"
```

### Task 8: Admin user management integration tests

**Files:**
- Create: `backend/src/test/java/vn/edu/fpt/myfschool/AdminRoleIntegrationTest.java`

- [ ] **Step 1: Create AdminRoleIntegrationTest**

```java
package vn.edu.fpt.myfschool;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import vn.edu.fpt.myfschool.common.enums.UserRole;
import vn.edu.fpt.myfschool.common.enums.UserStatus;
import vn.edu.fpt.myfschool.entity.User;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminRoleIntegrationTest extends BaseIntegrationTest {

    @Test
    void login_adminUser_returnsAdminRole() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"0909000009\",\"password\":\"test1234\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.token").isNotEmpty())
            .andExpect(jsonPath("$.data.user.role").value("ADMIN"));
    }

    @Test
    void listUsers_adminRole_returnsFilteredUsers() throws Exception {
        String token = loginAsAdmin();

        mockMvc.perform(get("/api/admin/users")
                .header("Authorization", authHeader(token))
                .param("role", "STUDENT"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data[0].role").value("STUDENT"));
    }

    @Test
    void updateUserStatus_adminRole_locksAndUnlocks() throws Exception {
        String token = loginAsAdmin();
        Long targetId = testStudent1.getUser().getId();

        // Lock
        mockMvc.perform(put("/api/admin/users/" + targetId + "/status")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"LOCKED\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("LOCKED"));

        // Unlock
        mockMvc.perform(put("/api/admin/users/" + targetId + "/status")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"ACTIVE\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void updateUserStatus_invalidStatus_returns400() throws Exception {
        String token = loginAsAdmin();
        Long targetId = testStudent1.getUser().getId();

        mockMvc.perform(put("/api/admin/users/" + targetId + "/status")
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"DELETED\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void listUsers_teacherRole_returns403() throws Exception {
        String token = loginAsTeacher();

        mockMvc.perform(get("/api/admin/users")
                .header("Authorization", authHeader(token)))
            .andExpect(status().isForbidden());
    }

    @Test
    void listUsers_parentRole_returns403() throws Exception {
        String token = loginAsParent();

        mockMvc.perform(get("/api/admin/users")
                .header("Authorization", authHeader(token)))
            .andExpect(status().isForbidden());
    }

    @Test
    void listUsers_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
            .andExpect(status().isUnauthorized());
    }
}
```

- [ ] **Step 2: Run tests**

Run: `cd backend && mvn test -Dtest=AdminRoleIntegrationTest -q`
Expected: All 7 tests pass

- [ ] **Step 3: Commit**

```bash
git add backend/src/test/java/vn/edu/fpt/myfschool/AdminRoleIntegrationTest.java
git commit -m "test(backend): add AdminRoleIntegrationTest for user management API"
```

---

## Phase 3: Backend — Permission Split

### Task 9: Update ClassController permissions

**Files:**
- Modify: `backend/src/main/java/vn/edu/fpt/myfschool/controller/ClassController.java`

- [ ] **Step 1: Update read endpoints to include ADMIN**

Change `@PreAuthorize` on `listClasses` (line 27):
```java
    @PreAuthorize("hasAnyRole('ADMIN', 'PARENT', 'STUDENT', 'TEACHER')")
```

Change `@PreAuthorize` on `getClassDetail` (line 39):
```java
    @PreAuthorize("hasAnyRole('ADMIN', 'PARENT', 'STUDENT', 'TEACHER')")
```

Change `@PreAuthorize` on `getStudents` (line 71):
```java
    @PreAuthorize("hasAnyRole('ADMIN', 'PARENT', 'STUDENT', 'TEACHER')")
```

- [ ] **Step 2: Update write endpoints to ADMIN only**

Change `@PreAuthorize` on `createClass` (line 46):
```java
    @PreAuthorize("hasRole('ADMIN')")
```

Change `@PreAuthorize` on `updateClass` (line 54):
```java
    @PreAuthorize("hasRole('ADMIN')")
```

Change `@PreAuthorize` on `deleteClass` (line 63):
```java
    @PreAuthorize("hasRole('ADMIN')")
```

Change `@PreAuthorize` on `assignSubject` (line 78):
```java
    @PreAuthorize("hasRole('ADMIN')")
```

Change `@PreAuthorize` on `removeSubject` (line 89):
```java
    @PreAuthorize("hasRole('ADMIN')")
```

- [ ] **Step 3: Verify compile**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/vn/edu/fpt/myfschool/controller/ClassController.java
git commit -m "refactor(backend): split ClassController permissions - write to ADMIN only"
```

### Task 10: Update SubjectController permissions

**Files:**
- Modify: `backend/src/main/java/vn/edu/fpt/myfschool/controller/SubjectController.java`

- [ ] **Step 1: Update read endpoints**

Change `@PreAuthorize` on `listSubjects` (line 26):
```java
    @PreAuthorize("hasAnyRole('ADMIN', 'PARENT', 'STUDENT', 'TEACHER')")
```

Change `@PreAuthorize` on `getSubject` (line 34):
```java
    @PreAuthorize("hasAnyRole('ADMIN', 'PARENT', 'STUDENT', 'TEACHER')")
```

- [ ] **Step 2: Update write endpoints**

Change `@PreAuthorize` on `createSubject` (line 40):
```java
    @PreAuthorize("hasRole('ADMIN')")
```

Change `@PreAuthorize` on `updateSubject` (line 48):
```java
    @PreAuthorize("hasRole('ADMIN')")
```

Change `@PreAuthorize` on `deleteSubject` (line 57):
```java
    @PreAuthorize("hasRole('ADMIN')")
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/vn/edu/fpt/myfschool/controller/SubjectController.java
git commit -m "refactor(backend): split SubjectController permissions - write to ADMIN only"
```

### Task 11: Update SemesterController permissions

**Files:**
- Modify: `backend/src/main/java/vn/edu/fpt/myfschool/controller/SemesterController.java`

- [ ] **Step 1: Update read endpoints**

Change `@PreAuthorize` on `listSemesters` (line 25):
```java
    @PreAuthorize("hasAnyRole('ADMIN', 'PARENT', 'STUDENT', 'TEACHER')")
```

Change `@PreAuthorize` on `getCurrentSemester` (line 33):
```java
    @PreAuthorize("hasAnyRole('ADMIN', 'PARENT', 'STUDENT', 'TEACHER')")
```

Change `@PreAuthorize` on `getSemester` (line 40):
```java
    @PreAuthorize("hasAnyRole('ADMIN', 'PARENT', 'STUDENT', 'TEACHER')")
```

- [ ] **Step 2: Update write endpoints**

Change `@PreAuthorize` on `createSemester` (line 47):
```java
    @PreAuthorize("hasRole('ADMIN')")
```

Change `@PreAuthorize` on `updateSemester` (line 55):
```java
    @PreAuthorize("hasRole('ADMIN')")
```

Change `@PreAuthorize` on `setCurrentSemester` (line 64):
```java
    @PreAuthorize("hasRole('ADMIN')")
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/vn/edu/fpt/myfschool/controller/SemesterController.java
git commit -m "refactor(backend): split SemesterController permissions - write to ADMIN only"
```

### Task 12: Update ScheduleController permissions

**Files:**
- Modify: `backend/src/main/java/vn/edu/fpt/myfschool/controller/ScheduleController.java`

- [ ] **Step 1: Update read endpoints**

Change `@PreAuthorize` on `getClassSchedule` (line 26):
```java
    @PreAuthorize("hasAnyRole('ADMIN', 'PARENT', 'STUDENT', 'TEACHER')")
```

Change `@PreAuthorize` on `getTeacherSchedule` (line 34):
```java
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
```

Change `@PreAuthorize` on `getAvailablePeriods` (line 58):
```java
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
```

- [ ] **Step 2: Update write endpoints**

Change `@PreAuthorize` on `createSchedule` (line 42):
```java
    @PreAuthorize("hasRole('ADMIN')")
```

Change `@PreAuthorize` on `deleteSchedule` (line 50):
```java
    @PreAuthorize("hasRole('ADMIN')")
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/vn/edu/fpt/myfschool/controller/ScheduleController.java
git commit -m "refactor(backend): split ScheduleController permissions - write to ADMIN only"
```

### Task 13: Update TuitionBillController permissions

**Files:**
- Modify: `backend/src/main/java/vn/edu/fpt/myfschool/controller/TuitionBillController.java`

- [ ] **Step 1: Update write endpoints**

Change `@PreAuthorize` on `createBill` (line 28):
```java
    @PreAuthorize("hasRole('ADMIN')")
```

Change `@PreAuthorize` on `deleteBill` (line 52):
```java
    @PreAuthorize("hasRole('ADMIN')")
```

Change `@PreAuthorize` on `simulatePayment` (line 60):
```java
    @PreAuthorize("hasRole('ADMIN')")
```

- [ ] **Step 2: Update read endpoints**

Change `@PreAuthorize` on `getClassBills` (line 36):
```java
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/vn/edu/fpt/myfschool/controller/TuitionBillController.java
git commit -m "refactor(backend): split TuitionBillController permissions - write to ADMIN only"
```

### Task 14: Update AnnouncementController permissions

**Files:**
- Modify: `backend/src/main/java/vn/edu/fpt/myfschool/controller/AnnouncementController.java`

- [ ] **Step 1: Update write endpoints**

Change `@PreAuthorize` on `createAnnouncement` (line 27):
```java
    @PreAuthorize("hasRole('ADMIN')")
```

Change `@PreAuthorize` on `getMyAnnouncements` (line 38):
```java
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
```

- [ ] **Step 2: Update read endpoints**

Change `@PreAuthorize` on `getAnnouncementDetail` (line 46):
```java
    @PreAuthorize("hasAnyRole('ADMIN', 'PARENT', 'STUDENT', 'TEACHER')")
```

Change `@PreAuthorize` on `getAnnouncements` (line 53):
```java
    @PreAuthorize("hasAnyRole('ADMIN', 'PARENT', 'STUDENT')")
```

Change `@PreAuthorize` on `markAsRead` (line 63):
```java
    @PreAuthorize("hasAnyRole('ADMIN', 'PARENT', 'STUDENT')")
```

Change `@PreAuthorize` on `getUnreadCount` (line 70):
```java
    @PreAuthorize("hasAnyRole('ADMIN', 'PARENT', 'STUDENT')")
```

- [ ] **Step 3: Update AnnouncementServiceImpl for ADMIN role**

In `backend/src/main/java/vn/edu/fpt/myfschool/service/impl/AnnouncementServiceImpl.java`, update `targetRolesFor()` (line 156):

```java
    private List<TargetRole> targetRolesFor(UserRole role) {
        if (role == UserRole.ADMIN) return List.of(TargetRole.PARENT, TargetRole.STUDENT, TargetRole.ALL);
        if (role == UserRole.PARENT) return List.of(TargetRole.PARENT, TargetRole.ALL);
        if (role == UserRole.STUDENT) return List.of(TargetRole.STUDENT, TargetRole.ALL);
        return List.of();
    }
```

Update `getVisibleClassIds()` to handle ADMIN (after line 153):

```java
        if (role == UserRole.ADMIN) {
            // Admin sees all classes — for v1, return empty (admin config via admin web, not announcement inbox)
            return List.of();
        }
```

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/vn/edu/fpt/myfschool/controller/AnnouncementController.java \
       backend/src/main/java/vn/edu/fpt/myfschool/service/impl/AnnouncementServiceImpl.java
git commit -m "refactor(backend): split AnnouncementController permissions - write to ADMIN only"
```

### Task 15: Fix existing tests broken by permission split

**Files:**
- Modify: `backend/src/test/java/vn/edu/fpt/myfschool/ScheduleIntegrationTest.java`
- Modify: `backend/src/test/java/vn/edu/fpt/myfschool/TuitionIntegrationTest.java`

**Why:** Existing tests use teacher tokens to create schedules/bills. After permission split, teacher can't write those. Update tests to use admin tokens for write operations, teacher tokens only for read/business.

- [ ] **Step 1: Update ScheduleIntegrationTest**

In `ScheduleIntegrationTest.java`, change `loginAsTeacher()` to `loginAsAdmin()` for `create_schedule_teacher_only` test (rename to `create_schedule_admin_only`):

```java
    @Test
    void create_schedule_admin_only() throws Exception {
        String token = loginAsAdmin();
        // ... rest unchanged
    }
```

Update `delete_schedule` test to use admin token:

```java
        String token = loginAsAdmin();
```

Update `get_available_periods` test — teacher can still read:

```java
        String token = loginAsAdmin(); // available-periods is now ADMIN+TEACHER read, but needs admin for create first
```

- [ ] **Step 2: Update TuitionIntegrationTest**

Change `loginAsTeacher()` to `loginAsAdmin()` in:
- `create_tuition_bill_teacher_only` (rename to `create_tuition_bill_admin_only`)
- `teacher_view_class_bills` (keep teacher for read, admin for create)
- `simulate_payment`

- [ ] **Step 3: Run all tests**

Run: `cd backend && mvn test -q`
Expected: All tests pass

- [ ] **Step 4: Commit**

```bash
git add backend/src/test/java/vn/edu/fpt/myfschool/ScheduleIntegrationTest.java \
       backend/src/test/java/vn/edu/fpt/myfschool/TuitionIntegrationTest.java
git commit -m "fix(test): update integration tests for admin permission split"
```

---

## Phase 4: Admin Web React App

### Task 16: Scaffold admin-web React app

**Files:**
- Create: `admin-web/package.json`
- Create: `admin-web/index.html`
- Create: `admin-web/vite.config.ts`
- Create: `admin-web/tsconfig.json`
- Create: `admin-web/src/main.tsx`
- Create: `admin-web/src/App.tsx`
- Create: `admin-web/src/api/client.ts`
- Create: `admin-web/src/api/auth.ts`
- Create: `admin-web/src/styles.css`

- [ ] **Step 1: Create package.json**

```json
{
  "name": "admin-web",
  "private": true,
  "version": "1.0.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "tsc && vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "react": "^19.1.0",
    "react-dom": "^19.1.0"
  },
  "devDependencies": {
    "@types/react": "^19.1.8",
    "@types/react-dom": "^19.1.6",
    "@vitejs/plugin-react": "^4.5.2",
    "typescript": "^5.8.3",
    "vite": "^6.3.5"
  }
}
```

- [ ] **Step 2: Create index.html**

```html
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>MyFschool Admin</title>
</head>
<body>
  <div id="root"></div>
  <script type="module" src="/src/main.tsx"></script>
</body>
</html>
```

- [ ] **Step 3: Create vite.config.ts**

```typescript
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: { port: 5173 }
})
```

- [ ] **Step 4: Create tsconfig.json**

```json
{
  "compilerOptions": {
    "target": "ES2020",
    "useDefineForClassFields": true,
    "lib": ["ES2020", "DOM", "DOM.Iterable"],
    "module": "ESNext",
    "skipLibCheck": true,
    "moduleResolution": "bundler",
    "allowImportingTsExtensions": true,
    "isolatedModules": true,
    "moduleDetection": "force",
    "noEmit": true,
    "jsx": "react-jsx",
    "strict": true
  },
  "include": ["src"]
}
```

- [ ] **Step 5: Create src/main.tsx**

```typescript
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import App from './App'
import './styles.css'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>
)
```

- [ ] **Step 6: Create src/api/client.ts**

```typescript
const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

export function getToken(): string | null {
  return localStorage.getItem('admin_token');
}

export function setToken(token: string) {
  localStorage.setItem('admin_token', token);
}

export function clearToken() {
  localStorage.removeItem('admin_token');
}

export async function apiFetch(path: string, options: RequestInit = {}): Promise<any> {
  const token = getToken();
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string> || {}),
  };
  if (token) headers['Authorization'] = `Bearer ${token}`;

  const res = await fetch(`${API_BASE}${path}`, { ...options, headers });

  if (res.status === 401) {
    clearToken();
    window.location.hash = '#/login';
    throw new Error('Unauthorized');
  }

  const json = await res.json();
  if (!json.success) throw new Error(json.message || 'API error');
  return json.data;
}
```

- [ ] **Step 7: Create src/api/auth.ts**

```typescript
import { apiFetch, setToken, clearToken, getToken } from './client';

export interface AdminUser {
  id: number;
  phone: string;
  name: string;
  email: string | null;
  role: string;
  status: string;
  createdAt: string;
}

export interface LoginResponse {
  token: string;
  user: AdminUser;
}

export async function login(phone: string, password: string): Promise<AdminUser> {
  const data: LoginResponse = await apiFetch('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ phone, password }),
  });
  setToken(data.token);
  return data.user;
}

export function isAdminLoggedIn(): boolean {
  return !!getToken();
}

export function logout() {
  clearToken();
}
```

- [ ] **Step 8: Install dependencies**

Run: `cd admin-web && npm install`
Expected: node_modules created

- [ ] **Step 9: Commit**

```bash
git add admin-web/
git commit -m "feat(admin-web): scaffold React app with auth API client"
```

### Task 17: Create LoginPage

**Files:**
- Create: `admin-web/src/pages/LoginPage.tsx`

- [ ] **Step 1: Create LoginPage.tsx**

```tsx
import { useState } from 'react';
import { login } from '../api/auth';

interface Props {
  onLogin: () => void;
}

export default function LoginPage({ onLogin }: Props) {
  const [phone, setPhone] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const user = await login(phone, password);
      if (user.role !== 'ADMIN') {
        setError('Bạn không có quyền truy cập chức năng này.');
        return;
      }
      onLogin();
    } catch (err: any) {
      setError(err.message || 'Đăng nhập thất bại');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="login-container">
      <form onSubmit={handleSubmit} className="login-form">
        <h1>MyFschool Admin</h1>
        <input
          type="text"
          placeholder="Số điện thoại"
          value={phone}
          onChange={e => setPhone(e.target.value)}
          required
        />
        <input
          type="password"
          placeholder="Mật khẩu"
          value={password}
          onChange={e => setPassword(e.target.value)}
          required
        />
        {error && <div className="error">{error}</div>}
        <button type="submit" disabled={loading}>
          {loading ? 'Đang đăng nhập...' : 'Đăng nhập'}
        </button>
      </form>
    </div>
  );
}
```

- [ ] **Step 2: Commit**

```bash
git add admin-web/src/pages/LoginPage.tsx
git commit -m "feat(admin-web): add LoginPage with ADMIN role check"
```

### Task 18: Create UsersPage

**Files:**
- Create: `admin-web/src/pages/UsersPage.tsx`

- [ ] **Step 1: Create UsersPage.tsx**

```tsx
import { useState, useEffect } from 'react';
import { apiFetch } from '../api/client';
import { AdminUser } from '../api/auth';

export default function UsersPage() {
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [role, setRole] = useState('');
  const [status, setStatus] = useState('');
  const [keyword, setKeyword] = useState('');
  const [loading, setLoading] = useState(false);

  async function fetchUsers() {
    setLoading(true);
    try {
      const params = new URLSearchParams();
      if (role) params.set('role', role);
      if (status) params.set('status', status);
      if (keyword) params.set('keyword', keyword);
      const data = await apiFetch(`/admin/users?${params.toString()}`);
      setUsers(data);
    } catch (err: any) {
      alert(err.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { fetchUsers(); }, []);

  async function toggleStatus(user: AdminUser) {
    const newStatus = user.status === 'ACTIVE' ? 'LOCKED' : 'ACTIVE';
    if (!confirm(`${newStatus === 'LOCKED' ? 'Khóa' : 'Mở'} tài khoản ${user.name}?`)) return;
    try {
      await apiFetch(`/admin/users/${user.id}/status`, {
        method: 'PUT',
        body: JSON.stringify({ status: newStatus }),
      });
      fetchUsers();
    } catch (err: any) {
      alert(err.message);
    }
  }

  return (
    <div>
      <h2>Quản lý tài khoản</h2>
      <div className="filters">
        <select value={role} onChange={e => setRole(e.target.value)}>
          <option value="">Tất cả vai trò</option>
          <option value="PARENT">Phụ huynh</option>
          <option value="STUDENT">Học sinh</option>
          <option value="TEACHER">Giáo viên</option>
        </select>
        <select value={status} onChange={e => setStatus(e.target.value)}>
          <option value="">Tất cả trạng thái</option>
          <option value="ACTIVE">Hoạt động</option>
          <option value="LOCKED">Khóa</option>
        </select>
        <input
          type="text"
          placeholder="Tìm theo tên hoặc SĐT..."
          value={keyword}
          onChange={e => setKeyword(e.target.value)}
        />
        <button onClick={fetchUsers} disabled={loading}>Tìm</button>
      </div>
      <table>
        <thead>
          <tr>
            <th>ID</th><th>Tên</th><th>SĐT</th><th>Vai trò</th><th>Trạng thái</th><th>Thao tác</th>
          </tr>
        </thead>
        <tbody>
          {users.map(u => (
            <tr key={u.id}>
              <td>{u.id}</td>
              <td>{u.name}</td>
              <td>{u.phone}</td>
              <td>{u.role}</td>
              <td>{u.status}</td>
              <td>
                <button onClick={() => toggleStatus(u)}>
                  {u.status === 'ACTIVE' ? 'Khóa' : 'Mở'}
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      {loading && <p>Đang tải...</p>}
    </div>
  );
}
```

- [ ] **Step 2: Commit**

```bash
git add admin-web/src/pages/UsersPage.tsx
git commit -m "feat(admin-web): add UsersPage with list/filter/lock/unlock"
```

### Task 19: Create config pages (stub)

**Files:**
- Create: `admin-web/src/pages/ClassesPage.tsx`
- Create: `admin-web/src/pages/SubjectsPage.tsx`
- Create: `admin-web/src/pages/SemestersPage.tsx`
- Create: `admin-web/src/pages/AssignmentsPage.tsx`
- Create: `admin-web/src/pages/SchedulesPage.tsx`
- Create: `admin-web/src/pages/TuitionPage.tsx`
- Create: `admin-web/src/pages/AnnouncementsPage.tsx`

**Why:** Stub pages with table + CRUD buttons. Each page fetches from its API endpoint. Full CRUD implementation follows the same pattern as UsersPage.

- [ ] **Step 1: Create ClassesPage.tsx**

```tsx
import { useState, useEffect } from 'react';
import { apiFetch } from '../api/client';

interface ClassItem { id: number; name: string; gradeLevel: number; academicYear: string; }

export default function ClassesPage() {
  const [items, setItems] = useState<ClassItem[]>([]);
  const [name, setName] = useState('');
  const [gradeLevel, setGradeLevel] = useState(10);
  const [academicYear, setAcademicYear] = useState('2026-2027');
  const [schoolName, setSchoolName] = useState('FPT Schools');

  async function fetchItems() {
    try {
      const data = await apiFetch('/classes?page=0&size=100');
      setItems(data.content || []);
    } catch (err: any) { alert(err.message); }
  }

  useEffect(() => { fetchItems(); }, []);

  async function createItem() {
    try {
      await apiFetch('/classes', {
        method: 'POST',
        body: JSON.stringify({ name, gradeLevel, academicYear, schoolName }),
      });
      setName('');
      fetchItems();
    } catch (err: any) { alert(err.message); }
  }

  async function deleteItem(id: number) {
    if (!confirm('Xóa lớp này?')) return;
    try {
      await apiFetch(`/classes/${id}`, { method: 'DELETE' });
      fetchItems();
    } catch (err: any) { alert(err.message); }
  }

  return (
    <div>
      <h2>Quản lý lớp học</h2>
      <div className="form-inline">
        <input placeholder="Tên lớp" value={name} onChange={e => setName(e.target.value)} />
        <input type="number" placeholder="Khối" value={gradeLevel} onChange={e => setGradeLevel(+e.target.value)} />
        <input placeholder="Năm học" value={academicYear} onChange={e => setAcademicYear(e.target.value)} />
        <button onClick={createItem}>Tạo lớp</button>
      </div>
      <table>
        <thead><tr><th>ID</th><th>Tên</th><th>Khối</th><th>Năm học</th><th></th></tr></thead>
        <tbody>
          {items.map(c => (
            <tr key={c.id}>
              <td>{c.id}</td><td>{c.name}</td><td>{c.gradeLevel}</td><td>{c.academicYear}</td>
              <td><button onClick={() => deleteItem(c.id)}>Xóa</button></td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
```

- [ ] **Step 2: Create SubjectsPage.tsx**

```tsx
import { useState, useEffect } from 'react';
import { apiFetch } from '../api/client';

interface Subject { id: number; name: string; code: string; }

export default function SubjectsPage() {
  const [items, setItems] = useState<Subject[]>([]);
  const [name, setName] = useState('');
  const [code, setCode] = useState('');

  async function fetchItems() {
    try { setItems(await apiFetch('/subjects')); } catch (err: any) { alert(err.message); }
  }
  useEffect(() => { fetchItems(); }, []);

  async function createItem() {
    try {
      await apiFetch('/subjects', { method: 'POST', body: JSON.stringify({ name, code }) });
      setName(''); setCode(''); fetchItems();
    } catch (err: any) { alert(err.message); }
  }

  async function deleteItem(id: number) {
    if (!confirm('Xóa môn học?')) return;
    try { await apiFetch(`/subjects/${id}`, { method: 'DELETE' }); fetchItems(); } catch (err: any) { alert(err.message); }
  }

  return (
    <div>
      <h2>Quản lý môn học</h2>
      <div className="form-inline">
        <input placeholder="Tên môn" value={name} onChange={e => setName(e.target.value)} />
        <input placeholder="Mã môn" value={code} onChange={e => setCode(e.target.value)} />
        <button onClick={createItem}>Tạo môn</button>
      </div>
      <table>
        <thead><tr><th>ID</th><th>Tên</th><th>Mã</th><th></th></tr></thead>
        <tbody>
          {items.map(s => (
            <tr key={s.id}><td>{s.id}</td><td>{s.name}</td><td>{s.code}</td>
              <td><button onClick={() => deleteItem(s.id)}>Xóa</button></td></tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
```

- [ ] **Step 3: Create SemestersPage.tsx**

```tsx
import { useState, useEffect } from 'react';
import { apiFetch } from '../api/client';

interface Semester { id: number; name: string; academicYear: string; startDate: string; endDate: string; isCurrent: boolean; }

export default function SemestersPage() {
  const [items, setItems] = useState<Semester[]>([]);
  const [name, setName] = useState('');
  const [academicYear, setAcademicYear] = useState('2026-2027');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');

  async function fetchItems() {
    try { setItems(await apiFetch('/semesters')); } catch (err: any) { alert(err.message); }
  }
  useEffect(() => { fetchItems(); }, []);

  async function createItem() {
    try {
      await apiFetch('/semesters', { method: 'POST', body: JSON.stringify({ name, academicYear, startDate, endDate }) });
      setName(''); fetchItems();
    } catch (err: any) { alert(err.message); }
  }

  async function setCurrent(id: number) {
    try { await apiFetch(`/semesters/${id}/set-current`, { method: 'PUT' }); fetchItems(); } catch (err: any) { alert(err.message); }
  }

  return (
    <div>
      <h2>Quản lý học kỳ</h2>
      <div className="form-inline">
        <input placeholder="Tên học kỳ" value={name} onChange={e => setName(e.target.value)} />
        <input type="date" value={startDate} onChange={e => setStartDate(e.target.value)} />
        <input type="date" value={endDate} onChange={e => setEndDate(e.target.value)} />
        <button onClick={createItem}>Tạo học kỳ</button>
      </div>
      <table>
        <thead><tr><th>ID</th><th>Tên</th><th>Năm học</th><th>Hiện tại</th><th></th></tr></thead>
        <tbody>
          {items.map(s => (
            <tr key={s.id}><td>{s.id}</td><td>{s.name}</td><td>{s.academicYear}</td><td>{s.isCurrent ? '✓' : ''}</td>
              <td>{!s.isCurrent && <button onClick={() => setCurrent(s.id)}>Đặt hiện tại</button>}</td></tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
```

- [ ] **Step 4: Create AssignmentsPage.tsx**

```tsx
import { useState, useEffect } from 'react';
import { apiFetch } from '../api/client';

export default function AssignmentsPage() {
  const [classes, setClasses] = useState<any[]>([]);
  const [subjects, setSubjects] = useState<any[]>([]);
  const [teachers, setTeachers] = useState<any[]>([]);
  const [classId, setClassId] = useState('');
  const [subjectId, setSubjectId] = useState('');
  const [teacherId, setTeacherId] = useState('');
  const [academicYear, setAcademicYear] = useState('2026-2027');

  useEffect(() => {
    apiFetch('/classes?page=0&size=100').then(d => setClasses(d.content || [])).catch(() => {});
    apiFetch('/subjects').then(setSubjects).catch(() => {});
  }, []);

  async function assign() {
    try {
      await apiFetch(`/classes/${classId}/subjects`, {
        method: 'POST',
        body: JSON.stringify({ classId: +classId, subjectId: +subjectId, teacherId: +teacherId, academicYear }),
      });
      alert('Phân công thành công');
    } catch (err: any) { alert(err.message); }
  }

  return (
    <div>
      <h2>Phân công giáo viên</h2>
      <div className="form-inline">
        <select value={classId} onChange={e => setClassId(e.target.value)}>
          <option value="">Chọn lớp</option>
          {classes.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
        </select>
        <select value={subjectId} onChange={e => setSubjectId(e.target.value)}>
          <option value="">Chọn môn</option>
          {subjects.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
        </select>
        <input placeholder="Teacher ID" type="number" value={teacherId} onChange={e => setTeacherId(e.target.value)} />
        <button onClick={assign}>Phân công</button>
      </div>
    </div>
  );
}
```

- [ ] **Step 5: Create SchedulesPage.tsx, TuitionPage.tsx, AnnouncementsPage.tsx**

Follow same pattern: fetch list, create form, table display. Each uses its respective API endpoint (`/schedules`, `/tuition/bills`, `/announcements`). Implementation mirrors UsersPage/ClassesPage patterns.

- [ ] **Step 6: Commit**

```bash
git add admin-web/src/pages/
git commit -m "feat(admin-web): add academic config pages (classes, subjects, semesters, assignments)"
```

### Task 20: Create App.tsx with routing and styles

**Files:**
- Create: `admin-web/src/App.tsx`
- Create: `admin-web/src/styles.css`

- [ ] **Step 1: Create App.tsx**

```tsx
import { useState, useEffect } from 'react';
import { isAdminLoggedIn, logout } from './api/auth';
import LoginPage from './pages/LoginPage';
import UsersPage from './pages/UsersPage';
import ClassesPage from './pages/ClassesPage';
import SubjectsPage from './pages/SubjectsPage';
import SemestersPage from './pages/SemestersPage';
import AssignmentsPage from './pages/AssignmentsPage';

const MENU = [
  { key: 'users', label: 'Tài khoản' },
  { key: 'classes', label: 'Lớp học' },
  { key: 'subjects', label: 'Môn học' },
  { key: 'semesters', label: 'Học kỳ' },
  { key: 'assignments', label: 'Phân công GV' },
];

export default function App() {
  const [loggedIn, setLoggedIn] = useState(isAdminLoggedIn());
  const [page, setPage] = useState('users');

  useEffect(() => { setLoggedIn(isAdminLoggedIn()); }, []);

  if (!loggedIn) {
    return <LoginPage onLogin={() => setLoggedIn(true)} />;
  }

  function handleLogout() {
    logout();
    setLoggedIn(false);
  }

  const pages: Record<string, JSX.Element> = {
    users: <UsersPage />,
    classes: <ClassesPage />,
    subjects: <SubjectsPage />,
    semesters: <SemestersPage />,
    assignments: <AssignmentsPage />,
  };

  return (
    <div className="app">
      <nav className="sidebar">
        <h2>MyFschool Admin</h2>
        {MENU.map(m => (
          <button
            key={m.key}
            className={page === m.key ? 'active' : ''}
            onClick={() => setPage(m.key)}
          >
            {m.label}
          </button>
        ))}
        <button className="logout" onClick={handleLogout}>Đăng xuất</button>
      </nav>
      <main>{pages[page] || <UsersPage />}</main>
    </div>
  );
}
```

- [ ] **Step 2: Create styles.css**

```css
* { margin: 0; padding: 0; box-sizing: border-box; }
body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; background: #f5f5f5; }

.app { display: flex; height: 100vh; }
.sidebar { width: 220px; background: #1a1a2e; color: #fff; padding: 20px; display: flex; flex-direction: column; gap: 8px; }
.sidebar h2 { font-size: 16px; margin-bottom: 16px; }
.sidebar button { background: none; border: none; color: #ccc; text-align: left; padding: 10px 12px; border-radius: 6px; cursor: pointer; font-size: 14px; }
.sidebar button:hover { background: rgba(255,255,255,0.1); }
.sidebar button.active { background: rgba(255,255,255,0.15); color: #fff; font-weight: 600; }
.sidebar button.logout { margin-top: auto; color: #f66; }

main { flex: 1; padding: 24px; overflow-y: auto; }
h2 { margin-bottom: 16px; }

.filters { display: flex; gap: 8px; margin-bottom: 16px; flex-wrap: wrap; }
.filters select, .filters input { padding: 8px; border: 1px solid #ddd; border-radius: 6px; }
.filters button { padding: 8px 16px; background: #1a1a2e; color: #fff; border: none; border-radius: 6px; cursor: pointer; }

.form-inline { display: flex; gap: 8px; margin-bottom: 16px; flex-wrap: wrap; }
.form-inline input, .form-inline select { padding: 8px; border: 1px solid #ddd; border-radius: 6px; }
.form-inline button { padding: 8px 16px; background: #2563eb; color: #fff; border: none; border-radius: 6px; cursor: pointer; }

table { width: 100%; border-collapse: collapse; background: #fff; border-radius: 8px; overflow: hidden; }
th, td { padding: 10px 12px; text-align: left; border-bottom: 1px solid #eee; font-size: 14px; }
th { background: #f8f8f8; font-weight: 600; }
button { cursor: pointer; }
button.danger { color: #dc2626; }

.error { color: #dc2626; margin-bottom: 12px; }

.login-container { display: flex; justify-content: center; align-items: center; height: 100vh; }
.login-form { background: #fff; padding: 40px; border-radius: 12px; box-shadow: 0 2px 12px rgba(0,0,0,0.1); width: 360px; }
.login-form h1 { text-align: center; margin-bottom: 24px; font-size: 20px; }
.login-form input { width: 100%; padding: 10px; margin-bottom: 12px; border: 1px solid #ddd; border-radius: 6px; font-size: 14px; }
.login-form button { width: 100%; padding: 10px; background: #1a1a2e; color: #fff; border: none; border-radius: 6px; font-size: 14px; }
```

- [ ] **Step 3: Verify build**

Run: `cd admin-web && npm run build`
Expected: Build succeeds, `dist/` created

- [ ] **Step 4: Commit**

```bash
git add admin-web/src/App.tsx admin-web/src/styles.css
git commit -m "feat(admin-web): add App with sidebar routing and CSS styles"
```

---

## Phase 5: Test Documentation

### Task 21: Update UC09 test case doc

**Files:**
- Modify: `06-Testing/TEST_CASES/UC09_AdminRole_TESTCASE.md`

- [ ] **Step 1: Update test execution log**

In section 10 (Build & Test Execution Log), update the commands to reflect actual execution:

```markdown
| 2026-07-08 | Claude | `cd backend && mvn test` | Pass | All integration tests pass |
| 2026-07-08 | Claude | `cd admin-web && npm run build` | Pass | React build succeeds |
```

- [ ] **Step 2: Update test data to match implementation**

Update `TD-ADMIN-001` phone to `0909000009` (matching BaseIntegrationTest seed).

- [ ] **Step 3: Commit**

```bash
git add 06-Testing/TEST_CASES/UC09_AdminRole_TESTCASE.md
git commit -m "docs(test): update UC09 test case with implementation details"
```

### Task 22: Update TESTCASE_INDEX.md

**Files:**
- Modify: `06-Testing/TEST_CASES/TESTCASE_INDEX.md`

- [ ] **Step 1: Update UC09 row status**

Change UC09 row from `Draft` to `Ready`:

```markdown
| **UC09** | **ADMIN** | **Admin Role & Admin Web Configuration** | **`UC09_AdminRole_TESTCASE.md`** | **Claude** | **Ready** | **2026-07-08** | **Backend: ADMIN role added, user management API, permission split. Admin web: React SPA with login, user management, academic config pages.** |
```

- [ ] **Step 2: Commit**

```bash
git add 06-Testing/TEST_CASES/TESTCASE_INDEX.md
git commit -m "docs(test): update TESTCASE_INDEX with UC09 status Ready"
```

---

## Verification

After all phases complete:

### Backend

```bash
cd backend && mvn test
```

Expected: All tests pass including `AdminRoleIntegrationTest`.

### Admin Web

```bash
cd admin-web && npm run build
```

Expected: Build succeeds.

### Manual Smoke

1. Start backend: `cd backend && mvn spring-boot:run`
2. Start admin web: `cd admin-web && npm run dev`
3. Open `http://localhost:5173`
4. Login with admin credentials
5. Navigate to Users page — verify user list loads
6. Lock/unlock a test user
7. Navigate to Classes — create/delete a class
8. Try login with teacher account — verify blocked with error message
