package vn.edu.fpt.myfschool.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.myfschool.common.dto.ApiResponse;
import vn.edu.fpt.myfschool.common.util.SecurityUtil;
import vn.edu.fpt.myfschool.entity.ClubRegistration;
import vn.edu.fpt.myfschool.service.ClubRegistrationService;

import java.util.List;

@RestController
@RequestMapping("/api/clubs")
@RequiredArgsConstructor
@Tag(name = "Clubs", description = "Đăng ký câu lạc bộ")
@SecurityRequirement(name = "Bearer Authentication")
public class ClubRegistrationController {

    private final ClubRegistrationService clubRegistrationService;

    @PostMapping("/register")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Đăng ký CLB")
    public ResponseEntity<ApiResponse<ClubRegistration>> register(
            @RequestParam String clubName, @RequestParam String academicYear) {
        return ResponseEntity.ok(ApiResponse.success("Đăng ký thành công",
            clubRegistrationService.register(clubName, academicYear, SecurityUtil.getCurrentUserId())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Hủy đăng ký")
    public ResponseEntity<ApiResponse<Void>> cancel(@PathVariable Long id) {
        clubRegistrationService.cancel(id, SecurityUtil.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success("Hủy thành công", null));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "CLB của tôi")
    public ResponseEntity<ApiResponse<List<ClubRegistration>>> getMyRegistrations(
            @RequestParam String academicYear) {
        return ResponseEntity.ok(ApiResponse.success(
            clubRegistrationService.getMyRegistrations(SecurityUtil.getCurrentUserId(), academicYear)));
    }
}
