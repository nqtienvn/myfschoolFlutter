package vn.edu.fpt.myfschool.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.myfschool.common.dto.ApiResponse;
import vn.edu.fpt.myfschool.common.dto.PromoteResponse;
import vn.edu.fpt.myfschool.service.EnrollmentPromotionService;

@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
@Tag(name = "Enrollment Bulk", description = "Xử lý hàng loạt đăng ký học")
@SecurityRequirement(name = "Bearer Authentication")
public class EnrollmentBulkController {

    private final EnrollmentPromotionService promotionService;

    @PostMapping("/promote")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Promote học sinh lên lớp trên")
    public ResponseEntity<ApiResponse<PromoteResponse>> promote(
            @RequestParam Long fromAcademicYearId,
            @RequestParam Long toAcademicYearId) {
        PromoteResponse response = promotionService.promoteAll(fromAcademicYearId, toAcademicYearId);
        return ResponseEntity.ok(ApiResponse.success("Promote học sinh thành công", response));
    }
}
