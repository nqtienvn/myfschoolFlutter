package vn.edu.fpt.myfschool.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.fpt.myfschool.common.dto.AdminUserDto;
import vn.edu.fpt.myfschool.common.dto.ApiResponse;
import vn.edu.fpt.myfschool.common.dto.CreateTeacherAccountRequest;
import vn.edu.fpt.myfschool.common.dto.TeacherAccountCredentialDto;
import vn.edu.fpt.myfschool.common.dto.TeacherManagementSummaryDto;
import vn.edu.fpt.myfschool.common.dto.TeacherSummaryDto;
import vn.edu.fpt.myfschool.common.dto.UpdateTeacherProfileRequest;
import vn.edu.fpt.myfschool.common.dto.UpdateTeacherSubjectsRequest;
import vn.edu.fpt.myfschool.common.dto.UpdateUserStatusRequest;
import vn.edu.fpt.myfschool.common.enums.UserRole;
import vn.edu.fpt.myfschool.common.enums.UserStatus;
import vn.edu.fpt.myfschool.service.AdminUserService;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin Users", description = "Quản lý tài khoản (Admin only)")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Danh sách tài khoản")
    public ResponseEntity<ApiResponse<Page<AdminUserDto>>> listUsers(
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                adminUserService.listUsers(role, status, keyword, page, size)));
    }

    @GetMapping("/teachers")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Danh sách giáo viên")
    public ResponseEntity<ApiResponse<Page<TeacherSummaryDto>>> listTeachers(
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Long academicYearId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                adminUserService.listTeachers(status, keyword, subjectId, academicYearId, page, size)));
    }

    @GetMapping("/teachers/summary")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Thống kê quản lý giáo viên")
    public ResponseEntity<ApiResponse<TeacherManagementSummaryDto>> getTeacherSummary(
            @RequestParam(required = false) Long academicYearId) {
        return ResponseEntity.ok(ApiResponse.success(
                adminUserService.getTeacherSummary(academicYearId)));
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

    @PostMapping("/teachers")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tạo tài khoản giáo viên")
    public ResponseEntity<ApiResponse<TeacherAccountCredentialDto>> createTeacherAccount(
            @Valid @RequestBody CreateTeacherAccountRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Tạo tài khoản giáo viên thành công",
                adminUserService.createTeacherAccount(request)));
    }

    @PutMapping("/teachers/{teacherId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cập nhật thông tin giáo viên")
    public ResponseEntity<ApiResponse<TeacherSummaryDto>> updateTeacherProfile(
            @PathVariable Long teacherId,
            @RequestParam(required = false) Long academicYearId,
            @Valid @RequestBody UpdateTeacherProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Cập nhật thông tin giáo viên thành công",
                adminUserService.updateTeacherProfile(teacherId, request, academicYearId)));
    }

    @PutMapping("/teachers/{teacherId}/subjects")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cập nhật môn phụ trách của giáo viên")
    public ResponseEntity<ApiResponse<TeacherSummaryDto>> updateTeacherSubjects(
            @PathVariable Long teacherId,
            @Valid @RequestBody UpdateTeacherSubjectsRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Cập nhật môn phụ trách thành công",
                adminUserService.updateTeacherSubjects(teacherId, request)));
    }

}
