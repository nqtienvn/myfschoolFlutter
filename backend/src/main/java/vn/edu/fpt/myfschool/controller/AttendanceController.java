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
import vn.edu.fpt.myfschool.common.enums.AttendanceStatus;
import vn.edu.fpt.myfschool.common.enums.Shift;
import vn.edu.fpt.myfschool.common.util.SecurityUtil;
import vn.edu.fpt.myfschool.service.AttendanceService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
@Tag(name = "Attendance", description = "Chuyên cần / Điểm danh")
@SecurityRequirement(name = "Bearer Authentication")
public class AttendanceController {

    private final AttendanceService attendanceService;

    @GetMapping("/daily")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "DS điểm danh theo ngày")
    public ResponseEntity<ApiResponse<DailyAttendanceDto>> getDailyAttendance(
            @RequestParam Long classId, @RequestParam LocalDate date, @RequestParam Shift shift) {
        return ResponseEntity.ok(ApiResponse.success(attendanceService.getDailyAttendance(classId, date, shift)));
    }

    @PostMapping("/submit")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Gửi điểm danh")
    public ResponseEntity<ApiResponse<List<AttendanceDto>>> submitAttendance(
            @Valid @RequestBody SubmitAttendanceRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Điểm danh thành công",
            attendanceService.submitAttendance(request, SecurityUtil.getCurrentUserId())));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Sửa 1 bản ghi CC")
    public ResponseEntity<ApiResponse<AttendanceDto>> updateAttendance(
            @PathVariable Long id, @RequestBody Map<String, String> body) {
        AttendanceStatus status = AttendanceStatus.valueOf(body.get("status"));
        return ResponseEntity.ok(ApiResponse.success(attendanceService.updateAttendance(id, status)));
    }

    @GetMapping("/student")
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT')")
    @Operation(summary = "Nhật ký chuyên cần")
    public ResponseEntity<ApiResponse<AttendanceLogDto>> getStudentAttendance(
            @RequestParam Long studentId, @RequestParam Long semesterId) {
        return ResponseEntity.ok(ApiResponse.success(attendanceService.getStudentAttendanceLog(studentId, semesterId)));
    }
}
