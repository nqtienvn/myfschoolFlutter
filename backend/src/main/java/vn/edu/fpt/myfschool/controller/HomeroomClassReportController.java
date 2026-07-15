package vn.edu.fpt.myfschool.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.myfschool.common.dto.ApiResponse;
import vn.edu.fpt.myfschool.common.dto.ClassSummaryDto;
import vn.edu.fpt.myfschool.common.util.SecurityUtil;
import vn.edu.fpt.myfschool.service.HomeroomClassReportService;

import java.util.List;

@RestController
@RequestMapping("/api/homeroom/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
public class HomeroomClassReportController {
    private final HomeroomClassReportService service;

    @GetMapping("/class-summary")
    public ResponseEntity<ApiResponse<List<ClassSummaryDto>>> summary(
            @RequestParam Long academicYearId,
            @RequestParam Long semesterId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Integer gradeLevel) {
        return ResponseEntity.ok(ApiResponse.success(service.getSummaries(academicYearId, semesterId, classId,
                gradeLevel, SecurityUtil.getCurrentUserId(), SecurityUtil.getCurrentUserRole())));
    }
}
