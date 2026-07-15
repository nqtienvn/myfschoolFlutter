package vn.edu.fpt.myfschool.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.enums.RiskStatus;
import vn.edu.fpt.myfschool.common.util.SecurityUtil;
import vn.edu.fpt.myfschool.service.StudentRiskService;

import java.util.List;

@RestController
@RequestMapping("/api/homeroom")
@RequiredArgsConstructor
public class StudentRiskController {
    private final StudentRiskService service;

    @GetMapping("/risks")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<StudentRiskFlagDto>>> list(
            @RequestParam Long academicYearId,
            @RequestParam Long semesterId,
            @RequestParam Long classId,
            @RequestParam(required = false) RiskStatus status) {
        return ResponseEntity.ok(ApiResponse.success(service.getRisks(academicYearId, semesterId, classId, status,
                SecurityUtil.getCurrentUserId(), SecurityUtil.getCurrentUserRole())));
    }

    @PostMapping("/risks/recalculate")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<StudentRiskFlagDto>>> recalculate(
            @RequestParam Long academicYearId,
            @RequestParam Long semesterId,
            @RequestParam Long classId) {
        service.getRisks(academicYearId, semesterId, classId, null,
                SecurityUtil.getCurrentUserId(), SecurityUtil.getCurrentUserRole());
        return ResponseEntity.ok(ApiResponse.success(service.recalculateClass(academicYearId, semesterId, classId)));
    }

    @PutMapping("/risks/{id}/acknowledge")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<StudentRiskFlagDto>> acknowledge(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(service.acknowledge(id,
                SecurityUtil.getCurrentUserId(), SecurityUtil.getCurrentUserRole())));
    }

    @PutMapping("/risks/{id}/resolve")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<StudentRiskFlagDto>> resolve(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(service.resolve(id,
                SecurityUtil.getCurrentUserId(), SecurityUtil.getCurrentUserRole())));
    }

    @GetMapping("/risk-config")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<StudentRiskConfigDto>> config(@RequestParam Long academicYearId) {
        return ResponseEntity.ok(ApiResponse.success(service.getConfig(academicYearId)));
    }

    @PutMapping("/risk-config")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<StudentRiskConfigDto>> updateConfig(
            @Valid @RequestBody UpdateStudentRiskConfigRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.updateConfig(request)));
    }
}
