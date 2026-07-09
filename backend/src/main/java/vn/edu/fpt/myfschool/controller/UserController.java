package vn.edu.fpt.myfschool.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.fpt.myfschool.common.dto.ApiResponse;
import vn.edu.fpt.myfschool.common.dto.ChangePasswordRequest;
import vn.edu.fpt.myfschool.common.dto.UpdateProfileRequest;
import vn.edu.fpt.myfschool.common.dto.UpdateSettingsRequest;
import vn.edu.fpt.myfschool.common.dto.UserDto;
import vn.edu.fpt.myfschool.common.dto.UserSettingDto;
import vn.edu.fpt.myfschool.common.util.SecurityUtil;
import vn.edu.fpt.myfschool.service.AuthService;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "User", description = "Quản lý hồ sơ cá nhân")
public class UserController {

    private final AuthService authService;

    @GetMapping("/profile")
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT', 'TEACHER')")
    @Operation(summary = "Xem hồ sơ cá nhân")
    public ResponseEntity<ApiResponse<UserDto>> getProfile() {
        Long userId = SecurityUtil.getCurrentUserId();
        UserDto profile = authService.getProfile(userId);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PutMapping("/profile")
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT', 'TEACHER')")
    @Operation(summary = "Cập nhật hồ sơ cá nhân")
    public ResponseEntity<ApiResponse<UserDto>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        UserDto profile = authService.updateProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thành công", profile));
    }

    @PutMapping("/password")
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT', 'TEACHER')")
    @Operation(summary = "Đổi mật khẩu")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        authService.changePassword(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Đổi mật khẩu thành công", null));
    }

    @GetMapping("/settings")
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT', 'TEACHER')")
    @Operation(summary = "Xem cài đặt")
    public ResponseEntity<ApiResponse<UserSettingDto>> getSettings() {
        Long userId = SecurityUtil.getCurrentUserId();
        UserDto profile = authService.getProfile(userId);
        return ResponseEntity.ok(ApiResponse.success(profile.settings()));
    }

    @PutMapping("/settings")
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT', 'TEACHER')")
    @Operation(summary = "Cập nhật cài đặt")
    public ResponseEntity<ApiResponse<UserSettingDto>> updateSettings(
            @Valid @RequestBody UpdateSettingsRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        UserSettingDto settings = authService.updateSettings(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật cài đặt thành công", settings));
    }
}
