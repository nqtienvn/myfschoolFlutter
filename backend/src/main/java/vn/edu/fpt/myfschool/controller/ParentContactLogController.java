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
@PreAuthorize("hasRole('TEACHER')")
public class ParentContactLogController {
    private final HomeroomEngagementService service;

    @GetMapping("/api/students/{studentId}/contact-logs")
    public ResponseEntity<ApiResponse<List<ParentContactLogDto>>> list(@PathVariable Long studentId,
            @RequestParam Long academicYearId, @RequestParam Long semesterId, @RequestParam Long classId) {
        return ResponseEntity.ok(ApiResponse.success(service.getContactLogs(studentId, academicYearId, semesterId,
                classId, SecurityUtil.getCurrentUserId())));
    }

    @PostMapping("/api/students/{studentId}/contact-logs")
    public ResponseEntity<ApiResponse<ParentContactLogDto>> create(@PathVariable Long studentId,
            @Valid @RequestBody SaveParentContactLogRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.createContactLog(studentId, request,
                SecurityUtil.getCurrentUserId())));
    }

    @PutMapping("/api/contact-logs/{id}")
    public ResponseEntity<ApiResponse<ParentContactLogDto>> update(@PathVariable Long id,
            @Valid @RequestBody SaveParentContactLogRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.updateContactLog(id, request,
                SecurityUtil.getCurrentUserId())));
    }

    @DeleteMapping("/api/contact-logs/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        service.deleteContactLog(id, SecurityUtil.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
