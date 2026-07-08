package vn.edu.fpt.myfschool.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.service.FeeTemplateService;

import java.util.List;

@RestController
@RequestMapping("/api/fee-templates")
@RequiredArgsConstructor
@Tag(name = "Fee Templates", description = "Cau hinh khoan phi")
@SecurityRequirement(name = "Bearer Authentication")
public class FeeTemplateController {

    private final FeeTemplateService feeTemplateService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<FeeTemplateDto>>> list(
            @RequestParam Long classId, @RequestParam Long semesterId) {
        return ResponseEntity.ok(ApiResponse.success(feeTemplateService.listByClass(classId, semesterId)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FeeTemplateDto>> create(
            @Valid @RequestBody CreateFeeTemplateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Tao template thanh cong", feeTemplateService.create(request)));
    }

    @PostMapping("/{id}/generate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Sinh hoc phi hang loat tu template")
    public ResponseEntity<ApiResponse<GenerateBillResultDto>> generate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
            "Sinh hoc phi thanh cong", feeTemplateService.generateBills(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        feeTemplateService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Xoa thanh cong", null));
    }
}