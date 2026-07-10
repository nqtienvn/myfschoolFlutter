package vn.edu.fpt.myfschool.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.service.AcademicYearMasterDataService;

@RestController
@RequestMapping("/api/academic-years/{academicYearId}/master-data")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AcademicYearMasterDataController {
    private final AcademicYearMasterDataService service;
    @GetMapping public ResponseEntity<ApiResponse<AcademicYearMasterDataConfigDto>> get(@PathVariable Long academicYearId) { return ResponseEntity.ok(ApiResponse.success(service.get(academicYearId))); }
    @PutMapping public ResponseEntity<ApiResponse<AcademicYearMasterDataConfigDto>> update(@PathVariable Long academicYearId, @Valid @RequestBody UpdateAcademicYearMasterDataRequest request) { return ResponseEntity.ok(ApiResponse.success("Đã lưu danh mục áp dụng", service.update(academicYearId, request))); }
}
