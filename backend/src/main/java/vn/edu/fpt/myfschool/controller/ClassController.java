package vn.edu.fpt.myfschool.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.exception.BadRequestException;
import vn.edu.fpt.myfschool.service.ClassService;

import java.util.List;

@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
@Tag(name = "Classes", description = "Quản lý lớp học")
@SecurityRequirement(name = "Bearer Authentication")
public class ClassController {

    private final ClassService classService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PARENT', 'STUDENT', 'TEACHER')")
    @Operation(summary = "Danh sách lớp")
    public ResponseEntity<ApiResponse<Page<ClassDto>>> listClasses(
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
            classService.listClasses(academicYear, keyword, page, size)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PARENT', 'STUDENT', 'TEACHER')")
    @Operation(summary = "Chi tiết lớp")
    public ResponseEntity<ApiResponse<ClassDetailDto>> getClassDetail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(classService.getClassDetail(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tạo lớp")
    public ResponseEntity<ApiResponse<ClassDto>> createClass(
            @Valid @RequestBody CreateClassRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Tạo lớp thành công", classService.createClass(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Sửa lớp")
    public ResponseEntity<ApiResponse<ClassDto>> updateClass(
            @PathVariable Long id,
            @Valid @RequestBody CreateClassRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thành công", classService.updateClass(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa lớp")
    public ResponseEntity<ApiResponse<Void>> deleteClass(@PathVariable Long id) {
        classService.deleteClass(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa lớp thành công", null));
    }

    @GetMapping("/{id}/students")
    @PreAuthorize("hasAnyRole('ADMIN', 'PARENT', 'STUDENT', 'TEACHER')")
    @Operation(summary = "Danh sách học sinh trong lớp")
    public ResponseEntity<ApiResponse<List<StudentSummaryDto>>> getStudents(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(classService.getStudentsInClass(id)));
    }

    @PostMapping("/{id}/subjects")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Phân công môn học")
    public ResponseEntity<ApiResponse<ClassSubjectDto>> assignSubject(
            @PathVariable Long id,
            @Valid @RequestBody CreateClassSubjectRequest request) {
        if (!id.equals(request.classId())) {
            throw new BadRequestException("classId trong đường dẫn và body không khớp");
        }
        return ResponseEntity.ok(ApiResponse.success("Phân công thành công", classService.assignSubject(request)));
    }

    @DeleteMapping("/subjects/{classSubjectId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Gỡ phân công môn học")
    public ResponseEntity<ApiResponse<Void>> removeSubject(@PathVariable Long classSubjectId) {
        classService.removeSubject(classSubjectId);
        return ResponseEntity.ok(ApiResponse.success("Gỡ phân công thành công", null));
    }
}
