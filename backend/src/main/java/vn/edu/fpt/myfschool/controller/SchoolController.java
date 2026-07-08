package vn.edu.fpt.myfschool.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.myfschool.common.dto.ApiResponse;
import vn.edu.fpt.myfschool.common.dto.CreateSchoolRequest;
import vn.edu.fpt.myfschool.common.dto.SchoolDto;
import vn.edu.fpt.myfschool.service.SchoolService;

import java.util.List;

@RestController
@RequestMapping("/api/schools")
@RequiredArgsConstructor
@Tag(name = "Schools", description = "Quản lý cơ sở trường học")
@SecurityRequirement(name = "Bearer Authentication")
public class SchoolController {

    private final SchoolService schoolService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT', 'PARENT')")
    @Operation(summary = "Lấy danh sách các cơ sở trường học")
    public ResponseEntity<ApiResponse<List<SchoolDto>>> listSchools() {
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách cơ sở thành công", schoolService.listSchools()));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Thêm cơ sở trường học mới (Admin only)")
    public ResponseEntity<ApiResponse<SchoolDto>> createSchool(
            @Valid @RequestBody CreateSchoolRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Thêm cơ sở thành công", schoolService.createSchool(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cập nhật thông tin cơ sở (Admin only)")
    public ResponseEntity<ApiResponse<SchoolDto>> updateSchool(
            @PathVariable Long id,
            @Valid @RequestBody CreateSchoolRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật cơ sở thành công", schoolService.updateSchool(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa cơ sở (Admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteSchool(@PathVariable Long id) {
        schoolService.deleteSchool(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa cơ sở thành công", null));
    }
}
