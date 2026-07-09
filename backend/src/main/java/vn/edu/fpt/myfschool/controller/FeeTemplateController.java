package vn.edu.fpt.myfschool.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.fpt.myfschool.common.dto.ApiResponse;
import vn.edu.fpt.myfschool.common.dto.CreateFeeTemplateRequest;
import vn.edu.fpt.myfschool.common.dto.FeeTemplateDto;
import vn.edu.fpt.myfschool.common.dto.GenerateBillResultDto;
import vn.edu.fpt.myfschool.service.FeeTemplateService;

import java.util.List;

@RestController
@RequestMapping("/api/fee-templates")
@RequiredArgsConstructor
@Tag(name = "Fee Templates", description = "Cau hinh khoan phi")
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