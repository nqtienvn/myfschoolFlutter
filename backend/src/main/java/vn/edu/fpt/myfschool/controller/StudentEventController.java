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
@RequiredArgsConstructor
public class StudentEventController {
    private final HomeroomEngagementService service;

    @GetMapping("/api/students/{studentId}/events")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<StudentEventDto>>> list(@PathVariable Long studentId,
            @RequestParam Long academicYearId, @RequestParam Long semesterId,
            @RequestParam(required = false) Long classId) {
        return ResponseEntity.ok(ApiResponse.success(service.getStudentEvents(studentId, academicYearId, semesterId,
                classId, SecurityUtil.getCurrentUserId(), SecurityUtil.getCurrentUserRole())));
    }

    @PostMapping("/api/students/{studentId}/events")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<StudentEventDto>> create(@PathVariable Long studentId,
            @Valid @RequestBody SaveStudentEventRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.createStudentEvent(studentId, request,
                SecurityUtil.getCurrentUserId(), SecurityUtil.getCurrentUserRole())));
    }

    @PutMapping("/api/student-events/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<StudentEventDto>> update(@PathVariable Long id,
            @Valid @RequestBody SaveStudentEventRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.updateStudentEvent(id, request,
                SecurityUtil.getCurrentUserId(), SecurityUtil.getCurrentUserRole())));
    }

    @DeleteMapping("/api/student-events/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id,
            @RequestParam Long academicYearId) {
        service.deleteStudentEvent(id, academicYearId,
                SecurityUtil.getCurrentUserId(), SecurityUtil.getCurrentUserRole());
        return ResponseEntity.ok(ApiResponse.success("Đã xóa vi phạm", null));
    }
}
