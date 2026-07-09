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
import vn.edu.fpt.myfschool.common.dto.CreateSubjectRequest;
import vn.edu.fpt.myfschool.common.dto.SubjectDto;
import vn.edu.fpt.myfschool.service.SubjectService;

import java.util.List;

@RestController
@RequestMapping("/api/subjects")
@RequiredArgsConstructor
@Tag(name = "Subjects", description = "Quản lý môn học")
public class SubjectController {

    private final SubjectService subjectService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PARENT', 'STUDENT', 'TEACHER')")
    @Operation(summary = "Danh sách môn học")
    public ResponseEntity<ApiResponse<List<SubjectDto>>> listSubjects(
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(ApiResponse.success(subjectService.listSubjects(keyword)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PARENT', 'STUDENT', 'TEACHER')")
    @Operation(summary = "Chi tiết môn học")
    public ResponseEntity<ApiResponse<SubjectDto>> getSubject(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(subjectService.getSubject(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tạo môn học")
    public ResponseEntity<ApiResponse<SubjectDto>> createSubject(
            @Valid @RequestBody CreateSubjectRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Tạo môn học thành công", subjectService.createSubject(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Sửa môn học")
    public ResponseEntity<ApiResponse<SubjectDto>> updateSubject(
            @PathVariable Long id,
            @Valid @RequestBody CreateSubjectRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thành công", subjectService.updateSubject(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa môn học")
    public ResponseEntity<ApiResponse<Void>> deleteSubject(@PathVariable Long id) {
        subjectService.deleteSubject(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa môn học thành công", null));
    }
}
