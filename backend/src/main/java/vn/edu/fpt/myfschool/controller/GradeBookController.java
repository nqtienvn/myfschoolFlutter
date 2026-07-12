package vn.edu.fpt.myfschool.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.fpt.myfschool.common.dto.ApiResponse;
import vn.edu.fpt.myfschool.common.dto.GradeBookDto;
import vn.edu.fpt.myfschool.common.dto.GradeItemDto;
import vn.edu.fpt.myfschool.common.dto.StudentScoreDto;
import vn.edu.fpt.myfschool.common.dto.UpdateStudentScoreRequest;
import vn.edu.fpt.myfschool.service.GradeBookService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/grade-books")
@RequiredArgsConstructor
@Tag(name = "Grade Books", description = "Bang diem theo lop-mon-hoc ky")
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

    @PutMapping("/scores")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
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

    @PostMapping("/{id}/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Công bố hoặc khóa bảng điểm")
    public ResponseEntity<ApiResponse<Void>> changeStatus(@PathVariable Long id,
            @PathVariable vn.edu.fpt.myfschool.common.enums.GradeBookStatus status) {
        gradeBookService.changeStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Đã cập nhật trạng thái bảng điểm", null));
    }
}
