package vn.edu.fpt.myfschool.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.fpt.myfschool.common.dto.ApiResponse;
import vn.edu.fpt.myfschool.common.dto.DashboardStudentStatsDto;
import vn.edu.fpt.myfschool.common.dto.DashboardTeacherStatsDto;
import vn.edu.fpt.myfschool.common.util.SecurityUtil;
import vn.edu.fpt.myfschool.service.DashboardService;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Tổng quan / Thống kê")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/student")
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT')")
    @Operation(summary = "Dashboard học sinh")
    public ResponseEntity<ApiResponse<DashboardStudentStatsDto>> getStudentDashboard(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long academicYearId,
            @RequestParam(required = false) Long semesterId) {
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.getStudentDashboard(
                    SecurityUtil.getCurrentUserId(), studentId, academicYearId, semesterId)));
    }

    @GetMapping("/teacher")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Dashboard giáo viên chủ nhiệm")
    public ResponseEntity<ApiResponse<DashboardTeacherStatsDto>> getTeacherDashboard(
            @RequestParam(required = false) Long academicYearId,
            @RequestParam(required = false) Long semesterId) {
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.getTeacherDashboard(
                    SecurityUtil.getCurrentUserId(), academicYearId, semesterId)));
    }
}
