package vn.edu.fpt.myfschool.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.fpt.myfschool.common.dto.ApiResponse;
import vn.edu.fpt.myfschool.common.dto.PaymentConfigurationDto;
import vn.edu.fpt.myfschool.common.dto.PaymentConfigurationRequest;
import vn.edu.fpt.myfschool.service.PaymentConfigurationService;

@RestController
@RequestMapping("/api/payment-configurations")
@RequiredArgsConstructor
@Tag(name = "Payment configuration", description = "Cấu hình thanh toán theo năm học")
public class PaymentConfigurationController {

    private final PaymentConfigurationService paymentConfigurationService;

    @GetMapping("/academic-years/{academicYearId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lấy cấu hình thanh toán của năm học")
    public ResponseEntity<ApiResponse<PaymentConfigurationDto>> getByAcademicYear(
            @PathVariable Long academicYearId) {
        return ResponseEntity.ok(ApiResponse.success(
            paymentConfigurationService.getByAcademicYear(academicYearId)));
    }

    @PutMapping("/academic-years/{academicYearId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tạo hoặc cập nhật cấu hình thanh toán của năm học")
    public ResponseEntity<ApiResponse<PaymentConfigurationDto>> upsert(
            @PathVariable Long academicYearId,
            @Valid @RequestBody PaymentConfigurationRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
            "Đã lưu cấu hình thanh toán",
            paymentConfigurationService.upsert(academicYearId, request)));
    }

    @GetMapping("/semesters/{semesterId}")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "Phụ huynh lấy hướng dẫn chuyển khoản theo học kỳ")
    public ResponseEntity<ApiResponse<PaymentConfigurationDto>> getBySemester(
            @PathVariable Long semesterId) {
        return ResponseEntity.ok(ApiResponse.success(
            paymentConfigurationService.getBySemester(semesterId)));
    }
}
