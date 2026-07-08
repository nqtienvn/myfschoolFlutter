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
import vn.edu.fpt.myfschool.service.SemesterResultCalculationService;

@RestController
@RequestMapping("/api/semester-results")
@RequiredArgsConstructor
@Tag(name = "Semester Results", description = "Tong ket hoc ky")
@SecurityRequirement(name = "Bearer Authentication")
public class SemesterResultCalculationController {

    private final SemesterResultCalculationService semesterResultCalculationService;

    @PostMapping("/calculate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tu dong tinh tong ket hoc ky")
    public ResponseEntity<ApiResponse<CalculateSemesterResultResponse>> calculate(
            @Valid @RequestBody CalculateSemesterResultRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
            "Tinh tong ket thanh cong",
            semesterResultCalculationService.calculate(request.classId(), request.semesterId())));
    }
}