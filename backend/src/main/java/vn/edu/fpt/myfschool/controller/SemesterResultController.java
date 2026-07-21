package vn.edu.fpt.myfschool.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.fpt.myfschool.common.dto.ApiResponse;
import vn.edu.fpt.myfschool.common.dto.ClassRankingDto;
import vn.edu.fpt.myfschool.common.dto.SemesterResultDto;
import vn.edu.fpt.myfschool.common.dto.ResultOverrideRequest;
import vn.edu.fpt.myfschool.common.dto.ResultPublishRequest;
import vn.edu.fpt.myfschool.common.dto.ResultSummaryDto;
import vn.edu.fpt.myfschool.common.dto.ResultCloseRequest;
import vn.edu.fpt.myfschool.common.dto.AcademicYearResultDto;
import vn.edu.fpt.myfschool.common.dto.AcademicYearResultRequest;
import vn.edu.fpt.myfschool.common.util.SecurityUtil;
import vn.edu.fpt.myfschool.service.SemesterResultService;
import vn.edu.fpt.myfschool.service.AcademicYearResultService;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/semester-results")
@RequiredArgsConstructor
@Tag(name = "Semester Results", description = "Tổng kết học kỳ")
public class SemesterResultController {

    private final SemesterResultService semesterResultService;
    private final AcademicYearResultService academicYearResultService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PARENT', 'STUDENT', 'TEACHER')")
    @Operation(summary = "Xem tổng kết học kỳ")
    public ResponseEntity<ApiResponse<SemesterResultDto>> getSemesterResult(
            @RequestParam Long studentId, @RequestParam Long semesterId) {
        return ResponseEntity.ok(ApiResponse.success(semesterResultService.getStudentSemesterResult(
                studentId, semesterId,
                SecurityUtil.getCurrentUserId(), SecurityUtil.getCurrentUserRole())));
    }

    @GetMapping("/ranking")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Xếp hạng lớp")
    public ResponseEntity<ApiResponse<ClassRankingDto>> getClassRanking(
            @RequestParam Long classId, @RequestParam Long semesterId) {
        return ResponseEntity.ok(ApiResponse.success(semesterResultService.getClassRanking(
                classId, semesterId,
                SecurityUtil.getCurrentUserId(), SecurityUtil.getCurrentUserRole())));
    }

    @GetMapping("/admin/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ResultSummaryDto>>> getResultSummary(
            @RequestParam Long academicYearId,
            @RequestParam Long semesterId,
            @RequestParam Long classId) {
        return ResponseEntity.ok(ApiResponse.success(
                semesterResultService.getResultSummary(academicYearId, semesterId, classId)));
    }

    @PutMapping("/admin/students/{studentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ResultSummaryDto>> overrideResult(
            @PathVariable Long studentId,
            @Valid @RequestBody ResultOverrideRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                semesterResultService.overrideResult(studentId, request, SecurityUtil.getCurrentUserId())));
    }

    @PostMapping("/admin/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ResultSummaryDto>>> publishResults(
            @Valid @RequestBody ResultPublishRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Đã công bố kết quả học kỳ",
                semesterResultService.publishResults(request, SecurityUtil.getCurrentUserId())));
    }

    @PostMapping("/admin/close")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> closeSemester(
            @Valid @RequestBody ResultCloseRequest request) {
        semesterResultService.closeSemester(request);
        return ResponseEntity.ok(ApiResponse.success("Đã đóng kết quả học kỳ", null));
    }

    @GetMapping("/admin/annual")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<AcademicYearResultDto>>> getAnnualResults(
            @RequestParam Long academicYearId,
            @RequestParam Long classId) {
        return ResponseEntity.ok(ApiResponse.success(
                academicYearResultService.getResults(academicYearId, classId)));
    }

    @PostMapping("/admin/annual/calculate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<AcademicYearResultDto>>> calculateAnnualResults(
            @Valid @RequestBody AcademicYearResultRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Đã tính tổng kết năm học",
                academicYearResultService.calculate(request)));
    }

    @PostMapping("/admin/annual/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<AcademicYearResultDto>>> publishAnnualResults(
            @Valid @RequestBody AcademicYearResultRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Đã công bố kết quả năm học",
                academicYearResultService.publish(request)));
    }
}
