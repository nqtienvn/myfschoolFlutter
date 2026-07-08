package vn.edu.fpt.myfschool.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.myfschool.common.dto.AdminUserDto;
import vn.edu.fpt.myfschool.common.dto.ApiResponse;
import vn.edu.fpt.myfschool.common.dto.UpdateUserStatusRequest;
import vn.edu.fpt.myfschool.common.enums.UserRole;
import vn.edu.fpt.myfschool.common.enums.UserStatus;
import vn.edu.fpt.myfschool.service.AdminUserService;

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
    public ResponseEntity<ApiResponse<Page<AdminUserDto>>> listUsers(
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
            adminUserService.listUsers(role, status, keyword, page, size)));
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
