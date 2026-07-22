package vn.edu.fpt.myfschool.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.fpt.myfschool.common.dto.ApiResponse;
import vn.edu.fpt.myfschool.common.dto.CalculateSemesterResultRequest;
import vn.edu.fpt.myfschool.common.dto.CalculateSemesterResultResponse;
import vn.edu.fpt.myfschool.common.dto.SchoolSemesterResultRequest;
import vn.edu.fpt.myfschool.service.SemesterResultCalculationService;

@RestController
@RequestMapping("/api/semester-results")
@RequiredArgsConstructor
@Tag(name = "Semester Results", description = "Tong ket hoc ky")
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

    @PostMapping("/calculate-school")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tu dong tinh tong ket hoc ky cho toan truong")
    public ResponseEntity<ApiResponse<CalculateSemesterResultResponse>> calculateSchool(
            @Valid @RequestBody SchoolSemesterResultRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Da tinh tong ket hoc ky cho toan truong",
                semesterResultCalculationService.calculateSchool(request.academicYearId(), request.semesterId())));
    }
}
