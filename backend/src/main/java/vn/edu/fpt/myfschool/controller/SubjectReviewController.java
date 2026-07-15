package vn.edu.fpt.myfschool.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.dto.ApiResponse;
import vn.edu.fpt.myfschool.common.util.SecurityUtil;
import vn.edu.fpt.myfschool.service.PeriodicReviewService;

import java.util.List;

@RestController
@RequestMapping("/api/subject-reviews")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TEACHER')")
public class SubjectReviewController {
    private final PeriodicReviewService service;

    @GetMapping("/assignments")
    public ResponseEntity<ApiResponse<List<ReviewAssignmentDto>>> assignments(
            @RequestParam Long academicYearId) {
        return ResponseEntity.ok(ApiResponse.success(
                service.getAssignments(academicYearId, SecurityUtil.getCurrentUserId())));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SubjectReviewDto>>> list(
            @RequestParam Long academicYearId,
            @RequestParam Long semesterId,
            @RequestParam Long classId,
            @RequestParam Long subjectId) {
        return ResponseEntity.ok(ApiResponse.success(service.getSubjectReviews(academicYearId, semesterId,
                classId, subjectId, SecurityUtil.getCurrentUserId())));
    }

    @PutMapping("/{studentId}")
    public ResponseEntity<ApiResponse<SubjectReviewDto>> save(
            @PathVariable Long studentId,
            @Valid @RequestBody SaveSubjectReviewRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                service.saveSubjectReview(studentId, request, SecurityUtil.getCurrentUserId())));
    }

    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<List<SubjectReviewDto>>> submit(
            @Valid @RequestBody SubmitSubjectReviewsRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                service.submitSubjectReviews(request, SecurityUtil.getCurrentUserId())));
    }

    @PutMapping("/{id}/return")
    public ResponseEntity<ApiResponse<SubjectReviewDto>> returnReview(
            @PathVariable Long id,
            @Valid @RequestBody ReturnSubjectReviewRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                service.returnSubjectReview(id, request, SecurityUtil.getCurrentUserId())));
    }
}
