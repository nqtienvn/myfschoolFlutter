package vn.edu.fpt.myfschool.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.fpt.myfschool.common.dto.ApiResponse;
import vn.edu.fpt.myfschool.common.dto.CreateTeachingAssignmentRequest;
import vn.edu.fpt.myfschool.common.dto.TeachingAssignmentDetailDto;
import vn.edu.fpt.myfschool.common.dto.TeachingAssignmentDto;
import vn.edu.fpt.myfschool.common.enums.AssignmentStatus;
import vn.edu.fpt.myfschool.service.TeachingAssignmentService;

import java.util.List;

@RestController
@RequestMapping("/api/teaching-assignments")
@RequiredArgsConstructor
@Tag(name = "Teaching Assignments", description = "Phân công giáo viên bộ môn")
public class TeachingAssignmentController {

    private final TeachingAssignmentService teachingAssignmentService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "DS phân công theo lớp hoặc GV")
    public ResponseEntity<ApiResponse<List<TeachingAssignmentDto>>> list(
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(defaultValue = "ACTIVE") AssignmentStatus status) {
        if (classId != null) {
            return ResponseEntity.ok(ApiResponse.success(
                    teachingAssignmentService.listByClass(classId, status)));
        } else if (teacherId != null) {
            return ResponseEntity.ok(ApiResponse.success(
                    teachingAssignmentService.listByTeacher(teacherId, status)));
        }
        throw new vn.edu.fpt.myfschool.common.exception.BadRequestException("Phai cung cap classId hoac teacherId");
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Chi tiết phân công")
    public ResponseEntity<ApiResponse<TeachingAssignmentDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(teachingAssignmentService.getById(id)));
    }

    @GetMapping("/{id}/detail")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Chi tiết phân công + TKB")
    public ResponseEntity<ApiResponse<TeachingAssignmentDetailDto>> getDetail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(teachingAssignmentService.getDetail(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Phân công GV bộ môn")
    public ResponseEntity<ApiResponse<TeachingAssignmentDto>> create(
            @Valid @RequestBody CreateTeachingAssignmentRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Phân công thành công", teachingAssignmentService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Sửa phân công (đổi GV hoặc thời hạn)")
    public ResponseEntity<ApiResponse<TeachingAssignmentDto>> update(
            @PathVariable Long id, @Valid @RequestBody CreateTeachingAssignmentRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Cập nhật thành công", teachingAssignmentService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Kết thúc phân công (soft delete)")
    public ResponseEntity<ApiResponse<TeachingAssignmentDto>> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Đã kết thúc phân công", teachingAssignmentService.deactivate(id)));
    }

    @PostMapping("/{id}/reactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Khôi phục phân công")
    public ResponseEntity<ApiResponse<TeachingAssignmentDto>> reactivate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Khôi phục thành công", teachingAssignmentService.reactivate(id)));
    }
}
