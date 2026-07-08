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
import vn.edu.fpt.myfschool.service.HomeroomAssignmentService;

import java.util.List;

@RestController
@RequestMapping("/api/homeroom-assignments")
@RequiredArgsConstructor
@Tag(name = "Homeroom Assignments", description = "Phân công GVCN")
@SecurityRequirement(name = "Bearer Authentication")
public class HomeroomAssignmentController {

    private final HomeroomAssignmentService homeroomAssignmentService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "DS GVCN")
    public ResponseEntity<ApiResponse<List<HomeroomAssignmentDto>>> list(
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long academicYearId) {
        return ResponseEntity.ok(ApiResponse.success(
            homeroomAssignmentService.listByClass(classId, academicYearId)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Chi tiết phân công GVCN")
    public ResponseEntity<ApiResponse<HomeroomAssignmentDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(homeroomAssignmentService.getById(id)));
    }

    @GetMapping("/current")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "GVCN hiện tại của lớp")
    public ResponseEntity<ApiResponse<HomeroomAssignmentDto>> getCurrent(
            @RequestParam Long classId, @RequestParam Long academicYearId) {
        return ResponseEntity.ok(ApiResponse.success(
            homeroomAssignmentService.getByClassAndYear(classId, academicYearId)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Gán GVCN")
    public ResponseEntity<ApiResponse<HomeroomAssignmentDto>> create(
            @Valid @RequestBody CreateHomeroomAssignmentRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
            "Gán GVCN thành công", homeroomAssignmentService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Đổi GVCN")
    public ResponseEntity<ApiResponse<HomeroomAssignmentDto>> update(
            @PathVariable Long id, @Valid @RequestBody CreateHomeroomAssignmentRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
            "Cập nhật thành công", homeroomAssignmentService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa phân công GVCN")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        homeroomAssignmentService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa thành công", null));
    }
}