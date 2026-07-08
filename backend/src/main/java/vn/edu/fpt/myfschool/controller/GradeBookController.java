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
import vn.edu.fpt.myfschool.service.GradeBookService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/grade-books")
@RequiredArgsConstructor
@Tag(name = "Grade Books", description = "Bang diem theo lop-mon-hoc ky")
@SecurityRequirement(name = "Bearer Authentication")
public class GradeBookController {

    private final GradeBookService gradeBookService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Lay hoac tao GradeBook")
    public ResponseEntity<ApiResponse<GradeBookDto>> getOrCreate(
            @RequestParam Long classId, @RequestParam Long subjectId, @RequestParam Long semesterId) {
        return ResponseEntity.ok(ApiResponse.success(
            gradeBookService.getOrCreate(classId, subjectId, semesterId)));
    }

    @PostMapping("/{id}/items")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Them cot diem (GradeItem)")
    public ResponseEntity<ApiResponse<GradeItemDto>> addItem(
            @PathVariable Long id, @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        Integer weight = (Integer) body.get("weight");
        Integer order = body.get("order") != null ? (Integer) body.get("order") : null;
        return ResponseEntity.ok(ApiResponse.success(
            "Them cot diem thanh cong", gradeBookService.addItem(id, name, weight, order)));
    }

    @PutMapping("/scores")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Cap nhat diem hang loat")
    public ResponseEntity<ApiResponse<List<StudentScoreDto>>> updateScores(
            @Valid @RequestBody UpdateStudentScoreRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
            "Cap nhat diem thanh cong", gradeBookService.updateScores(request)));
    }

    @GetMapping("/{id}/students")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Xem diem tat ca HS trong GradeBook")
    public ResponseEntity<ApiResponse<List<StudentScoreDto>>> getStudentScores(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
            gradeBookService.getStudentScores(id)));
    }

    @PostMapping("/{id}/finalize")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lock gradebook (khong cho sua)")
    public ResponseEntity<ApiResponse<Void>> finalizeBook(@PathVariable Long id) {
        gradeBookService.finalize(id);
        return ResponseEntity.ok(ApiResponse.success("Da khoa bang diem", null));
    }
}