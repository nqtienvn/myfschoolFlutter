package vn.edu.fpt.myfschool.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.myfschool.common.dto.HomeroomReportDto;
import vn.edu.fpt.myfschool.common.dto.ReopenPeriodicReportRequest;
import vn.edu.fpt.myfschool.common.enums.PeriodicReportStatus;
import vn.edu.fpt.myfschool.common.dto.ApiResponse;
import vn.edu.fpt.myfschool.common.util.SecurityUtil;
import vn.edu.fpt.myfschool.service.PeriodicReviewService;

import java.util.List;

@RestController
@RequestMapping("/api/periodic-reports")
@RequiredArgsConstructor
public class PeriodicReportController {
    private final PeriodicReviewService service;

    @GetMapping("/students/{studentId}")
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT')")
    public ResponseEntity<ApiResponse<HomeroomReportDto>> published(
            @PathVariable Long studentId,
            @RequestParam Long academicYearId,
            @RequestParam Long semesterId) {
        return ResponseEntity.ok(ApiResponse.success(service.getPublishedReport(studentId, academicYearId,
                semesterId, SecurityUtil.getCurrentUserId(), SecurityUtil.getCurrentUserRole())));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<HomeroomReportDto>>> adminList(
            @RequestParam Long academicYearId,
            @RequestParam Long semesterId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) PeriodicReportStatus status) {
        return ResponseEntity.ok(ApiResponse.success(
                service.getAdminReports(academicYearId, semesterId, classId, status)));
    }

    @PutMapping("/admin/{id}/reopen")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<HomeroomReportDto>> reopen(
            @PathVariable Long id,
            @Valid @RequestBody ReopenPeriodicReportRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                service.reopenReport(id, request, SecurityUtil.getCurrentUserId())));
    }
}
