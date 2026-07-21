package vn.edu.fpt.myfschool.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.fpt.myfschool.common.dto.ApiResponse;
import vn.edu.fpt.myfschool.common.dto.LoginRequest;
import vn.edu.fpt.myfschool.common.dto.LoginResponse;
import vn.edu.fpt.myfschool.common.dto.RegisterRequest;
import vn.edu.fpt.myfschool.common.dto.PasswordResetRequest;
import vn.edu.fpt.myfschool.common.dto.PasswordResetValidateRequest;
import vn.edu.fpt.myfschool.common.dto.PasswordResetConfirmRequest;
import vn.edu.fpt.myfschool.common.dto.PasswordResetValidationResponse;
import vn.edu.fpt.myfschool.config.PasswordResetProperties;
import vn.edu.fpt.myfschool.service.AuthService;
import vn.edu.fpt.myfschool.service.PasswordResetService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Xác thực & Quản lý tài khoản")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final PasswordResetProperties passwordResetProperties;

    @PostMapping("/login")
    @Operation(summary = "Đăng nhập")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", response));
    }

    @PostMapping("/register")
    @Operation(summary = "Đăng ký tài khoản")
    public ResponseEntity<ApiResponse<LoginResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        LoginResponse response = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success("Đăng ký thành công", response));
    }

    @PostMapping("/password-reset/request")
    @Operation(summary = "Yêu cầu link đặt lại mật khẩu")
    public ResponseEntity<ApiResponse<Void>> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request,
            HttpServletRequest httpRequest) {
        long startedAt = System.nanoTime();
        passwordResetService.request(request.phone(), httpRequest.getRemoteAddr());
        padPublicResponse(startedAt);
        return ResponseEntity.ok(ApiResponse.success(
                "Nếu tài khoản đủ điều kiện, hướng dẫn đặt lại mật khẩu đã được gửi qua email.", null));
    }

    @PostMapping("/password-reset/validate")
    @Operation(summary = "Kiểm tra link đặt lại mật khẩu")
    public ResponseEntity<ApiResponse<PasswordResetValidationResponse>> validatePasswordReset(
            @Valid @RequestBody PasswordResetValidateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(passwordResetService.validate(request.token())));
    }

    @PostMapping("/password-reset/confirm")
    @Operation(summary = "Đặt mật khẩu mới")
    public ResponseEntity<ApiResponse<Void>> confirmPasswordReset(
            @Valid @RequestBody PasswordResetConfirmRequest request) {
        passwordResetService.confirm(request);
        return ResponseEntity.ok(ApiResponse.success("Đặt lại mật khẩu thành công", null));
    }

    private void padPublicResponse(long startedAt) {
        long minimumNanos = passwordResetProperties.getMinimumResponseMillis() * 1_000_000L;
        long remaining = minimumNanos - (System.nanoTime() - startedAt);
        if (remaining <= 0) return;
        try {
            Thread.sleep(remaining / 1_000_000L, (int) (remaining % 1_000_000L));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
