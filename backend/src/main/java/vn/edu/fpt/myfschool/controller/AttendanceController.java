package vn.edu.fpt.myfschool.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
import vn.edu.fpt.myfschool.common.dto.AdminAttendanceAdjustmentRequest;
import vn.edu.fpt.myfschool.common.dto.AdminAttendanceDayDto;
import vn.edu.fpt.myfschool.common.dto.AttendanceCorrectionRequestDto;
import vn.edu.fpt.myfschool.common.dto.AttendanceDto;
import vn.edu.fpt.myfschool.common.dto.AttendanceLogDto;
import vn.edu.fpt.myfschool.common.dto.ClassAttendanceSummaryDto;
import vn.edu.fpt.myfschool.common.dto.DailyAttendanceDto;
import vn.edu.fpt.myfschool.common.dto.HomeroomAttendanceContextDto;
import vn.edu.fpt.myfschool.common.dto.SubmitAttendanceRequest;
import vn.edu.fpt.myfschool.common.enums.Shift;
import vn.edu.fpt.myfschool.common.util.SecurityUtil;
import vn.edu.fpt.myfschool.service.AttendanceService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
@Tag(name = "Attendance", description = "Chuyên cần / Điểm danh")
public class AttendanceController {

    private final AttendanceService attendanceService;

    @GetMapping("/homeroom-context")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Lớp chủ nhiệm và cấu hình buổi học theo ngày")
    public ResponseEntity<ApiResponse<HomeroomAttendanceContextDto>> getHomeroomContext(
            @RequestParam(required = false) LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        return ResponseEntity.ok(ApiResponse.success(
            attendanceService.getHomeroomContext(targetDate, SecurityUtil.getCurrentUserId())));
    }

    @GetMapping("/daily")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "DS điểm danh theo ngày")
    public ResponseEntity<ApiResponse<DailyAttendanceDto>> getDailyAttendance(
            @RequestParam Long classId, @RequestParam LocalDate date, @RequestParam Shift shift) {
        return ResponseEntity.ok(ApiResponse.success(
                attendanceService.getDailyAttendance(classId, date, shift, SecurityUtil.getCurrentUserId())));
    }

    @PostMapping("/submit")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Gửi điểm danh")
    public ResponseEntity<ApiResponse<List<AttendanceDto>>> submitAttendance(
            @Valid @RequestBody SubmitAttendanceRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Điểm danh thành công",
                attendanceService.submitAttendance(request, SecurityUtil.getCurrentUserId())));
    }

    @PostMapping("/corrections")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Gửi yêu cầu sửa điểm danh để Admin duyệt")
    public ResponseEntity<ApiResponse<AttendanceCorrectionRequestDto>> requestCorrection(
            @Valid @RequestBody SubmitAttendanceRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Đã gửi yêu cầu sửa điểm danh",
            attendanceService.requestAttendanceCorrection(request, SecurityUtil.getCurrentUserId())));
    }

    @GetMapping("/student")
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT')")
    @Operation(summary = "Nhật ký chuyên cần")
    public ResponseEntity<ApiResponse<AttendanceLogDto>> getStudentAttendance(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long semesterId) {
        return ResponseEntity.ok(ApiResponse.success(attendanceService.getStudentAttendanceLog(
            studentId, semesterId, SecurityUtil.getCurrentUserId())));
    }

    @GetMapping("/class-summary")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Thống kê chuyên cần lớp học")
    public ResponseEntity<ApiResponse<List<ClassAttendanceSummaryDto>>> getClassAttendanceSummary(
            @RequestParam Long classId,
            @RequestParam Long semesterId,
            @RequestParam Long academicYearId) {
        return ResponseEntity.ok(ApiResponse.success(
            attendanceService.getClassAttendanceSummary(classId, semesterId, academicYearId)));
    }

    @GetMapping("/admin/daily")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Quản lý điểm danh theo lớp, ngày và buổi")
    public ResponseEntity<ApiResponse<List<AdminAttendanceDayDto>>> getAdminDailyAttendance(
            @RequestParam Long academicYearId,
            @RequestParam LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success(
            attendanceService.getAdminDailyAttendance(academicYearId, date)));
    }

    @PutMapping("/admin/daily")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin bổ sung hoặc điều chỉnh tổng điểm danh trong ngày")
    public ResponseEntity<ApiResponse<AdminAttendanceDayDto>> adjustAdminDailyAttendance(
            @Valid @RequestBody AdminAttendanceAdjustmentRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật điểm danh thành công",
            attendanceService.adjustAdminDailyAttendance(request)));
    }

    @GetMapping("/admin/corrections")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Danh sách yêu cầu sửa điểm danh chờ duyệt")
    public ResponseEntity<ApiResponse<List<AttendanceCorrectionRequestDto>>> getPendingCorrections(
            @RequestParam Long academicYearId,
            @RequestParam LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success(
            attendanceService.getPendingCorrections(academicYearId, date)));
    }

    @PutMapping("/admin/corrections/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AttendanceCorrectionRequestDto>> approveCorrection(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Đã duyệt yêu cầu sửa điểm danh",
            attendanceService.reviewAttendanceCorrection(id, true)));
    }

    @PutMapping("/admin/corrections/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AttendanceCorrectionRequestDto>> rejectCorrection(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Đã từ chối yêu cầu sửa điểm danh",
            attendanceService.reviewAttendanceCorrection(id, false)));
    }
}
