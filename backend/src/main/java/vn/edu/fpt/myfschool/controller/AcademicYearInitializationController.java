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
import vn.edu.fpt.myfschool.common.dto.InitializeAcademicYearRequest;
import vn.edu.fpt.myfschool.common.dto.InitializeAcademicYearResponse;
import vn.edu.fpt.myfschool.service.AcademicYearInitializationService;

@RestController
@RequestMapping("/api/academic-years")
@RequiredArgsConstructor
@Tag(name = "Academic Year Init", description = "Khởi tạo năm học")
@SecurityRequirement(name = "Bearer Authentication")
public class AcademicYearInitializationController {

    private final AcademicYearInitializationService initializationService;

    @PostMapping("/{id}/initialize")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Khởi tạo năm học từ năm trước")
    public ResponseEntity<ApiResponse<InitializeAcademicYearResponse>> initialize(
            @PathVariable Long id,
            @Valid @RequestBody InitializeAcademicYearRequest request) {
        InitializeAcademicYearResponse response = initializationService.initialize(id, request.fromAcademicYearId());
        return ResponseEntity.ok(ApiResponse.success("Khởi tạo năm học thành công", response));
    }
}
