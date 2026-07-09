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
import vn.edu.fpt.myfschool.common.dto.CreateSemesterRequest;
import vn.edu.fpt.myfschool.common.dto.SemesterDto;
import vn.edu.fpt.myfschool.service.SemesterService;

import java.util.List;

@RestController
@RequestMapping("/api/semesters")
@RequiredArgsConstructor
@Tag(name = "Semesters", description = "Quản lý học kỳ")
public class SemesterController {

    private final SemesterService semesterService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PARENT', 'STUDENT', 'TEACHER')")
    @Operation(summary = "Danh sách học kỳ")
    public ResponseEntity<ApiResponse<List<SemesterDto>>> listSemesters(
            @RequestParam(required = false) Long academicYearId) {
        return ResponseEntity.ok(ApiResponse.success(semesterService.listSemesters(academicYearId)));
    }

    @GetMapping("/current")
    @PreAuthorize("hasAnyRole('ADMIN', 'PARENT', 'STUDENT', 'TEACHER')")
    @Operation(summary = "Học kỳ hiện tại")
    public ResponseEntity<ApiResponse<SemesterDto>> getCurrentSemester() {
        return ResponseEntity.ok(ApiResponse.success(semesterService.getCurrentSemester()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PARENT', 'STUDENT', 'TEACHER')")
    @Operation(summary = "Chi tiết học kỳ")
    public ResponseEntity<ApiResponse<SemesterDto>> getSemester(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(semesterService.getSemester(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tạo học kỳ")
    public ResponseEntity<ApiResponse<SemesterDto>> createSemester(
            @Valid @RequestBody CreateSemesterRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Tạo học kỳ thành công", semesterService.createSemester(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Sửa học kỳ")
    public ResponseEntity<ApiResponse<SemesterDto>> updateSemester(
            @PathVariable Long id,
            @Valid @RequestBody CreateSemesterRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thành công", semesterService.updateSemester(id, request)));
    }

    @PutMapping("/{id}/set-current")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Đặt làm học kỳ hiện tại")
    public ResponseEntity<ApiResponse<Void>> setCurrentSemester(@PathVariable Long id) {
        semesterService.setCurrentSemester(id);
        return ResponseEntity.ok(ApiResponse.success("Đặt học kỳ hiện tại thành công", null));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa mềm học kỳ")
    public ResponseEntity<ApiResponse<Void>> deleteSemester(@PathVariable Long id) {
        semesterService.deleteSemester(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa học kỳ thành công", null));
    }
}
