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
import vn.edu.fpt.myfschool.common.enums.LeaveStatus;
import vn.edu.fpt.myfschool.common.util.SecurityUtil;
import vn.edu.fpt.myfschool.service.LeaveRequestService;

import java.util.List;

@RestController
@RequestMapping("/api/leave-requests")
@RequiredArgsConstructor
@Tag(name = "Leave Requests", description = "Đơn xin nghỉ học")
@SecurityRequirement(name = "Bearer Authentication")
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;

    @PostMapping
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "Tạo đơn xin nghỉ")
    public ResponseEntity<ApiResponse<LeaveRequestDto>> createLeaveRequest(
            @Valid @RequestBody CreateLeaveRequestRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Tạo đơn thành công",
            leaveRequestService.createLeaveRequest(request, SecurityUtil.getCurrentUserId())));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "Đơn của tôi")
    public ResponseEntity<ApiResponse<List<LeaveRequestDto>>> getMyLeaveRequests() {
        return ResponseEntity.ok(ApiResponse.success(
            leaveRequestService.getParentLeaveRequests(SecurityUtil.getCurrentUserId())));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Đơn chờ duyệt")
    public ResponseEntity<ApiResponse<List<LeaveRequestDto>>> getPendingRequests() {
        return ResponseEntity.ok(ApiResponse.success(
            leaveRequestService.getPendingLeaveRequests(SecurityUtil.getCurrentUserId())));
    }

    @GetMapping("/class")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Đơn theo lớp")
    public ResponseEntity<ApiResponse<List<LeaveRequestDto>>> getClassRequests(
            @RequestParam Long classId,
            @RequestParam(required = false) LeaveStatus status) {
        return ResponseEntity.ok(ApiResponse.success(leaveRequestService.getClassLeaveRequests(classId, status)));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Duyệt đơn")
    public ResponseEntity<ApiResponse<LeaveRequestDto>> approveRequest(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Duyệt đơn thành công",
            leaveRequestService.approveLeaveRequest(id, null, SecurityUtil.getCurrentUserId())));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Từ chối đơn")
    public ResponseEntity<ApiResponse<LeaveRequestDto>> rejectRequest(
            @PathVariable Long id,
            @RequestBody ReviewLeaveRequestRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Từ chối đơn thành công",
            leaveRequestService.rejectLeaveRequest(id, request.response(), SecurityUtil.getCurrentUserId())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PARENT')")
    @Operation(summary = "Hủy đơn")
    public ResponseEntity<ApiResponse<Void>> cancelRequest(@PathVariable Long id) {
        leaveRequestService.cancelLeaveRequest(id, SecurityUtil.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success("Hủy đơn thành công", null));
    }

    @GetMapping("/pending-count")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Đếm đơn chờ")
    public ResponseEntity<ApiResponse<Long>> getPendingCount() {
        return ResponseEntity.ok(ApiResponse.success(
            leaveRequestService.getPendingCount(SecurityUtil.getCurrentUserId())));
    }
}
