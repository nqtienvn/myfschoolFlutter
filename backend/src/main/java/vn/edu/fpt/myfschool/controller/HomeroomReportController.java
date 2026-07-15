package vn.edu.fpt.myfschool.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.dto.ApiResponse;
import vn.edu.fpt.myfschool.common.util.SecurityUtil;
import vn.edu.fpt.myfschool.service.PeriodicReviewService;

import java.util.List;

@RestController
@RequestMapping("/api/homeroom-reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TEACHER')")
public class HomeroomReportController {
    private final PeriodicReviewService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<HomeroomReportDto>>> list(
            @RequestParam Long academicYearId,
            @RequestParam Long semesterId,
            @RequestParam Long classId) {
        return ResponseEntity.ok(ApiResponse.success(service.getHomeroomReports(academicYearId, semesterId,
                classId, SecurityUtil.getCurrentUserId())));
    }

    @GetMapping("/students/{studentId}")
    public ResponseEntity<ApiResponse<HomeroomReportDto>> detail(
            @PathVariable Long studentId,
            @RequestParam Long academicYearId,
            @RequestParam Long semesterId,
            @RequestParam Long classId) {
        return ResponseEntity.ok(ApiResponse.success(service.getHomeroomReport(studentId, academicYearId,
                semesterId, classId, SecurityUtil.getCurrentUserId())));
    }

    @PutMapping("/students/{studentId}")
    public ResponseEntity<ApiResponse<HomeroomReportDto>> save(
            @PathVariable Long studentId,
            @Valid @RequestBody SavePeriodicReportRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                service.saveHomeroomReport(studentId, request, SecurityUtil.getCurrentUserId())));
    }

    @PostMapping("/students/{studentId}/publish")
    public ResponseEntity<ApiResponse<HomeroomReportDto>> publishStudent(
            @PathVariable Long studentId,
            @Valid @RequestBody ReportScopeRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                service.publishStudent(studentId, request, SecurityUtil.getCurrentUserId())));
    }

    @PostMapping("/publish-class")
    public ResponseEntity<ApiResponse<List<HomeroomReportDto>>> publishClass(
            @Valid @RequestBody ReportScopeRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                service.publishClass(request, SecurityUtil.getCurrentUserId())));
    }
}
