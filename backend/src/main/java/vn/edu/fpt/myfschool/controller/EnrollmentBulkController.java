package vn.edu.fpt.myfschool.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.fpt.myfschool.common.dto.ApiResponse;
import vn.edu.fpt.myfschool.common.dto.PromoteResponse;
import vn.edu.fpt.myfschool.service.EnrollmentPromotionService;

@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
@Tag(name = "Enrollment Bulk", description = "Xử lý hàng loạt đăng ký học")
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
