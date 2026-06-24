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
import vn.edu.fpt.myfschool.service.GradeService;

import java.util.List;

@RestController
@RequestMapping("/api/grades")
@RequiredArgsConstructor
@Tag(name = "Grades", description = "Quản lý điểm số")
@SecurityRequirement(name = "Bearer Authentication")
public class GradeController {

    private final GradeService gradeService;

    @GetMapping("/semester")
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT')")
    @Operation(summary = "Xem điểm theo học kỳ")
    public ResponseEntity<ApiResponse<StudentSemesterGradesDto>> getStudentGrades(
            @RequestParam Long studentId, @RequestParam Long semesterId) {
        return ResponseEntity.ok(ApiResponse.success(gradeService.getStudentGrades(studentId, semesterId)));
    }

    @GetMapping("/subject")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Xem điểm môn học")
    public ResponseEntity<ApiResponse<SubjectGradesDto>> getSubjectGrades(
            @RequestParam Long subjectId, @RequestParam Long semesterId, @RequestParam Long classId) {
        return ResponseEntity.ok(ApiResponse.success(gradeService.getSubjectGrades(subjectId, semesterId, classId)));
    }

    @PutMapping
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Nhập/sửa điểm 1 học sinh")
    public ResponseEntity<ApiResponse<GradeDto>> updateGrade(
            @Valid @RequestBody UpdateGradeRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật điểm thành công", gradeService.updateGrade(request)));
    }

    @PostMapping("/batch")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Nhập điểm hàng loạt")
    public ResponseEntity<ApiResponse<List<GradeDto>>> batchUpdateGrades(
            @Valid @RequestBody BatchGradeUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Nhập điểm thành công", gradeService.batchUpdateGrades(request)));
    }

    @PostMapping("/simulation")
    @PreAuthorize("hasAnyRole('PARENT', 'STUDENT')")
    @Operation(summary = "Mô phỏng điểm")
    public ResponseEntity<ApiResponse<SimulationResultDto>> simulateGrades(
            @Valid @RequestBody GradeSimulationRequest request) {
        return ResponseEntity.ok(ApiResponse.success(gradeService.simulateGrades(1L, request)));
    }
}
