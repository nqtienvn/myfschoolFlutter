package vn.edu.fpt.myfschool.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.util.SecurityUtil;
import vn.edu.fpt.myfschool.service.HomeroomEngagementService;

import java.util.List;

@RestController
@RequestMapping("/api/parent-meetings")
@RequiredArgsConstructor
public class ParentMeetingController {
    private final HomeroomEngagementService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'PARENT', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<ParentMeetingDto>>> list(@RequestParam Long academicYearId,
            @RequestParam Long semesterId, @RequestParam(required = false) Long classId) {
        return ResponseEntity.ok(ApiResponse.success(service.getMeetings(academicYearId, semesterId, classId,
                SecurityUtil.getCurrentUserId(), SecurityUtil.getCurrentUserRole())));
    }

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<ParentMeetingDto>> create(@Valid @RequestBody SaveParentMeetingRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.createMeeting(request, SecurityUtil.getCurrentUserId())));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<ParentMeetingDto>> update(@PathVariable Long id,
            @Valid @RequestBody SaveParentMeetingRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.updateMeeting(id, request, SecurityUtil.getCurrentUserId())));
    }

    @PutMapping("/{id}/respond")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<ApiResponse<ParentMeetingDto>> respond(@PathVariable Long id,
            @Valid @RequestBody MeetingResponseRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.respondMeeting(id, request, SecurityUtil.getCurrentUserId())));
    }

    @PutMapping("/{id}/attendance")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<ParentMeetingDto>> attendance(@PathVariable Long id,
            @Valid @RequestBody MeetingAttendanceRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.markAttendance(id, request, SecurityUtil.getCurrentUserId())));
    }
}
