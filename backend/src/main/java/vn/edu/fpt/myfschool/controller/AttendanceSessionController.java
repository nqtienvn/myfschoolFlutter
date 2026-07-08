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
import vn.edu.fpt.myfschool.common.enums.Shift;
import vn.edu.fpt.myfschool.common.util.SecurityUtil;
import vn.edu.fpt.myfschool.service.AttendanceSessionService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/attendance-sessions")
@RequiredArgsConstructor
@Tag(name = "Attendance Sessions", description = "Diem danh theo buoi")
@SecurityRequirement(name = "Bearer Authentication")
public class AttendanceSessionController {

    private final AttendanceSessionService attendanceSessionService;

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<AttendanceSessionDto>> createSession(
            @Valid @RequestBody CreateAttendanceSessionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
            "Bat dau diem danh",
            attendanceSessionService.createSession(request, SecurityUtil.getCurrentUserId())));
    }

    @PutMapping("/{id}/details")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<List<AttendanceDetailDto>>> updateDetails(
            @PathVariable Long id, @Valid @RequestBody UpdateAttendanceDetailRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
            "Cap nhat diem danh",
            attendanceSessionService.updateDetails(request, SecurityUtil.getCurrentUserId())));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<AttendanceSessionDto>> closeSession(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
            "Ket thuc diem danh",
            attendanceSessionService.closeSession(id, SecurityUtil.getCurrentUserId())));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<AttendanceSessionDto>>> getSessions(
            @RequestParam Long classId, @RequestParam LocalDate date, @RequestParam Shift shift) {
        return ResponseEntity.ok(ApiResponse.success(
            attendanceSessionService.findByClassDateShift(classId, date, shift)));
    }
}