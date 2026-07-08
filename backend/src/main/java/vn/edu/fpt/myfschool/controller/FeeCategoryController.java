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
import vn.edu.fpt.myfschool.service.FeeCategoryService;

import java.util.List;

@RestController
@RequestMapping("/api/fee-categories")
@RequiredArgsConstructor
@Tag(name = "Fee Categories", description = "Danh muc loai phi")
@SecurityRequirement(name = "Bearer Authentication")
public class FeeCategoryController {

    private final FeeCategoryService feeCategoryService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PARENT', 'STUDENT', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<FeeCategoryDto>>> list() {
        return ResponseEntity.ok(ApiResponse.success(feeCategoryService.list()));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FeeCategoryDto>> create(
            @Valid @RequestBody CreateFeeCategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Tao danh muc thanh cong", feeCategoryService.create(request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        feeCategoryService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Xoa thanh cong", null));
    }
}