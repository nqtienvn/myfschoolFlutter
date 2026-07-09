package vn.edu.fpt.myfschool.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.fpt.myfschool.common.dto.ApiResponse;
import vn.edu.fpt.myfschool.common.dto.InitializeAcademicYearRequest;
import vn.edu.fpt.myfschool.common.dto.InitializeAcademicYearResponse;
import vn.edu.fpt.myfschool.service.AcademicYearInitializationService;

@RestController
@RequestMapping("/api/academic-years")
@RequiredArgsConstructor
@Tag(name = "Academic Year Init", description = "Khởi tạo năm học")
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
