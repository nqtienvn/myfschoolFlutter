package vn.edu.fpt.myfschool.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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
import vn.edu.fpt.myfschool.common.dto.ClassDetailDto;
import vn.edu.fpt.myfschool.common.dto.ClassDto;
import vn.edu.fpt.myfschool.common.dto.GenerateClassesRequest;
import vn.edu.fpt.myfschool.common.dto.StudentSummaryDto;
import vn.edu.fpt.myfschool.common.dto.UpdateClassRequest;
import vn.edu.fpt.myfschool.common.util.SecurityUtil;
import vn.edu.fpt.myfschool.service.ClassService;

import java.util.List;

@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
@Tag(name = "Classes", description = "Quản lý lớp học")
public class ClassController {

    private final ClassService classService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PARENT', 'STUDENT', 'TEACHER')")
    @Operation(summary = "Danh sách lớp")
    public ResponseEntity<ApiResponse<Page<ClassDto>>> listClasses(
            @RequestParam(required = false) Long academicYearId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                classService.listClasses(academicYearId, keyword, page, size,
                        SecurityUtil.getCurrentUserId(), SecurityUtil.getCurrentUserRole())));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Chi tiết lớp")
    public ResponseEntity<ApiResponse<ClassDetailDto>> getClassDetail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(classService.getClassDetail(
                id, SecurityUtil.getCurrentUserId(), SecurityUtil.getCurrentUserRole())));
    }

    @PostMapping("/generate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Sinh nhiều lớp theo quy tắc đặt tên")
    public ResponseEntity<ApiResponse<List<ClassDto>>> generateClasses(@Valid @RequestBody GenerateClassesRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Sinh lớp thành công", classService.generateClasses(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Sửa lớp")
    public ResponseEntity<ApiResponse<ClassDto>> updateClass(
            @PathVariable Long id,
            @Valid @RequestBody UpdateClassRequest request) {
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
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Danh sách học sinh trong lớp")
    public ResponseEntity<ApiResponse<List<StudentSummaryDto>>> getStudents(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(classService.getStudentsInClass(
                id, SecurityUtil.getCurrentUserId(), SecurityUtil.getCurrentUserRole())));
    }
}
